package com.command.jddd.opencv;

import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.view.WindowManager;
import android.view.View;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class imgActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{

    private static final String TAG = "OCVSample";

    int screenWidth;
    int screenHeight;

    double loader = 0; // Double for the moment instead of bool

    JavaCameraView javaCameraView;
    ImageView iv;

    // Holds the best Mat to make comparisons
    Mat camLayer;
    Mat animLayer;
    Mat maskLayer;

    Scalar COLOR_RED = new Scalar(255,0,0,255);
    Scalar COLOR_GREEN = new Scalar(81, 190, 0);
    Scalar COLOR_ORANGE = new Scalar(255,140,0);
    Scalar COLOR_BLUE = new Scalar(123,104,238);
    Scalar COLOR_YELLOW = new Scalar(255, 255, 0);

    List<MatOfPoint> contours;
    List<MatOfPoint> mContours;
    List<MatOfPoint> maskContours;

    Point[] imgPtsArray;
    List<Point> imgPtsList;

    AssetManager assetManager;

    int count = 0;
    ArrayList<Mat> animation;
    Iterator<Mat> animItr;
    Mat t;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_img);
        javaCameraView = (JavaCameraView)findViewById(R.id.camera_view);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);
        javaCameraView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                // ... Respond to touch events
                Log.e(TAG, "  Touch event");
                //Starting a new Intent
                Intent animScreen = new Intent(getApplicationContext(), animActivity.class);

                //Sending data to another Activity
                // contourScreen.putExtra("name", inputName.getText().toString());
                // contourScreen.putExtra("email", inputEmail.getText().toString());

                startActivity(animScreen);
                return true;
            }
        });
        // iv.setVisibility(SurfaceView.VISIBLE);
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "  OpenCVLoader.initDebug(), not working.");
        } else {
            Log.d(TAG, "  OpenCVLoader.initDebug(), working.");
        }
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause ...");
        super.onPause();
        if (javaCameraView != null)
            javaCameraView.disableView();
    }

    public void onDestroy() {
        Log.i(TAG, "onDestroy ...");
        super.onDestroy();
        if (javaCameraView != null)
            javaCameraView.disableView();
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume ...");
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            Log.i(TAG, "Status:  " + status);
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    javaCameraView.enableView();
                    Log.i(TAG, "LoaderCallbackInterface Success");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.i(TAG, "Camera View Started ...");
        try {
            Log.i(TAG, "Camera width ... " + Integer.toString(width));
            Log.i(TAG, "Camera height ... " + Integer.toString(height));
            initialize(width, height);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCameraViewStopped() {
        Log.i(TAG, "Camera View Stopped ...");
    }



    public void initialize(int width, int height) throws IOException {

        screenWidth = width;
        screenHeight = height;
        imgPtsList = new ArrayList<>();
        loader = 100;
    }

    public Mat processInputFrame(Mat inputFrame) {

        Mat hierarchy = new Mat();

        camLayer = new Mat();
        maskLayer = new Mat();
        animLayer = inputFrame.clone();

        contours = new ArrayList<>();
        mContours = new ArrayList<>();
        maskContours = new ArrayList<>();

        Imgproc.cvtColor(animLayer, animLayer, Imgproc.COLOR_RGB2GRAY);
        Imgproc.blur(animLayer, animLayer, new Size(13, 13));
        Imgproc.threshold(animLayer, animLayer, 100, 255, Imgproc.THRESH_BINARY);
        // Imgproc.Canny(findLayer, findLayer, 10.0, 30.0);

        Imgproc.findContours(animLayer, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        // Imgproc.drawContours(animLayer, contours, -1, COLOR_RED, 5);

        MatOfPoint biggestCt = new MatOfPoint();
        double biggestCtArea = 0;
        for (MatOfPoint c : contours) {
            double ctArea = Imgproc.contourArea(c);
            if(ctArea > biggestCtArea)
            {
                biggestCtArea = ctArea;
                biggestCt = c;
            }
        }
        mContours.add(biggestCt);
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        MatOfPoint2f contour2f = new MatOfPoint2f( biggestCt.toArray() );

        if (contour2f.checkVector(contour2f.channels(), contour2f.depth()) > 0) {

            double approxDistance = Imgproc.arcLength(contour2f, true)*0.02;
            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);
            MatOfPoint points = new MatOfPoint( approxCurve.toArray() );

            if (points.total() == 4) {

                // Imgproc.drawContours(animLayer, mContours, -1, COLOR_RED, 3);
                ArrayList<Point> pList = new ArrayList<>(points.toList());

                Point topLeft = pList.get(0);
                double topLeftDis = pointDistance(topLeft);
                Point bottomRight = pList.get(0);
                double bottomRightDis = pointDistance(bottomRight);

                Point topRight = pList.get(0);
                Point bottomLeft = pList.get(0);

                for(Point p : pList) {
                    // Closest distance: Top Left
                    double newTopLeftDis = pointDistance(p);
                    if( newTopLeftDis < topLeftDis){
                        topLeftDis = newTopLeftDis;
                        topLeft = p;
                    }
                    // Further distance: Bottom Right
                    double newBottomRightDis = pointDistance(p);
                    if( newBottomRightDis > bottomRightDis){
                        bottomRightDis = newBottomRightDis;
                        bottomRight = p;
                    }
                }
                for(Point p : pList) {
                    if(p != bottomRight && p != topLeft) {
                        if(p.x > topRight.x)
                            topRight = p;
                        if(p.y > bottomLeft.y)
                            bottomLeft = p;
                    }
                }


                Imgproc.line(animLayer, topLeft, topRight, COLOR_YELLOW);
                Imgproc.line(animLayer, topRight, bottomRight, COLOR_YELLOW);
                Imgproc.line(animLayer, bottomRight, bottomLeft, COLOR_YELLOW);
                Imgproc.line(animLayer, bottomLeft, topLeft, COLOR_YELLOW);

                Imgproc.circle(animLayer, topLeft, 15, COLOR_GREEN, 2);
                Imgproc.circle(animLayer, topRight, 15, COLOR_YELLOW, 2);
                Imgproc.circle(animLayer, bottomLeft, 15, COLOR_BLUE, 2);
                Imgproc.circle(animLayer, bottomRight, 15, COLOR_ORANGE, 2);
            }
        }
        return animLayer;
    }

    public double pointDistance(Point p){
        return Math.sqrt(Math.pow(p.x, 2) + Math.pow(p.y, 2));
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Log.i(TAG, "Camera loader: " + loader);
        if(loader < 100)
            return inputFrame.gray();
        else
            return processInputFrame(inputFrame.rgba());
    }
}
