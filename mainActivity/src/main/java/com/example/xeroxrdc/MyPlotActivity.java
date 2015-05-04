package com.example.xeroxrdc;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A straightforward example of using AndroidPlot to plot some data.
 */
public class MyPlotActivity extends Activity
{
    private int imgSelect = 0;
    private static final String TAG = "PlotActivity";
    private View rootView;
    private XYPlot plot;
    private List<String> strFiles;
    private List<Integer> colors = Arrays.asList(Color.WHITE, Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.MAGENTA, Color.CYAN);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Used to set the view for the plot to be displayed in the application
     * @param v - The view in which you would like to add the plot
     */
    public void setView(View v) {
        rootView = v;
    }

    public void initializePlot(String file, List<String> files) {
        // initialize our XYPlot reference:
        plot = (XYPlot) rootView.findViewById(R.id.lineChart);
        XmlObjects xmlObjects;
        Integer colorSelector;
        strFiles = files;

        System.out.println("From the plot: " + file);

        XmlDataFile xmlDataFile = new XmlDataFile();
        xmlObjects = xmlDataFile.data(file);

        int iterations = xmlObjects.getXmlObjects().size()/colors.size();
        if(iterations == 0) iterations = 1;

        for(int j = 0; j < iterations; j++) {
            for (int i = 0; i < xmlObjects.getXmlObjects().size(); i++) {
                colorSelector = colors.get(i);
                XmlObject xmlObject = xmlObjects.getXmlObjects().get(i);
                List seriesData = xmlObject.getDistances();
                XYSeries series = new SimpleXYSeries(seriesData, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, xmlObject.getName());
                plot.addSeries(series, new LineAndPointFormatter(colorSelector, colorSelector, null, new PointLabelFormatter(colorSelector)));
            }
        }


        plot.setDomainLabel("Image Number");
        plot.setDomainStep(XYStepMode.INCREMENT_BY_VAL, 1);
        plot.setRangeLabel("Pixels");
        plot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, 100);
        plot.setRangeBottomMax(0.0);
        plot.setRangeTopMin(5.0);
        plot.setTicksPerRangeLabel(4);
        plot.setTitle("Distance to Reference");
        plot.getGraphWidget().setDomainLabelOrientation(-45);
        plot.redraw();
        setImageSelector();
    }

    private void setImageSelector() {
        plot.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                float x = motionEvent.getX();
                float y = motionEvent.getY();
                XYGraphWidget widget = plot.getGraphWidget();
                RectF gridRect = widget.getGridRect();
                if(gridRect.contains(x,y)) {
                    long newX = Math.round(widget.getXVal(x));
                    Log.d(TAG, "Touched at: " + x + ", " + y + ".  Target X val = " + newX);
                    ImageView topImg, botImg;
                    topImg = (ImageView) rootView.findViewById(R.id.imageView1);
                    botImg = (ImageView) rootView.findViewById(R.id.imageView2);

                    File f = new File(strFiles.get((int)newX));
                    Bitmap bitmap = null;

                    try {
                        bitmap = BitmapFactory.decodeStream(new FileInputStream(f), null, null);
                    } catch(Exception e) {
                        e.printStackTrace();
                    }

                    if(imgSelect == 0) {
                        topImg.setImageBitmap(bitmap);
                        imgSelect = 1;
                    } else {
                        botImg.setImageBitmap(bitmap);
                        imgSelect = 0;
                    }
                }
                return false;
            }
        });
    }

    public XYPlot getXyPlot() {
        return plot;
    }
}