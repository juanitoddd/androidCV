package com.command.jddd.opencv;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class imgActivity extends AppCompatActivity{

    private static final String TAG = "OCVSample";
    static boolean flag;
    Mat marker;
    Mat inMat1;
    Mat inMat2;
    JavaCameraView javaCameraView;

    AssetManager assetManager;

    ArrayList<Point> pfinal;
    ArrayList<Point> initial;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");

                    try {
                        helloworld();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public Mat img2Mat(String fileName) throws IOException {
        marker = new Mat();
        assetManager = getAssets();
        InputStream istr = null;
        try {
            istr = assetManager.open(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(istr != null) {
            Log.i(TAG, "Found ...");
        } else {
            Log.i(TAG, "Not found ...");
        }
        Bitmap bitmap = BitmapFactory.decodeStream(istr);
        Utils.bitmapToMat(bitmap, marker);
        return marker;
    }

    public void helloworld() throws IOException, CvException {

        assetManager = getAssets();
        ImageView iv = (ImageView) findViewById(R.id.imageView);

        inMat1 = img2Mat("trans.png");
        List<Mat> bgra = new ArrayList<>(4);
        Core.split(inMat1, bgra);

        Bitmap bmp = null;
        try {
            bmp = Bitmap.createBitmap(inMat1.cols(), inMat1.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(inMat1, bmp);
            iv.setImageBitmap(bmp);
        }
        catch (CvException e){Log.d("Exception",e.getMessage());}
    }

    /*
    static {
        if(!(OpenCVLoader.initDebug())) {
            Log.d("ERROR", "Unable to load OpenCV");
        } else {
            Log.d("SUCCESS", "OpenCV loaded");
            flag = true;
        }
    }
    */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_img);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
}
