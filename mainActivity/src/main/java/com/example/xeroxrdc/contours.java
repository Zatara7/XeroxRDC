package com.example.xeroxrdc;

/**
 * Created by Zatara7 on 4/23/2015.
 */
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageView;
import android.app.Activity;
import android.graphics.Bitmap;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.features2d.*;
import org.opencv.calib3d.*;
import org.opencv.highgui.*;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class contours extends Activity {
    private ArrayList<Rect> rectBoundaries;
    int imgWidth, imgHeight, analysisHeight, analysisWidth;

    // For the reference selection
    private RadioGroup rg;
    Context context = this;
    int radioValue;
    RadioButton rb;
    String rbValue;
    boolean yes = true;
    List<String> list = new ArrayList<>();

    public void onRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();
        switch (view.getId()) {
            case R.id.radio_yes:
                if (checked) {
                    if(yes) {
                        yes = false;
                        rbValue = "yes";
                        Toast.makeText(getApplicationContext(),
                                "Selected Yes", Toast.LENGTH_SHORT).show();
                    } else {

                        //Alert dialog if the reference object is already selected
                        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                        alertDialog.setTitle("Alert");
                        alertDialog.setMessage("Reference object already set");
                        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                alertDialog.dismiss();
                            }
                        });

                        alertDialog.show();
                    }
                    break;
                }
            case R.id.radio_no:
                if (checked) {
                    rbValue = "no";
                    Toast.makeText(getApplicationContext(),
                            "Selected No", Toast.LENGTH_SHORT).show();
                    break;
                }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contours);
        final ImageView iv = (ImageView) findViewById(R.id.imageView1);

        iv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    imgWidth = iv.getMeasuredWidth();
                    imgHeight = iv.getMeasuredHeight();

                    int xx, yy, offset;
                    double ratio;

                    ratio = ((double)analysisWidth / (double)imgWidth);
                    if(ratio < ((double)analysisHeight / (double)imgHeight)) {
                        ratio = (double)analysisHeight / (double)imgHeight;
                        offset = (int)(imgWidth - (analysisWidth / ratio)) / 2;
                        xx = (int)((event.getX() - offset) * ratio);
                        yy = (int)(event.getY() * ratio);
                    } else {
                        offset = (int)(imgHeight - (analysisHeight / ratio)) / 2;
                        xx = (int) (event.getX() * ratio);
                        yy = (int) ((event.getY() - offset) * ratio);
                    }

                    System.out.println("I am in the listener!");
                    int checker = 0;

                    for (int i = 0; i < rectBoundaries.size(); i++) {
                        Rect j = rectBoundaries.get(i);

                        if (j.contains(new Point(xx, yy))) {
                            System.out.println("Rect X: " + j.x + " Rect Y: " + j.y + " Rect width:" + j.width + " Rect height:" + j.height);
                            checker = 1;
                        }
                    }

                    if (checker == 1) {
                        object_clicked();

                    } else {
                        System.out.println("Object was not clicked!");
                        Toast.makeText(getApplicationContext(), "Object wasnt clicked!", Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            }
        });
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS ) {
                // now we can call opencv code !
                find_contours();
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_7,this, mLoaderCallback);
        // you may be tempted, to do something here, but it's *async*, and may take some time,
        // so any opencv call here will lead to unresolved native errors.
    }

    public void find_contours() {
        Rect r;
        // Open image
        Bitmap mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.aligned1);

        Mat img = new Mat();
        Mat newImg = new Mat();
        Utils.bitmapToMat(mBitmap, img);

        //Use canny edge detector to detect borders
        Mat canny = new Mat();
        Imgproc.Canny(img, canny, 50, 150);

        //Store the borders as Contours
        ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(canny, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        //draw the contours on the image so you can see the objects
        Imgproc.drawContours(img, contours, -1, new Scalar(255, 0, 0));

        // convert back to bitmap
        Utils.matToBitmap(img, mBitmap);
        analysisHeight = mBitmap.getHeight();
        analysisWidth = mBitmap.getWidth();

        // find the imageview and draw it!
        final ImageView iv = (ImageView) findViewById(R.id.imageView1);
        iv.setImageBitmap(mBitmap);

        // Store the contour boundaries
        rectBoundaries = new ArrayList<Rect>();
        r = new Rect();

        for (int i = 0; i < contours.size(); i++) {
            r = Imgproc.boundingRect(contours.get(i));
            double height, width;
            height = (double)analysisHeight * 0.02;
            width = (double)analysisWidth * 0.02;
            r.x = (r.x - (int)width);
            r.width = (r.width + (int)(width * 2));
            r.y = (r.y - (int)height);
            r.height = (r.height + (int)(height * 2));
            rectBoundaries.add(r);
        }
    }

    public void object_clicked() {
        LayoutInflater layout = LayoutInflater.from(contours.this);
        View promptView = layout.inflate(R.layout.activity2, null);
        //AlertDialog to get the name of the object and set it as reference object or not

        final AlertDialog.Builder alert = new AlertDialog.Builder(contours.this);
        alert.setView(promptView);
        final EditText editText = (EditText) promptView.findViewById(R.id.edit1);

        alert.setTitle("Popup");

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                String editValue = editText.getText().toString().trim();
                // if the object name entry is blank

                if (editValue == null || editValue.equals("")) {

                    final AlertDialog alertName = new AlertDialog.Builder(contours.this).create();
                    alertName.setTitle("Alert");
                    alertName.setMessage("Name should not be blank");
                    alertName.setButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            alertName.dismiss();
                        }
                    });

                    alertName.show();

                }
                // if the object name is entered
                else {
                    try {
                        //Internal storage file in the path /data/data/package_name/files/user_input.txt
                        File file = context.getFileStreamPath("user_input.txt");
                        if (file == null || !file.exists()) {
                            try {
                                OutputStreamWriter os = new OutputStreamWriter(openFileOutput("user_input.txt", Context.MODE_APPEND));
                                os.write(editValue);
                                os.write(" ");
                                os.write(rbValue);
                                os.write("\n");
                                os.close();
                                Toast.makeText(contours.this, "Entry saved", Toast.LENGTH_LONG).show();
                            } catch (Throwable t) {

                                Toast.makeText(contours.this, "Exception: " + t.toString(), Toast.LENGTH_LONG).show();
                            }
                        } else {
                            try {
                                BufferedReader bf = new BufferedReader(new InputStreamReader(openFileInput("user_input.txt")));
                                String str;

                                while ((str = bf.readLine()) != null) {

                                    StringTokenizer st = new StringTokenizer(str);
                                    while (st.hasMoreTokens()) {

                                        list.add(st.nextToken());
                                    }
                                }
                                //To check if the object name already exists or not

                                if (list.contains(editValue)) {
                                    Toast.makeText(contours.this, "Name already exists.", Toast.LENGTH_LONG).show();
                                }

                                //write the input from the user to the file for further processing
                                else {
                                    try {
                                        OutputStreamWriter os = new OutputStreamWriter(openFileOutput("user_input.txt", Context.MODE_APPEND));
                                        os.write(editValue);
                                        os.write(" ");
                                        os.write(rbValue);
                                        os.write("\n");
                                        os.close();
                                        Toast.makeText(contours.this, "Saved", Toast.LENGTH_LONG).show();

                                    } catch (Throwable t) {

                                        Toast.makeText(contours.this, "Exception: " + t.toString(), Toast.LENGTH_LONG).show();
                                    }
                                }
                                bf.close();
                            }
                            catch (Throwable t) {
                                Toast.makeText(contours.this, "Exception: " + t.toString(), Toast.LENGTH_LONG).show();
                            }
                        }//else
                    } catch (Throwable t) {
                        Toast.makeText(contours.this, "Exception: " + t.toString(), Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        alert.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alert.create();
        alert.show();
    }
}