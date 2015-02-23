package com.xeroxrdc.rof;

/**
 * ROFRider 
 * Image analysis class
 * Uses OpenCV to measure position of sensor and
 * reference mark on an image of a printer page.
 * @author Chris
 * Attempted Documenter: Max Spangler
 
 */

import org.opencv.core.Core;
import org.opencv.core.Size;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.CvType;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.MatOfPoint;

import java.util.ArrayList;
import java.util.List;
import java.lang.Math;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;

public class ROFRider {
	
	private static final double PERMISSIBLE_REFERENCE_LENGTH = 120;
	private static final double DISTANCE_UPSCALE = 5.2;
	private static final double DEGRADE_CORRECTION = 0;
	private static final int    ADAPTIVE_KERNEL_SIZE = 201; //Magic number
	private static final int    ADAPTIVE_OFFSET = 11;       //Magic number
	private static final int    NOISE_FILTER_DIAMETER = 4;
	private static final double MAX_COMPONENT_MERGE_DISTANCE = 30;
	private static final double MATCH_THRESHOLD = 1.5;
	
	private static final double CE_PERM_BOUND = 4;
	private static final double CE_IN_DOT = 0.6;
	private static final double CE_LINE_MIN_DIST = 20;
	private static final double CE_START_STANDOFF = 5;
	private static final double CE_SAMPLE_SKIP = 4;
	
	private static final double RE_CURVE_APPROX_DIST = 20;
	private static final int    RE_FILTER_SIZE = 3;
	private static final int    RE_WINDOWING = 16;
	private static final double RE_MIN_EDGE_LENGTH = 16;
	private static final double RE_PERCENTAGE_THRESHOLD = 0.85;
	
	public static final int METHOD_REAL_EDGE = 0;
	public static final int METHOD_CALIBRATION_FAST = 1;
	public static final int METHOD_CALIBRATION_ROBUST = 2;
	
	//private double  frameSkip;
	private double  curOrientation;
	private double  curFrame;
	private int     orientationMethod;
	private boolean useVirtualEdge;
	private boolean locateSensor;
	private boolean locateRealEdge;
	private boolean Found_ROF;
	private boolean Found_Sensor;
	
	private List<Double> ROF_TSRI;
	private List<Double> Sensor_TSRI;
	private double MarkToEdge;
	private double MarkToRef;
	private double SensorToEdge;
	private double SensorToRef;
	private double MarkWidth;
	
	/*
	public static void main(String[] args) {
		ROFRider r;
		r = new ROFRider();
		r.init("rofmark_tsri.dat", "sensor_tsri.dat");
		
		r.setLocateRealEdge(false);
		r.setOrientationMethod(ROFRider.METHOD_CALIBRATION_ROBUST);
		
		r.singleFrameDiagnostic(Highgui.imread("sample5.png"));
		System.out.println(r.getMarkToEdge());
		System.out.println(r.getMarkToRef());
		System.out.println(r.getSensorToEdge());
		System.out.println(r.getSensorToRef());
	}
	*/

    // Setter functions
	public void setUseVirtualEdge(boolean s){
		useVirtualEdge = s;
	}
	
	public void setLocateSensor(boolean s){
		locateSensor = s;
	}
	
	public void setLocateRealEdge(boolean s){
		locateRealEdge = s;
	}
	
	public void setOrientationMethod(int m){
		orientationMethod = m;
	}
	
	/*public void setFrameSkip(int s){
		frameSkip = s;
	}*/
	

	public double getMarkToEdge(){
		return (MarkToEdge + DEGRADE_CORRECTION) * (DISTANCE_UPSCALE / MarkWidth);
	}
	
	public double getMarkToRef(){
		return (MarkToRef + DEGRADE_CORRECTION) * (DISTANCE_UPSCALE / MarkWidth);
	}
	
	public double getSensorToEdge(){
		return (SensorToEdge+ DEGRADE_CORRECTION) * (DISTANCE_UPSCALE / MarkWidth);
	}
	
	public double getSensorToRef(){
		return (SensorToRef + DEGRADE_CORRECTION) * (DISTANCE_UPSCALE / MarkWidth);
	}

	public boolean foundROF(){
		return Found_ROF;
	}
	
	public boolean foundSensor(){
		return Found_Sensor;
	}
	
	public void reset(){
		curFrame = 0;
		curOrientation = -1;
	}
	
    //Initialize the class by loading in the two files used for the image processing. These files provided the
    //  "fingerprints" that the program will compare images against
	public void init(String ROFFile, String SensorFile){
		ROF_TSRI = readTSRIfile(ROFFile); //Obtain the ROF "fingerprint"
		Sensor_TSRI = readTSRIfile(SensorFile); //Obtain the sensor "fingerprint"
		MarkToEdge = 0; //Initialize some default values
		MarkToRef = 0;
		SensorToEdge = 0;
		SensorToRef = 0;
		MarkWidth = 1;
		//frameSkip = 1;
		curOrientation = -1;
		curFrame = 0;
		locateRealEdge = true;
		useVirtualEdge = true;
		locateSensor = true;
		orientationMethod = METHOD_REAL_EDGE;
	}
	
	private List<Double> readTSRIfile(String filename) { //Open up the TSRI file,store the data in TSRI_data list and return that list
		Scanner sc;
		List<Double> TSRI_data;
		TSRI_data = new ArrayList<Double>();
		try {
			sc = new Scanner(new File(filename));
			while (sc.hasNextDouble()) {
				TSRI_data.add(sc.nextDouble());
			}
			return TSRI_data;

		} catch (FileNotFoundException e) {
			System.out.println("TSRI file not found.");
			e.printStackTrace();
			return null;
		}
	}

	private void binMorphFilter(Mat image, int size) {
		Mat structElem;
		structElem = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE,
		        new Size(size, size));
		
