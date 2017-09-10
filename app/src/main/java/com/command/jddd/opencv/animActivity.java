package com.command.jddd.opencv;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.ImageView;

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

    JavaCameraView javaCameraView;
    ImageView iv;

    // Holds the best Mat to make comparisons
    Mat camLayer;
    Mat animLayer;

    Scalar COLOR_RED = new Scalar(255,0,0,255);
    Scalar COLOR_GREEN = new Scalar(81, 190, 0);
    Scalar COLOR_ORANGE = new Scalar(255,140,0);
    Scalar COLOR_BLUE = new Scalar(123,104,238);
    Scalar COLOR_YELLOW = new Scalar(255, 255, 0);

    List<MatOfPoint> contours;
    List<MatOfPoint> mContours;

    Point[] imgPtsArray;
    List<Point> imgPtsList;

    AssetManager assetManager;
    Bitmap bmp = null;

    int count = 0;
    ArrayList<Mat> animation;
    Iterator<Mat> animItr;
    Mat t;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anim);
        javaCameraView = (JavaCameraView)findViewById(R.id.camera_view);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);
        // iv = (ImageView) findViewById(R.id.overlayView);
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "  OpenCVLoader.initDebug(), not working.");
        } else {
            Log.d(TAG, "  OpenCVLoader.initDebug(), working.");
        }
        /*
        if (flag) {
            javaCameraView.setCameraIndex(0);
            javaCameraView.setCvCameraViewListener(this);
            javaCameraView.enableView();
            try {
                initialize();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        */
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
        Log.i(TAG, "Initializing ...");
        // animLayer = img2Mat("square.png");
        Mat tmpLayer = img2Mat("square.png");
        animLayer = new Mat(new Size(240, 180), 24);
        Imgproc.resize( tmpLayer, animLayer, new Size(240, 180) );
        imgPtsList = new ArrayList<>();
        // Log.i(TAG, "inMat checkVector: " + animLayer.checkVector(animLayer.channels(), animLayer.depth()));

        Point p1 = new Point(0,0);
        Point p2 = new Point(width,0);
        Point p3 = new Point(width, height);
        Point p4 = new Point(0,height);
        imgPtsList.add(p1);
        imgPtsList.add(p2);
        imgPtsList.add(p3);
        imgPtsList.add(p4);

        Imgproc.resize(tmpLayer, animLayer, new Size(240, 180));
        // Mat proyection = transformation(tr);

        String[] names = new String[]{"square.png", "arrow.png", "circle.png", "star.png", "triangle.png", "hand.png", "circle.png", "arrow.png"};
        animation = new ArrayList<>();
        for(int j = 0; j < names.length; j++) {
            Mat tmp = new Mat();
            Imgproc.resize(img2Mat(names[j]), tmp, new Size(240, 180));
            animation.add(tmp);
        }

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
        Bitmap bitmap = BitmapFactory.decodeStream(istr);
        Utils.bitmapToMat(bitmap, mPt);
        return mPt;
    }

    public void loadAnimation() throws IOException {
        /*

        */
        /*
        ImageView iv = (ImageView) findViewById(R.id.imageView);
        Bitmap bmp = null;
        bmp = Bitmap.createBitmap(imgVector[0].cols(), imgVector[0].rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imgVector[0], bmp);
        iv.setImageBitmap(bmp);
        */
    }

    public Mat processInputFrame(Mat inputFrame) {
        // Log.i(TAG, "Input Fame " + inputFrame.empty());

        Mat hierarchy = new Mat();

        camLayer = new Mat();
        // animLayer = new Mat();

        contours = new ArrayList<>();
        mContours = new ArrayList<>();

        Imgproc.cvtColor(inputFrame, camLayer, Imgproc.COLOR_RGB2GRAY);
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

                // Rect rect = Imgproc.boundingRect(points);
                // Imgproc.drawContours(inputFrame, mContours, -1, COLOR_RED, 3);
                // Imgproc.rectangle(inputFrame, rect.tl(), rect.br(), COLOR_GREEN,1, 8,0);
                // Point[] pArray = points.toArray();
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

                Imgproc.line(inputFrame, topLeft, topRight, COLOR_YELLOW);
                Imgproc.line(inputFrame, topRight, bottomRight, COLOR_YELLOW);
                Imgproc.line(inputFrame, bottomRight, bottomLeft, COLOR_YELLOW);
                Imgproc.line(inputFrame, bottomLeft, topLeft, COLOR_YELLOW);

                Imgproc.circle(inputFrame, topLeft, 15, COLOR_GREEN, 2);
                Imgproc.circle(inputFrame, topRight, 15, COLOR_YELLOW, 2);
                Imgproc.circle(inputFrame, bottomLeft, 15, COLOR_BLUE, 2);
                Imgproc.circle(inputFrame, bottomRight, 15, COLOR_ORANGE, 2);

                ArrayList<Point> tr = new ArrayList<>();
                tr.add(topLeft);
                tr.add(topRight);
                tr.add(bottomRight);
                tr.add(bottomLeft);

                // inMat2.copyTo(outMat());
                // FIXME: Check why the animation effect is not taking place

                if(animItr.hasNext()) {
                    if(++count % 2 == 0) { //Slowdown frame rate
                        t = animItr.next();
                        Log.i(TAG, "count ... " + count);
                    }
                    t.copyTo(inputFrame.submat(new Rect(0, 0, 240, 180)));
                } else {
                    animItr = animation.iterator();
                }

                // Log.i(TAG, "count ... " + count++);
                // Opencv stops working
                // iv.setImageBitmap(bmp);
            }
        }
        return inputFrame;
    }

    public double pointDistance(Point p){
        return Math.sqrt(Math.pow(p.x, 2) + Math.pow(p.y, 2));
    }

    public Mat transformation(ArrayList<Point> pa) {
        Mat warped = new Mat(camLayer.cols(), camLayer.rows(), camLayer.type());
        Mat endM = Converters.vector_Point2f_to_Mat(pa);
        Mat startM = Converters.vector_Point2f_to_Mat(imgPtsList);
        Mat trans = Imgproc.getPerspectiveTransform(startM, endM);
        Size s = new Size(camLayer.cols(), camLayer.rows());
        Imgproc.warpPerspective(animLayer, warped, trans, s);
        return warped;
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

    // Not used
    public static List<MatOfPoint> getSquareContours(List<MatOfPoint> contours) {

        List<MatOfPoint> squares = new ArrayList<MatOfPoint>();
        for (MatOfPoint c : contours) {
            if (isContourSquare(c)) {
                squares.add(c);
            }
        }
        return squares;
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        return processInputFrame(inputFrame.rgba());
        // return inputFrame.rgba();
    }
}
