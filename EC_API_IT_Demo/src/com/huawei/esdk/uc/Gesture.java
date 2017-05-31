package com.huawei.esdk.uc;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.Core;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yaoyuan on 16/10/15.
 */
public class Gesture {
    private static final String tag= "Gesture";
    private int gestureNo = 0 ;
    private int gestureType = 0;


    private int recognize(Bitmap  bitmap){//type: 1-five, 0-none
        boolean five = false;

        Mat frame = new Mat();
        Utils.bitmapToMat(bitmap,frame);

        Mat blurredImage = new Mat();
        Mat hsvImage = new Mat();
        Mat mask = new Mat();
        Mat morphOutput = new Mat();

        // remove some noise
        Imgproc.blur(frame, blurredImage, new Size(7, 7));

        // convert the frame to HSV
        Imgproc.cvtColor(blurredImage, hsvImage, Imgproc.COLOR_BGR2HSV);

        Scalar minValues = new Scalar(0,30,30);
        Scalar maxValues = new Scalar(180,170,256);


// threshold HSV image to select tennis balls
        Core.inRange(hsvImage, minValues, maxValues, mask);

        Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(24, 24));
        Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(12, 12));

        Imgproc.erode(mask, morphOutput, erodeElement);
        Imgproc.erode(mask, morphOutput, erodeElement);

        Imgproc.dilate(mask, morphOutput, dilateElement);
        Imgproc.dilate(mask, morphOutput, dilateElement);

        // init
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

// find contours
        Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

// if any contour exist...
        if (hierarchy.size().height > 0 && hierarchy.size().width > 0)
        {
            // for each contour, display it in blue
//            for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0])
//            {
//                Imgproc.drawContours(frame, contours, idx, new Scalar(250, 0, 0));
//            }
            Log.e(tag,"Now a new frame.");
            for (int i=0;i<(int)contours.size();i++){
//                Log.e(tag,"this countersize is :");
////                + Imgproc.contourArea(contours.get(i))
                if (Imgproc.contourArea(contours.get(i))>500000){
                    five = true;
                    Log.e(tag,"this countersize is :"+ Imgproc.contourArea(contours.get(i)));
                }
            }
        }
        if(five){
            return 1;
        }
        else{
            return 0;
        }
    }

    private boolean gestureDuration(Bitmap bitmap){
        if (recognize(bitmap)==1){
            gestureNo++;
        }else{
            gestureNo = 0;
        }
        if (gestureNo == 1){
            gestureNo = 0;
            return true;
        }else{
            return false;
        }
    }

    public  boolean gestureEvent(Bitmap  bitmap){
        return gestureDuration(bitmap);
    }
}