        //Calls to erode and dilate are known as the opening of an image
        // Potentially this could be replaced with the built in opening function in openCV
        // Imgproc.morphologyEx(image, image, MORPH_OPEN, structElem)
        // This is equivalent since open(src, element) = dilate(erode(src, element));
        //Imgproc.erode(image, image, structElem);
		//Imgproc.dilate(image, image, structElem);
		Imgproc.morphologyEx(image, image, Imgproc.MORPH_OPEN, structElem);
	}

    //Converts the image to a grayscale and then does an adaptive threshold rendering the image
    //  to bytes of either 0 or 255. I believe this would draw out lines and shapes that are 'distinct'
    //  from their surroundings
	private void binTransform(Mat image) {
    
		Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2GRAY); //Convert image to grayscale
        // This appears to be done several times throughout the code. Perhaps worth doing once
        // per image and saving the result temporarily.
        
        // Turns a grayscale image to a binary image. The inputs are in order:
        //  source, destination, maxValue, adaptiveMethod, thresholdType, blocksize, C(onstant)
        // The adaptive thresh mean c means the thresh value is the average of the blocksize by blocksize
        //   area around each point.
        // Binary_INV sets the bit to zero is the point is greater than the thresh value
        //   else it sets it to the max of 255
		Imgproc.adaptiveThreshold(image, image, 255,
		        Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, ADAPTIVE_KERNEL_SIZE,
		        ADAPTIVE_OFFSET);
	}

    
	private double tsriDif(List<Double> alist, Mat blist) {
		int i;
		double rDist;
		double logA;
		double logB;
		double[] values;
		values = new double[7];
		blist.get(0, 0, values); //blist is a collection of 7 Hu invariants
		rDist = 0;
        
        //Computes the distance by summing the differences of the logs of the Hu invariants
        //  I do not not fully understand why this works mathematically but it appears to be
        //  a measure of similarity
		for (i = 0; i < 7; i++) {
			logA = Math.abs(Math.log(Math.abs(alist.get(i)))) * Math.signum(alist.get(i));
			logB = Math.abs(Math.log(Math.abs(values[i]))) * Math.signum(values[i]);
			rDist += Math.abs((1.0 / logA) - (1.0 / logB));
		}
		return rDist;
	}
	
    //Used during the robust form of extractOrientation
	private boolean lineRunLength(double[] lineA, double lineB[], double permBound, double inDot){
		double LBound;
		double RBound;
		Point latL;
		Point latR;
		Point nv;
		Point nvB;
		Point latNV;
		double temp;
		double ddx;
		double midP;
		boolean merge;
		double mag;
		
		nv = new Point(lineA[2] - lineA[0], lineA[3] - lineA[1]); //Midpoint of line A
		
		mag = Math.sqrt(nv.x * nv.x + nv.y * nv.y); //Treat the midpoint as if it were a vector from the origin
                                                    // and compute its magnitude
		nv.x /= mag; //normalize it so nv is now a unit vector pointing to the midpoint of line A
		nv.y /= mag;
		
		latNV = new Point(nv.y, -nv.x); //latNV is point nv rotated 90 degress clockwise
        
        // Imagine the left endpoint of Line A is now a midpoint of a new line with the orientation of latNV
        // latL is the left endpoint of that line and latR is the right endpoint of that line.
        // The length of that line is latNV * permbound
		latL = new Point(lineA[0] - latNV.x * permBound, lineA[1] - latNV.y * permBound);
		latR = new Point(lineA[0] + latNV.x * permBound, lineA[1] + latNV.y * permBound);
		
    	merge = false; //initialize to false
        
		if(Math.abs(nv.x) > Math.abs(nv.y)){ // if the magnitude of x is greater than that of y
			ddx = nv.y / nv.x; //ratio of y to x between in range of (-1, 1)
			midP = lineB[1]; //The y component of B's left end point
			LBound = latL.y + (lineB[0] - latL.x) * ddx; 
			RBound = latR.y + (lineB[0] - latR.x) * ddx; 
			if(LBound > RBound){
				temp = RBound;
				RBound = LBound;
				LBound = temp;
			}
			if((midP >= LBound) && (midP <= RBound)){ //If the y component of B's left point is within the range, merge
				merge = true;
			}
			else{ //else check to see if the right endpoint is in the range by the same math as above
				midP = lineB[3];
				LBound = latL.y + (lineB[2] - latL.x) * ddx; 
				RBound = latR.y + (lineB[2] - latR.x) * ddx; 
				if(LBound > RBound){
					temp = RBound;
					RBound = LBound;
					LBound = temp;
				}
				if((midP >= LBound) && (midP <= RBound)) merge = true; //And if so, merge
			}
		}
		else{ //magnitude of y is greater than x
			ddx = nv.x / nv.y; //Ratio of x to y in range of [-1,1]
			midP = lineB[0]; //The x component of B's left end point
			LBound = latL.x + (lineB[1] - latL.y) * ddx; //Define the left bound as latL's xpoint plus the difference in y values times the inverse slope of nv
			RBound = latR.x + (lineB[1] - latR.y) * ddx; //Define the right bound as LatR's xpoint plus the difference in y values times the inverse slope of nv
            
			if(LBound > RBound){ //If the left bound is greater than the right bound, swap them
				temp = RBound;
				RBound = LBound;
				LBound = temp;
			}
            
			if((midP > LBound) && (midP < RBound)){ //If the midpoint midP is between the left and right bounds, set merge to true
				merge = true;
			}
			else{
				midP = lineB[2]; //Else try again with the midpoint being lineB's other point and  see if it is between the bounds
				LBound = latL.x + (lineB[3] - latL.y) * ddx; 
				RBound = latR.x + (lineB[3] - latR.y) * ddx; 
				if(LBound > RBound){
					temp = RBound;
					RBound = LBound;
					LBound = temp;
				}
				if((midP > LBound) && (midP < RBound)) merge = true; //If so, set merge to true
			}
			
		}
		
		if(merge){ //if merge got set to true
			nvB = new Point(lineB[2] - lineB[0], lineB[3] - lineB[1]); //find the midpoint of B
			
			mag = Math.sqrt(nvB.x * nvB.x + nvB.y * nvB.y);
			nvB.x /= mag;
			nvB.y /= mag; //Normalize the vector pointing to the midpoint
			
            //if the scalar product of the two normalized vectors is less than or equal to inDot, do not merge
			if(Math.abs(nvB.x * nv.x + nvB.y * nv.y) <= inDot) merge = false;
		}
		
		return merge; //Return whether or not the two lines should merge
	}
	
    // Rotate the point pt around an "origin" point 'o' in 2-dimensional space CLOCKWISE by 'a' radians
	private Point rot2D(Point pt, Point o, double a){
		return new Point((o.x - pt.x) * (1 - Math.cos(a)) - (o.y - pt.y) * Math.sin(a)       + pt.x,
					     (o.x - pt.x) * Math.sin(a)       + (o.y - pt.y) * (1 - Math.cos(a)) + pt.y);
	}
	
    
    //If robust is false, it seems to find the average orientation of the lines in the image and returns
    //  the angle of that orientation
    //Obligatory: "Here there be dragons"
	private double extractOrientation(Mat image, boolean robust){
		//Inner class OrientationLine
        //It seems like a and b are two points that form a line. Unsure of the uses of m and l
        class OrientationLine{
			public OrientationLine(double ax, double ay, double bx, double by, double mx, double my, double lx, double ly, double a) {
	            double m;
	            double dx;
	            double dy;
				
				this.ax = ax;
	            this.ay = ay;
	            this.bx = bx;
	            this.by = by;
	            this.mx = mx;
	            this.my = my;
	            this.a  = a;
	            
	            dx = bx - ax; //delta X
	            dy = by - ay; //delta Y
	            
	            m = Math.sqrt(dx*dx + dy*dy); //magniturde
	            nx = dx / m; //Normalization of x
	            ny = dy / m; //Normalization of y
	            mag = m; //store m in magnitude
	            
	            latx = lx;
	            laty = ly;
	            score = 0;
	           
            }
			public OrientationLine(OrientationLine ol) {
				 this(ol.ax, ol.ay, ol.bx, ol.by, ol.mx, ol.my, ol.latx, ol.laty, ol.a);
            }
			public double ax;
			public double ay;
			public double bx;
			public double by;
			public double nx;
			public double ny;
			public double mx;
			public double my;
			public double mag;
			public double latx;
			public double laty;
			public double a;
			public double score;
		}
		
		Mat lines; 
		Mat edgeD; 
		double[] lineA; 
		double[] lineB;
		double[] lineOut;
		double[] calibLoc;
		double angleAcc;
		List<Double> calibSamples;
		double diff;
		double diffAvg;
		double diffVar;
		double bestScore;
		int    bestIndex;
		int i;
		int q;
		int j;
		Point maxPoint;
		Point minPoint;
		Point genCentroid;
		Point p0;
		Point p1;
		Point latNorm;
		Point screenCenter;   //Point in the center of the image
		Point runningPoint;
		Point runningV;
		double permBound;
		double startStandoff;
		double sampleSkip;
		double inDot;
		double minDist;
		List<OrientationLine> lineList;
		OrientationLine ola;
		OrientationLine olb;
		OrientationLine olT;
		
		lines = new Mat();
		edgeD = new Mat();  //Holds data showing the edges of image
		runningPoint = new Point();
		runningV = new Point();
		lineOut = new double[4];
		lineList = new ArrayList<OrientationLine>();
    	calibSamples = new ArrayList<Double>();

	    screenCenter = new Point(image.width() * 0.5, image.height() * 0.5); //

		angleAcc = 0;
		Imgproc.Canny(image, edgeD, 80, 90); //Finds edges in the image. The smallest value between in this
        //  case 80 and 90 is used for edge linking. The largest value is used to find initial
        //  segments of strong edges
		Imgproc.HoughLinesP(edgeD, lines, 0.5, Math.PI/360, 40, PERMISSIBLE_REFERENCE_LENGTH, 30);
        // Finds lines in image using Hough Transformation
        // I'm not sure I understand the other parameters due to the use of magic numbers -- Note from Ali: Magic numbers here are figured out by just plugging in values and seeing what works. Thats what we did in cmpen 454
		
		if(robust){
			
			permBound = CE_PERM_BOUND;
			inDot = CE_IN_DOT;
			minDist = CE_LINE_MIN_DIST;
			startStandoff = CE_START_STANDOFF;
			sampleSkip = CE_SAMPLE_SKIP;
		
		    for (i = 0; i < lines.cols(); i++) 
		    {
		    	lineA = lines.get(0, i);  //Obtain the next line in the file

                        //Possible speed up: The lineA[2]-lineA[0] and some other calculations could be performed here rather
                        //  than recalculated within the nested loop. J is determined by lineA and so it should be moved out.
                double lineA2sublineA0 = lineA[2] - lineA[0];
                double lineA3sublineA1 = lineA[3] - lineA[1];
                if(lineA2sublineA0 > lineA3sublineA1){ //If delta X is greater than delta Y
				// WARNING: This if statement looks like it contains a bug. It would make more sense to compare delta X to delta Y
				// In that case it should be > (lineA[3] - lineA[1]). If someone could confirm this, that would be great.
				// ****Ali note: I changed this to delta X is greater than delta Y.
					j = 0; //Use j to index X values. In this instance we care about the horizontal "direction" of the lines
				}
				else{
					j = 1; //Use j to index Y values. Here we care about the vertical direction of the lines
				}
                
		    	if(lineA[0] != -1){ //Only process the line if x1 is not -1 (must be some invalid point value)
                    //Possible place to speed up performance. This nested loop makes this method O(n^2).
			    	for(q = i + 1; q < lines.cols(); q++){ //Iterate through the remaining 'lines'
						lineB = lines.get(0, q);
						if(lineB[0] != -1){ //Again do not attempt to process a line if it is invalid
							if(lineRunLength(lineA, lineB, permBound, inDot)){ //Check if A should merge with B
								if(lineRunLength(lineB, lineA, permBound, inDot)){ //Check if B should merge with A (is this necessary?)
									
									if(lineA[j] <= lineA[j + 2]){ //If j = 0 this says Does line A point to the right? If j=1 it asks does A point up?
										if(lineB[j] <= lineB[j + 2]){ //If j = 0 this says Does line B point to the right? If j=1 it asks does B point up?
										
											if(lineA[j] <= lineB[j])  //If j = 0 Does A have the leftmost X value? If j =1 Does A have the lower y value?
												minPoint = new Point(lineA[0], lineA[1]); //The minimum value is A's leftmost X value (its first point)
											else
												minPoint = new Point(lineB[0], lineB[1]); //The minimum value is B's leftmost X value (its first)
										}
										else{ //If j = 0 B points to the left. If j=1 B points down
											if(lineA[j] <= lineB[j + 2]) //If j = 0 Is A's leftmost point less than B's left most X? if j =1 Is A's lowest y lower than B's lowest Y?
												minPoint = new Point(lineA[0], lineA[1]); //A's first point is the minimum
											else
												minPoint = new Point(lineB[2], lineB[3]); //B's second poitn is the minimum
										}
									}
									else{ //If j = 0, line A points to the left. If j = 1, line A points down
										if(lineB[j] <= lineB[j + 2]){ //If j=0, does B point right? if j = 1 does B point up?
											if(lineA[j + 2] <= lineB[j]) //If j = 0, is A's leftmost point left of B's left most? if j=1, is A's lower point lower than B's lowest?
												minPoint = new Point(lineA[2], lineA[3]); //If j = 0, A's second point is the left most, if j=1 A's second point is the lowest
											else
												minPoint = new Point(lineB[0], lineB[1]); //If j = 0 B's first point is te left most, if j=1 B's second point is the lowest
										}
										else{ //If j = 0, B points left, if j=1 B points down
											if(lineA[j + 2] <= lineB[j + 2]) //If j = 0, is A's left most point left of B's leftmost point? If j =1, is A's lower point lower than B's lowest?
												minPoint = new Point(lineA[2], lineA[3]); //A's second point is the most left if j=0  or lowest if j=1
											else
												minPoint = new Point(lineB[2], lineB[3]); //B's second point is the most left if j=0  or lowest if j=1
										}
									}
									
									if(lineA[j] > lineA[j + 2]){ //If j = 0, Does A point left, if j=1 does A point down?
										if(lineB[j] > lineB[j + 2]){ //If j =0 Does B point left, if j=1 does B point down?
											if(lineA[j] > lineB[j]) //If j =0, is A's rightmost point right of B's. If j=1 is A's highest point higher than B's?
												maxPoint = new Point(lineA[0], lineA[1]); //If j =0, A's first point is the rightmost. If j=1 A's first point is the highest
											else
												maxPoint = new Point(lineB[0], lineB[1]); //If j=0, B's first point is the rightmost. If j=1 B's first point is the highest
										}
										else{ //If j=0 B points to the right. If j=1 B points down.
											if(lineA[j] > lineB[j + 2]) //If j=0, Is A's rightmost point right of B's rightmost? If j=1, Is A's highest point above B's highest?
												maxPoint = new Point(lineA[0], lineA[1]); //If j=0, A's first point is the rightmost. If j=1, A's first point is the highest.
											else
												maxPoint = new Point(lineB[2], lineB[3]); //If j=0, B's second point is the rightmost, If j=1, B's second point is the highest
										}
									}
									else{ //If j=0, A points to the right. if j=1, A points up.
										if(lineB[j] > lineB[j + 2]){ //If B points to the left or down
											if(lineA[j + 2] > lineB[j]) //If j=0, is A's rightmost point right of B's rightmost? If j=1 is A's highest point above B's highest?
												maxPoint = new Point(lineA[2], lineA[3]); //If j=0, A's second point is the right most, if j=1 A's second point is the highest
											else
												maxPoint = new Point(lineB[0], lineB[1]); //If j=0, B's first point is the right most, if j=1 B's first point is the highest
										}
										else{ //If B points up or to the right
											if(lineA[j + 2] > lineB[j + 2]) //If j=0 is A's rightmost point right of B's? If j=1 is A's highest point above B's highest?
												maxPoint = new Point(lineA[2], lineA[3]); //If j =0, A's second point is the right most, if j=1 A's second point is the highest
											else
												maxPoint = new Point(lineB[2], lineB[3]);//If j=0, B's second point is the right most, if j=1 B's second poitn is the highest
										}
									}
									
									lineOut[0] = -1; lineOut[1] = -1; //Initialize the lineOut values to -1
									lineOut[2] = -1; lineOut[3] = -1;
									
									lines.put(0, q, lineOut); //Invalidate this line
									
									lineOut[0] = minPoint.x; lineOut[1] = minPoint.y;
									lineOut[2] = maxPoint.x; lineOut[3] = maxPoint.y;
									
									lines.put(0, i, lineOut); //Store the new, merged valid line
                                    //WARNING: I do not fully understand why lines are being overwritten with these put statements so I may be misunderstanding this code block entirely
                                    // I think this is just looking for lines that are so close they should be merged and then doing so.
                                    // Ali: houghLinesP is designed to find all lines probablistically, but also creates 'compound' lines as a guess. I believe this is trying to eliminate
                                    // 		the line fragments.
								}
							}
						}
			    	}
		    	}
		    }
		    for (i = 0; i < lines.cols(); i++){ //Iterate through all the columns, calculating orientation lines for the valid ones and adding them to lineList
		    	lineA = lines.get(0, i); //Get the line
		    	if(lineA[0] != -1){ //If the first value is not -1 (so valid)

			    	angleAcc = Math.PI - Math.atan2(lineA[2] - lineA[0], lineA[3] - lineA[1]); //Obtain the angle of the line
			    	if(angleAcc < 0) angleAcc += Math.PI*2; //And make sure it is positive

			    	genCentroid = new Point((lineA[2] + lineA[0]) * 0.5, (lineA[3] + lineA[1]) * 0.5); //Centroid is the line midpoint
			    	p0 = rot2D(genCentroid, screenCenter, angleAcc); //Rotate the centroid around the screen center
			    	p1 = rot2D(genCentroid, screenCenter, angleAcc + Math.PI); //Do so again by an additional pi radians
			   		if(p0.x > p1.x){  //If the first point is greater than the second
			   			p0.x = p1.x; //Overwrite the first points values with those from the second
			   			p0.y = p1.y;
			   			angleAcc += Math.PI; //add Pi to the angle to account for the value from p1
			   			if(angleAcc > (Math.PI * 2)) angleAcc -= Math.PI * 2; //Make sure it is between 0 and 2*pi
			   		}	
			   		latNorm = rot2D(new Point(1, 0), new Point(0, 0), -angleAcc); //Rotate a unit vector into the position of that angle*-1

                                        //Add a new line to lineList with p0 as the first point and latNorm as the second, with the angle included
			   		lineList.add(new OrientationLine(lineA[0], lineA[1], lineA[2], lineA[3], p0.x, p0.y, latNorm.x, latNorm.y, angleAcc));
		    	}
		    }
		    
		    for(i = 0; i < lineList.size(); i++){ //Iterate through lineList and remove lines that are too close in distance but differ by more than inDot
		    	ola = lineList.get(i); //Get each element added to the lineList
		    	q = i + 1; //Set q to be one greater than i
		    	while(q < lineList.size()){ //While q is within the bounds of the lineList

		    		olb = lineList.get(q); //Get the next line

                                //If x1*x2 + y1*y2 > inDot && the distance between the x components of their midpoints is less than minDist
		    		if(((ola.nx * olb.nx + ola.ny * olb.ny) > inDot) && (Math.abs(ola.mx - olb.mx) < minDist)){ 
                        // If the midpoint of lineB is left of As, B will overwrite A in the list.
                        // B will always be removed though                        
		    			if(olb.mx < ola.mx){ //If line B's midpoint is left of line A's midpoint
		    				ola = new OrientationLine(olb); //Reassign orientation line A
		    				lineList.set(i, ola); //And reassign the ith element of the line list to the new line
		    			}
		    			lineList.remove(q); //Then remove the line just examined 
		    		}
		    		else{
		    			q++; //Increment q to look at a different line from lineList
		    		}
		    	}
		    }
		    bestScore = 1000;
		    bestIndex = 0;
		    for(i = 0; i < lineList.size(); i++){ //For each remaining line perform some computations which I do not understand
                                                  //  to determine the line best representing the orientation angle of the image
		    	olT = lineList.get(i);
		    	
		    	runningPoint.x = olT.ax - olT.latx * startStandoff; // Find the "running point"
	    		runningPoint.y = olT.ay - olT.laty * startStandoff;
	    		
	    		diffAvg = 0;
		    	diffVar = 0;
		    	calibSamples.clear();
	    		for(q = 0; q < olT.mag; q += sampleSkip){
		    		runningV.x = runningPoint.x + q * olT.nx;
		    		runningV.y = runningPoint.y + q * olT.ny;
		    		calibLoc = raycast(image, runningV, new Point(olT.latx, olT.laty), 2);
			    	if((calibLoc[0] != -1) || (calibLoc[1] != -1)){
			    		if((calibLoc[0] == -1) || (calibLoc[1] == -1))
			    			diff = 1000;
			    		else
				    		diff = (calibLoc[1] - calibLoc[0]);
				    	diffAvg += diff;
				    	calibSamples.add(diff);
			    	}
		    	}
	    		if(calibSamples.size() != 0){
		    		diffAvg /= calibSamples.size();
		    		for(q = 0; q < calibSamples.size(); q++){
		    			diff = calibSamples.get(q) - diffAvg;
		    			diffVar += (diff * diff) / calibSamples.size();
		    		}
			    	olT.score = Math.sqrt(diffVar);
	    		}
	    		else{
	    			olT.score = 1000;
	    		}
		    	lineList.set(i, new OrientationLine(olT));
		    
		    	if(olT.score < bestScore){
		    		bestIndex = i;
		    		bestScore = olT.score;
		    	}
		    }
            
		    return lineList.get(bestIndex).a;
		}
		else{
			genCentroid = new Point();
			  for (i = 0; i < lines.cols(); i++) {
			    	lineA = lines.get(0, i); //Line has 4 values that are two sets of x and y points x1, y1, x2, y2
                    // Find the angle of the line by finding the theta of the point (x2-x1, y2-y1)
                    //   Keep a running sum of these angles
			    	angleAcc += Math.atan2(lineA[2] - lineA[0], lineA[3] - lineA[1]);
					genCentroid.x += (lineA[2] + lineA[0]) * 0.5; //The point genCentroid is the sum of the line midpoints
					genCentroid.y += (lineA[3] + lineA[1]) * 0.5; //   Add up these points to be averaged later
			  }
			  genCentroid.x = genCentroid.x / i - image.width() * 0.5; //Calculate the average midpoint and reorient it
			  genCentroid.y = genCentroid.y / i - image.height() * 0.5;//  around the center of the image
			  angleAcc = Math.PI - (angleAcc / i); //The cumulative line angles is averaged and then subtracted from pi
			  p0 = rot2D(genCentroid, screenCenter, angleAcc); //Rotate the centroid about the center of the screen by the average angle found
			  p1 = rot2D(genCentroid, screenCenter, angleAcc + Math.PI); //Do again with the opposite angle
			  if(p0.x > p1.x){ //If angleAcc produces a larger x than angleAcc+PI 
				  angleAcc += Math.PI; //Then add PI to angleAcc and make sure it is between 0 and 2PI
				  if(angleAcc > (Math.PI * 2)) angleAcc -= Math.PI * 2;
			  }	
			  return angleAcc; //The return value is this angle

		}	    
	}

    //Merges bordering contour boxes from the list r that are no more than distance d apart
	private void mergeLikeBB(List<Rect> r, double d) {
		int i;
		int j;
		Rect origRect;
		Rect newRect;
		Rect finalRect;
		double rDist;
		boolean dontAdd;

		i = 0;
		while (i < r.size()) {
			origRect = r.get(i); //Get the first rectangle
			dontAdd = false;
			for (j = i + 1; j < r.size(); j++) { //For each remaining rectangle
				newRect = r.get(j); //Get the next rectangle
                //rDist is calculated by taking the difference of the x-coordinate midpoints of the two rectangles and squaring it,
                // then doing the same with the difference of the y-coordinate midpoints of the two, adding the two differences together, and
                // taking the square root of the result. rDist is thus the distance between the midpoint of both rectangles
				rDist = Math.sqrt(Math.pow(
				        ((origRect.x + origRect.width) * 0.5)
				                - ((newRect.x + newRect.width) * 0.5), 2)
				        + Math.pow(((origRect.y + origRect.height) * 0.5)
				                - ((newRect.y + newRect.height) * 0.5), 2));
				if (rDist < d) { //If  that distance is less than d perform the following, else compare the first against the next in the list					
					finalRect = new Rect();
					finalRect.x = origRect.x < newRect.x ? origRect.x : newRect.x; //The x coordinate is the bottom left corner so choose the leftmost x
					finalRect.y = origRect.y < newRect.y ? origRect.y : newRect.y; //The y coordinate is the bottom left corner so choose the lower y
                    //If the bottom right of the original is right of the second, compute the distance from the new rectangle left to the originals and add the width of the original
                    //   Else the second rectangle ends farther to the right so find the width of the final rectangle accordingly
					finalRect.width = (origRect.x + origRect.width) > (newRect.x + newRect.width) ? (origRect.x + origRect.width - finalRect.x)  : (newRect.x + newRect.width - finalRect.x);
                    //Similar computation for the height of the merged rectangle
					finalRect.height = (origRect.y + origRect.height) > (newRect.y + newRect.height) ? (origRect.y + origRect.height - finalRect.y) : (newRect.y + newRect.height - finalRect.y);
                    
					r.set(j, finalRect);//Overwrite the jth element with this merged final Rectangle
					dontAdd = true; //Set true so that the ith rectangle is removed from the list
					break;
				}
			}
            
			if (dontAdd) { //dontAdd will only be set if a rectangle has been merged
				r.remove(i); //Remove the ith rectangle and do not increment since i points to a new rectangle
			} else { //Nothing could be merged with the ith rectangle, increment and search again
				i++;
			}
		}
	}
	
    //Returns an array containing doubles representing some number (up to maxZone) of "roughly continuous" black pixels from point start in the direction of
    //  the vector specified by point v
	private double[] raycast(Mat image, Point start, Point v, int maxZone){
		Point rayCast;
		double d;
		double lineAcc;
		double lineWidth;
		double[] lineX;
		int lineXtick;
		boolean readWhite;
		double[] pixel;
		int i;
	
		rayCast = new Point(start.x, start.y); //Start rayCast at the starting point
		lineAcc = 0;
		lineWidth = 0;
		readWhite = false;
		lineXtick = 0;
		d = 0;
		lineX = new double[maxZone]; //lineX has maxZone elements
		for(i = 0; i < maxZone; i++) lineX[i] = -1; //Initialize them to -1 to indicate they are yet unused
		
        //While the rayCast point is in the image and the maximum number of zones has not been reached
		while((lineXtick < maxZone) && (rayCast.x >= 0) && (rayCast.x < image.width()) 
				                    && (rayCast.y >= 0) && (rayCast.y < image.height())){
                                    
			pixel = image.get((int) rayCast.y, (int) rayCast.x); //WARNING: get is (row,column) so is the ordering correct here?
			if(pixel[0] == 255){ //If the pixel is completely black
				readWhite = true; //Set readWhite to true
				lineAcc += d; //Add d to lineAcc
				lineWidth++; //Increment lineWidth
			} else if(readWhite){ //If readWhite has been set to true and the current pixel is not black
				readWhite = false; //Set readWhite back to false
				lineX[lineXtick] = lineAcc / lineWidth; //Store the "line accumulation" divided by the width in lineX
				lineAcc = 0; //Reset both values
				lineWidth = 0;
				lineXtick++;//Increment lineXtick
			}
			rayCast.x += v.x;//Move rayCast by the vector pointed to by Point v
			rayCast.y += v.y;
			d++; //Increment d although I'm not sure why this is used or why lineAcc exists
		}
		return lineX; //Return the array lineX
	}
	
	
	public void realEdgeFilterStep(Mat image){
		double avgLum;
		Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2GRAY);
		Imgproc.medianBlur(image, image, RE_FILTER_SIZE);
		Imgproc.GaussianBlur(image, image, new Size(RE_FILTER_SIZE, RE_FILTER_SIZE), 0, 0);
		avgLum = Core.mean(image).val[0] * RE_PERCENTAGE_THRESHOLD;
		Imgproc.threshold(image, image, avgLum, 255, Imgproc.THRESH_BINARY);
	}
	
    //Extract the real edge from image with the orientation provided
	public void extractRealEdge(Mat image, Double orientation, Double xdeflect){
		class LineData{ //Inner class LineData
			LineData(){
				a = new Point(0,0);
				b = new Point(0,0);
				dire = new Point(0,0);
				mag = 0;
				hiContrastAccumulator = 0;
				hiContrastSamples = 0;
				loContrastAccumulator = 0;
				loContrastSamples = 0;
			}
			public Point  a;
			public Point  b;
			public Point  dire;
			public double mag;
			public double hiContrastAccumulator;
			public double hiContrastSamples;
			public double loContrastAccumulator;
			public double loContrastSamples;
		};
		
		Mat working;
		List<MatOfPoint> geoEdge;
		List<LineData>   contrastLines;
		LineData         tdata;
		MatOfPoint2f     mopTemp;
		int i;
		int j;
		int xscan;
		int yscan;
		int numContours;
		boolean skipAdd;
		double  proj;
		double  zCross;
		double  lineBestVote;
		int     lineVoteIndex;
		double  edgeContrast;
		double  ort;
		Point   delta;
		Point   projPoint;
		Point[] winding;
		Point   p0;
		Point   p1;
		Point   screenCenter;
		
		geoEdge = new ArrayList<MatOfPoint>();
		mopTemp = new MatOfPoint2f();
		contrastLines = new ArrayList<LineData>();
		tdata = new LineData();
		delta = new Point();
		winding = new Point[2];
		projPoint = new Point();
		
		realEdgeFilterStep(image);
		working = image.clone();

		Imgproc.findContours(working, geoEdge, new Mat(), 
				             Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_TC89_L1); //Extract the contours
                             // Returns all the contours with no hierarchy, uses chain approximation
        
		i = 0; 
		while(i < geoEdge.size()){
			geoEdge.get(i).convertTo(mopTemp, CvType.CV_32FC2); //Convert the image to 
			Imgproc.approxPolyDP(mopTemp, mopTemp, RE_CURVE_APPROX_DIST, true); //Approximate a polygon using Douglas-Peucker
		    mopTemp.convertTo(geoEdge.get(i), CvType.CV_32S); //I do not nderstand the CvType conversions
		    //.cols might be .rows... <- One of the only comments provided by original author. Preserve for history.
		    if(geoEdge.get(i).rows() <= 1)
		    	geoEdge.remove(i);
		    else
		    	i++;
		}
		
		for(i = 0; i < geoEdge.size(); i++){ //For each geoedge
			numContours = geoEdge.get(i).rows(); //Get the number of contours
			for(j = 0; j < numContours; j++){ //Loop through each contour
				tdata.a = geoEdge.get(i).toArray()[j]; //tdata.a is the first
				tdata.b = geoEdge.get(i).toArray()[(j + 1) % numContours]; //tdata.b is the next (loops so last compares with first)
				
				tdata.dire.x = tdata.b.x - tdata.a.x; //Store delta x as the x direction
				tdata.dire.y = tdata.b.y - tdata.a.y; //Store delta y as the y direction
				
                //Compute the magnitude of the direction
				tdata.mag = Math.sqrt(tdata.dire.x * tdata.dire.x + tdata.dire.y * tdata.dire.y);
                
                //And normalize the direction
				tdata.dire.x = tdata.dire.x / tdata.mag;
				tdata.dire.y = tdata.dire.y / tdata.mag;
				
				skipAdd = false;
				
                //Add tdata unless it violates one of these conditional statements
				if(tdata.mag < RE_MIN_EDGE_LENGTH)
					skipAdd = true;
				else if((tdata.a.y < RE_WINDOWING) && (tdata.b.y < RE_WINDOWING))
					skipAdd = true;
				else if((tdata.a.x > (working.width() - RE_WINDOWING)) && (tdata.b.x > (working.width() - RE_WINDOWING)))
					skipAdd = true;
				else if((tdata.a.y > (working.height() - RE_WINDOWING)) && (tdata.b.y > (working.height() - RE_WINDOWING)))
					skipAdd = true;
				else if((tdata.a.x < RE_WINDOWING) && (tdata.b.x < RE_WINDOWING))
					skipAdd = true;
				
				if(!skipAdd) contrastLines.add(tdata); //If skipAdd was not set to true, add it
			}
		}
        
        working = image.clone();     
        //WARNING: Triple for loop going over every pixel. Use as little as possible.        
		for(yscan = 0; yscan < working.height(); yscan++){ //scan through the height of the image
			for(xscan = 0; xscan < working.width(); xscan++){ //scan through the width of the image
				for(i = 0; i < contrastLines.size(); i++){ //For each contrast line
					delta.x = xscan - contrastLines.get(i).a.x; //Find the deltas of the current scan position from the line
					delta.y = yscan - contrastLines.get(i).a.y;
					
                    // Projection?
					proj = (delta.x * contrastLines.get(i).dire.x) + (delta.y * contrastLines.get(i).dire.y);
					
                    //If the projection is positive and greater than the magnitude
					if((proj < contrastLines.get(i).mag) && (proj >= 0)){
                        //I do not understand this portion
						projPoint.x = contrastLines.get(i).a.x + proj * delta.x;
						projPoint.y = contrastLines.get(i).a.y + proj * delta.y;
						
						winding[0] = new Point(contrastLines.get(i).b.x - contrastLines.get(i).a.x,
								               contrastLines.get(i).b.y - contrastLines.get(i).a.y);
						winding[1] = new Point(projPoint.x - contrastLines.get(i).b.x, 
								               projPoint.y - contrastLines.get(i).b.y);

						zCross = winding[0].x * winding[1].y - winding[0].y * winding[1].x;
						if(zCross >= 0){
							contrastLines.get(i).hiContrastAccumulator += working.get(yscan, xscan)[0];
							contrastLines.get(i).hiContrastSamples++;
						}
						else{
							contrastLines.get(i).loContrastAccumulator += working.get(yscan, xscan)[0];
							contrastLines.get(i).loContrastSamples++;
						}
					}
				}
			}
		}

		lineBestVote = 0;
		lineVoteIndex = 0;
		for(i = 0; i < contrastLines.size(); i++){ //For each contrast line
            //Assign to the accumulators their average values
			contrastLines.get(i).hiContrastAccumulator /= contrastLines.get(i).hiContrastSamples;
			contrastLines.get(i).loContrastAccumulator /= contrastLines.get(i).loContrastSamples;
            //Get the difference of the average contrasts
			edgeContrast = Math.abs(contrastLines.get(i).hiContrastAccumulator - contrastLines.get(i).loContrastAccumulator);
            //Keep a record of the highest contrast
			if(edgeContrast > lineBestVote){
				lineBestVote = edgeContrast;
				lineVoteIndex = i;
			}
		}
        
        //If more than one contrast line exists
		if(contrastLines.size() > 0){
		    screenCenter = new Point(image.width() * 0.5, image.height() * 0.5);
            
            //Get and normalize the orientation angle
			ort = Math.atan2(contrastLines.get(lineVoteIndex).dire.x, contrastLines.get(lineVoteIndex).dire.y);
			if(ort < 0){
				ort = ort + Math.PI * 2;
			}
			ort = Math.PI - ort;
            
            //Find the two possible points and store the correct one, again correcting the orientation angle
			p0 = rot2D(contrastLines.get(lineVoteIndex).a, screenCenter, ort);
			p1 = rot2D(contrastLines.get(lineVoteIndex).a, screenCenter, ort + Math.PI);
			if(p0.x > p1.x){
				p0.x = p1.x;
				p0.y = p1.y;
				ort += Math.PI;
				if(ort > (Math.PI * 2)) ort -= Math.PI * 2;
			}	
			orientation = new Double(ort); //Return the orientation found
			xdeflect = new Double(p0.x); //Return the xdeflect found 
		}
	}
	
	
	public void singleFrameDiagnostic(Mat ROF_Img) { //Process a single image frame

		Mat working; //Contains a copy of the parameter ROF_Img. Program seems to be using ROF_Img anyway most of the time. Intentional?
		Mat rotation; //An affine matrix of 2D rotation
		Mat extraction; //An extracted subdivision of ROF_Img
		List<MatOfPoint> components;
		List<Rect> bbFirstPass;

		Rect ROFRect;
		Rect SensorRect;
		double curFit;
		
		int ROFIndex;
		double ROFFit;
		int ROFIndex2;
		double ROFFit2;
		
		int SensorIndex;
		double SensorFit;
		int SensorIndex2;
		double SensorFit2;
		
		Double realOrientation;
		Double realXDeflect; //Value is set during extractRealEdge method

		double[] lineX;
		
		int i;
		Moments transInvM; //Moments taken from Mat extraction
		Mat tsrInvM; //Stores the result of Humoments performed on transInvM

		components = new ArrayList<MatOfPoint>();
		bbFirstPass = new ArrayList<Rect>();
		tsrInvM = new Mat();
		ROFRect = new Rect();
		SensorRect = new Rect();
		realOrientation = new Double(0);
		realXDeflect = new Double(0);
		
		working = ROF_Img.clone();
        
        //If locate real edge has been selected, call the extraction method
		if(locateRealEdge){
			extractRealEdge(working, realOrientation, realXDeflect);
		}
		
		binTransform(ROF_Img); //Convert the image to a gray scale and thresh the values so
                               //  that the byte representation is either all 0s or all Fs (pure white or pure black)
		binMorphFilter(ROF_Img, NOISE_FILTER_DIAMETER); //Perform an opening of the image based around an elliptical structuring elements
                                                        //  with the size set by the NOISE_FILTER_DIAMETER

        //Default frameSkip value is 1 so the first part will always default to true unless it is changed by a call to setFrameSkip
		/*if((curFrame % frameSkip == 0) || (curOrientation == -1)){ //Also performed if the current Orientation is not set
			if(locateRealEdge){ //If set to locate a Real Edge
				switch(orientationMethod){
					case METHOD_REAL_EDGE:
						curOrientation = realOrientation.doubleValue(); //If locateRealEdge was set the real edge has already been
                        // extracted and realOrientation holds the orientation that was found
						break;
					case METHOD_CALIBRATION_FAST:
						curOrientation = extractOrientation(ROF_Img, false); //Perform a fast, non-robust orientation calculation
						break;
					case METHOD_CALIBRATION_ROBUST:
						curOrientation = extractOrientation(ROF_Img, true); //Perform a robust orientation calculation
						break;
				}
			}
			else{
				switch(orientationMethod){
				case METHOD_REAL_EDGE:
					curOrientation = extractOrientation(ROF_Img, true);//Perform a robust orientation calculation
					break;
				case METHOD_CALIBRATION_FAST:
					curOrientation = extractOrientation(ROF_Img, false);//Perform a fast, non-robust orientation calculation
					break;
				case METHOD_CALIBRATION_ROBUST:
					curOrientation = extractOrientation(ROF_Img, true);//Perform a robust orientation calculation
					break;
			}
				
			}
		}*/

		if(locateRealEdge) {
			curOrientation = realOrientation.doubleValue();
		}
		else {
			switch(orientationMethod) {
			case METHOD_REAL_EDGE:
				curOrientation = extractOrientation(ROF_Img, true);
				break;
			case METHOD_CALIBRATION_FAST:
				curOrientation = extractOrientation(ROF_Img, false);
				break;
			case METHOD_CALIBRATION_ROBUST:
				if(curOrientation == -1) {
					curOrientation = extractOrientation(ROF_Img, true);				//Performs a robust orientation only if fast orientation varies from previously known
				} else {															// orientation by more than 5%
					double tempOrientation = extractOrientation(ROF_Img, false);
					if(tempOrientation > curOrientation) {
						if((curOrientation/tempOrientation) < .95) {
							curOrientation = extractOrientation(ROF_Img, true);
						}
					} else {
						if((tempOrientation/curOrientation) < .95) {
							curOrientation = extractOrientation(ROF_Img, true);
						}
					}
				}
			}
		}
        
        // Rotation = The matrix to rotate around the point that is the center of ROF_Img with its
        //   current orientation (must be transformed into degrees) with a scale of 1 to 1
        //   A positive angle (in this case curOrientation) corresponds to a counter-clockwise rotation
		rotation = Imgproc.getRotationMatrix2D(new Point(ROF_Img.width()*0.5, ROF_Img.height()*0.5), 
				                               curOrientation * (180 / Math.PI), 
				                               1);
		
        //Apply an affine transformation of ROF_Img and overwrite ROF_Img with the result. The
        //  transformation will be the rotation matrix computed above. The same size will be preserved.
		Imgproc.warpAffine(ROF_Img, ROF_Img, rotation, new Size(ROF_Img.width(), ROF_Img.height()));
		
		
		working = ROF_Img.clone(); //Clone the current image
        
        //Extract the contours from the clone of ROF_Img and store the contours in the components list.
        //  The clone is needed since the source image is MODIFIED by this procedure
        //  The new Mat() is just an optional output for topology information.
        //  RETR_EXTERNAL retrieves only the extreme outer contours
        //  CHAIN_APPROX_NONE means all contour points will be listed (distance between points equals 1)
		Imgproc.findContours(working, components, new Mat(), Imgproc.RETR_EXTERNAL,
		                     Imgproc.CHAIN_APPROX_NONE);

		for (i = 0; i < components.size(); i++) {
            //For each contour in components, add the bounding rectangle of the contour
            //  to bbFirstPass
			bbFirstPass.add(Imgproc.boundingRect(components.get(i)));
		}
		mergeLikeBB(bbFirstPass, MAX_COMPONENT_MERGE_DISTANCE); //Combine the boundary rectangles that are within MAX_COMPONENT_MERGE_DISTANCE of each other

		ROFFit = MATCH_THRESHOLD;
		ROFFit2 = MATCH_THRESHOLD;
		ROFIndex = 0;
		ROFIndex2 = 0;
		
		SensorFit = MATCH_THRESHOLD;
		SensorFit2 = MATCH_THRESHOLD;
		SensorIndex = 0;
		SensorIndex2 = 0;

		
		for (i = 0; i < bbFirstPass.size(); i++) { //Iterate through the reduced set of rectangles
			if((bbFirstPass.get(i).width >= 10) && (bbFirstPass.get(i).height >= 10)){ //If the height and width of the ith rectangle are both greater than 10 pixels		//We'll need to fix this for the sony camera I think
				extraction = new Mat(ROF_Img, bbFirstPass.get(i)); //Extract that rectangle
				transInvM = Imgproc.moments(extraction, true);//Calculates the moments of the extraction up to the third order
				
				Imgproc.HuMoments(transInvM, tsrInvM);//Calculates seven Hu invariants and stores them in tsrInvM
				
				curFit = tsriDif(ROF_TSRI, tsrInvM); //Computes the difference between these moments and the fingerprint ROF_TSRI
				if(curFit < MATCH_THRESHOLD){ //If the current Fit is less than the match threshold
					if (curFit < ROFFit) { //If the value is less than the current ROFFit
						ROFIndex = i; //Store the index of this rectangle
						ROFFit = curFit; //And record the new closest fit
					} else if (curFit < ROFFit2){ //Record the second closest fitting rectangles index and fit as well
						ROFIndex2 = i;
						ROFFit2 = curFit;
					}
				}
                
				if(locateSensor){ //If the sensor is to be located
					curFit = tsriDif(Sensor_TSRI, tsrInvM); //Recompute the current fit by comparing tsrInvM to the Sensor_TSRI
					if(curFit < MATCH_THRESHOLD){ //Like above, if the current Fit is less than the threshold
						if (curFit < SensorFit) { //Compare it with the best and second best fit found thus far and store it accordingly 
							SensorIndex = i;
							SensorFit = curFit;
						} else if (curFit < SensorFit2){
							SensorIndex2 = i;
							SensorFit2 = curFit;
						}
					}
				}
                
			}
			else{ //The rectangle is not large enough and can be discarded
                //Fills in the rectangle completely with all black, modifying ROF_Img 
				Core.rectangle(ROF_Img, new Point(bbFirstPass.get(i).x, bbFirstPass.get(i).y), 
						                new Point(bbFirstPass.get(i).x + bbFirstPass.get(i).width, 
				                                  bbFirstPass.get(i).y + bbFirstPass.get(i).height),
				                        new Scalar(0),
				                        Core.FILLED);
			}
		}
        
		if(locateSensor){ //If the sensor is being located
			if(SensorIndex == ROFIndex){ //If the same rectangle represents both the Sensor and ROF
				if(SensorFit < ROFFit){ //If is is a closer match to the Sensor, replace the ROFIndex with its second best matching index
					ROFIndex = ROFIndex2;
				}else{
					SensorIndex = SensorIndex2; //If the ROFFit is better, make SensorIndex that of its second closest match
				}
			}
			SensorRect = bbFirstPass.get(SensorIndex); //Store the rectangle at the SensorIndex in the SensorRect rectangle
		}
		
		ROFRect = bbFirstPass.get(ROFIndex); //Store the rectangle containing the best guess at the ROF in ROFRect
		if(ROFFit != MATCH_THRESHOLD){ //If the ROFFit has been changed from the initial MATCH_THRESHOLD
            //Call the raycast function on the image starting just left of the ROFRect's lower left corner and halfway up that rectangle. The
            //  vector angle is <-1, 0> so it heads directly left. The maxZone is 2.
			lineX = raycast(ROF_Img, new Point(ROFRect.x - 1, ROFRect.y + ROFRect.height * 0.5), new Point(-1, 0), 2);
			if(locateRealEdge) //If we are locating a real edge
				MarkToEdge = (ROFRect.x + ROFRect.width * 0.5) - realXDeflect.doubleValue(); //Subtract the center of the ROF rectangles X position from the real edge position
			else
				MarkToEdge = lineX[0] + ROFRect.width * 0.5; //Else use the first raycast distance
			
			if(useVirtualEdge)
				MarkToRef = (ROFRect.x + ROFRect.width * 0.5); //The virtual edge MarkToRef distance is just the position of the ROF's center
			else
				MarkToRef = lineX[1] + ROFRect.width * 0.5; //Add half the width of the mark to the second distance found using raycast
			
			MarkWidth = ROFRect.width; //Set the width of the mark
			Found_ROF = true; //ROF was found
		}
		else{
			Found_ROF = false; //No ROF was found
		}
		if(locateSensor){
			if((SensorFit != 20.0)){ //WARNING: This 20.0 seems to be an old hard-coded value. This may be an error since it is initially set to MATCH_THRESHOLD which is not 20. May falsely report Found_Sensor
                //WARNING: Since lineX is only used if locateRealEdge is false or useVirtualEdge is false, we should be checking for those flags before calling this function
				lineX = raycast(ROF_Img, new Point(SensorRect.x - 1, SensorRect.y + SensorRect.height * 0.5), new Point(-1, 0), 2);
				if(locateRealEdge)
					SensorToEdge = (SensorRect.x + SensorRect.width * 0.5) - realXDeflect.doubleValue(); //The sensor to edge is the distance from the center of the sensor to the real edge(realXDeflect) found earlier
				else
					SensorToEdge = lineX[0] + SensorRect.width * 0.5; //With no real edge, just take the first ray length and add half the sensor width to it 
				
				if(useVirtualEdge)
					SensorToRef = (SensorRect.x + SensorRect.width * 0.5); //If using a virtual edge just use the X coordinate of the SensorRect's center point
				else
					SensorToRef = lineX[1] + SensorRect.width * 0.5; //Else add the second distance from the raycast to half the width of the sensor (I do not understand raycast well enough to understand this)
				
				Found_Sensor = true; //The sensor was found so set it to True
			}
			else{
				Found_Sensor = false; //The sensor was not found so false
			}
		}
		else{
			Found_Sensor = false; //We weren't looking for it so no we did not find it.
		}
	
		curFrame++; //Increment the frame indicating that this one has been analyzed
	}
}