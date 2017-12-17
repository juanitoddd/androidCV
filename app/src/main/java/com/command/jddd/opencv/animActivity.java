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
public class animActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{

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
        setContentView(R.layout.activity_anim);
        javaCameraView = (JavaCameraView)findViewById(R.id.camera_view);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);
        javaCameraView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                // ... Respond to touch events
                Log.e(TAG, "  Touch event");
                //Starting a new Intent
                Intent contourScreen = new Intent(getApplicationContext(), imgActivity.class);

                //Sending data to another Activity
                // contourScreen.putExtra("name", inputName.getText().toString());
                // contourScreen.putExtra("email", inputEmail.getText().toString());

                startActivity(contourScreen);
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

        String formattedName;
        animation = new ArrayList<>();

        for(int j = 0; j < 122; j++) {

            formattedName = "gabriel" + String.format("%03d", j) + ".png";

            Mat tmp = new Mat();
            Imgproc.resize(img2Mat(formattedName), tmp, new Size(width, height));
            animation.add(tmp);
        }
        loader = 100;
        animItr = animation.iterator();
        t = animation.get(0);
    }

    public MatOfPoint img2MatOfPoint(String fileName) throws IOException {
        MatOfPoint mPt = new MatOfPoint();
        assetManager = getAssets();
        InputStream istr = null;
        try {
            istr = assetManager.open(fileName);
        } catch (IOException e) {
            Log.i(TAG, "Not found ...");
            e.printStackTrace();
        }
        Bitmap bitmap = BitmapFactory.decodeStream(istr);
        Utils.bitmapToMat(bitmap, mPt);
        return mPt;
    }

    public Mat img2Mat(String fileName) throws IOException {
        Mat mPt = new Mat();
        assetManager = getAssets();
        InputStream istr = null;
        try {
            istr = assetManager.open(fileName);
        } catch (IOException e) {
            Log.i(TAG, "Not found ...");
            e.printStackTrace();
        }
        // Bitmap bitmap = BitmapFactory.decodeStream(istr);
        Options opt = new Options();
        opt.inDither = false;   //important
        opt.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeStream(istr, null, opt);
        Utils.bitmapToMat(bitmap, mPt);
        return mPt;
    }

    public Mat processInputFrame(Mat inputFrame) {

        Mat hierarchy = new Mat();

        camLayer = new Mat();
        maskLayer = new Mat();
        animLayer = inputFrame.clone();

        contours = new ArrayList<>();
        mContours = new ArrayList<>();
        maskContours = new ArrayList<>();

        Imgproc.cvtColor(animLayer, camLayer, Imgproc.COLOR_RGB2GRAY);
        Imgproc.blur(camLayer, camLayer, new Size(13, 13));
        Imgproc.threshold(camLayer, camLayer, 100, 255, Imgproc.THRESH_BINARY);
        // Imgproc.Canny(findLayer, findLayer, 10.0, 30.0);

        Imgproc.findContours(camLayer, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        // Imgproc.drawContours(camLayer, contours, -1, COLOR_RED, 5);

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
                // Log.i(TAG, "Inside 4 Contours");
                Rect rect = Imgproc.boundingRect(points);
                // Imgproc.rectangle(animLayer, rect.tl(), rect.br(), COLOR_GREEN,1, 8,0);

                // Imgproc.drawContours(inputFrame, mContours, -1, COLOR_RED, 3);
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

                /*
                Imgproc.line(animLayer, topLeft, topRight, COLOR_YELLOW);
                Imgproc.line(animLayer, topRight, bottomRight, COLOR_YELLOW);
                Imgproc.line(animLayer, bottomRight, bottomLeft, COLOR_YELLOW);
                Imgproc.line(animLayer, bottomLeft, topLeft, COLOR_YELLOW);

                Imgproc.circle(animLayer, topLeft, 15, COLOR_GREEN, 2);
                Imgproc.circle(animLayer, topRight, 15, COLOR_YELLOW, 2);
                Imgproc.circle(animLayer, bottomLeft, 15, COLOR_BLUE, 2);
                Imgproc.circle(animLayer, bottomRight, 15, COLOR_ORANGE, 2);
                */
                // Taking reference the Rectangle around the Contours
                // Contour points
                ArrayList<Point> cPoints = new ArrayList<>();
                cPoints.add(new Point(topLeft.x - rect.x, topLeft.y - rect.y));
                cPoints.add(new Point(topRight.x - rect.x, topRight.y - rect.y));
                cPoints.add(new Point(bottomRight.x - rect.x, bottomRight.y - rect.y));
                cPoints.add(new Point(bottomLeft.x - rect.x, bottomLeft.y - rect.y));

                // Rectangle points
                ArrayList<Point> rPoints = new ArrayList<>();
                rPoints.add(new Point(0, 0));
                rPoints.add(new Point(rect.width, 0));
                rPoints.add(new Point(rect.width, rect.height));
                rPoints.add(new Point(0, rect.height));

                if(animItr.hasNext()) {

                    t = animItr.next();
                    // Copy to surrounding rectangle
                    Mat small = new Mat();
                    Imgproc.resize(t,small,new Size(rect.width, rect.height));//resize image

                    // Transformation Process
                    Mat warped = new Mat(rect.height, rect.width , CvType.CV_8UC4);
                    Mat startM = Converters.vector_Point2f_to_Mat(rPoints);
                    Mat endM = Converters.vector_Point2f_to_Mat(cPoints);
                    Mat trans = Imgproc.getPerspectiveTransform(startM, endM);

                    Size s = new Size(rect.width, rect.height);
                    Imgproc.warpPerspective(small, warped, trans, s);

                    // Region Of Interest
                    Mat roi = animLayer.submat(new Rect(rect.x, rect.y, rect.width, rect.height));

                    Mat tmp = new Mat(rect.height, rect.width, CvType.CV_8U);
                    Mat msk = Mat.zeros(rect.height, rect.width, CvType.CV_8U);
                    Imgproc.cvtColor(warped, tmp, Imgproc.COLOR_RGBA2GRAY);
                    Core.findNonZero(tmp, msk);

                    // Create Mask of 0 Depth and 1 Channel, better method ?
                    msk.convertTo(msk, CvType.CV_8U);
                    List<Mat> rgba = new ArrayList<>(3);
                    Core.split(tmp, rgba);
                    Mat mR = rgba.get(0);

                    warped.copyTo(roi, mR);
                } else {
                    animItr = animation.iterator();
                }
            }
        }
        return animLayer;
    }

    public double pointDistance(Point p){
        return Math.sqrt(Math.pow(p.x, 2) + Math.pow(p.y, 2));
    }

    // Not used
    public static boolean isContourSquare(MatOfPoint thisContour) {

        Rect ret = null;

        MatOfPoint2f thisContour2f = new MatOfPoint2f();
        MatOfPoint approxContour = new MatOfPoint();
        MatOfPoint2f approxContour2f = new MatOfPoint2f();
        thisContour.convertTo(thisContour2f, CvType.CV_32FC2);
        Imgproc.approxPolyDP(thisContour2f, approxContour2f, 2, true);

        approxContour2f.convertTo(approxContour, CvType.CV_32S);
        if (approxContour.size().height == 4) {
            ret = Imgproc.boundingRect(approxContour);
        }
        return (ret != null);
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        // Log.i(TAG, "Inside onCameraFrame");
        if(loader < 100)
            return inputFrame.gray();
        else
            return processInputFrame(inputFrame.rgba());
    }
}
