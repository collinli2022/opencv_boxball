package com.example.opencv_boxball.helper_modules;

import android.view.MenuItem;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class ball_detection {
    private static final String  TAG = "Helper_module.Ball_dectection";

    private boolean show_mask = false;
    public void setShow_mask(boolean value) {this.show_mask=value;}

    private int change_method = 0;
    public void setChange_method(int value) {this.change_method=value;}

    public Mat detectCircle(Mat mRGBAT, int minRadius) {
        Mat mThresholded = new Mat();
        Mat orig = mRGBAT.clone();

//        Imgproc.cvtColor(mRgba, mRgba, Imgproc.COLOR_RGB2HSV_FULL);

        Scalar Lower = new Scalar(29, 86, 6);
        Scalar greenUpper = new Scalar(64, 255, 255);

        Scalar perfect = new Scalar(0, 255, 0); // Prefect color (Green)
        Scalar range = new Scalar(100,100,100); // Range around perfect color

        // Convert the input to HSV color space
        Imgproc.cvtColor(mRGBAT,mRGBAT,Imgproc.COLOR_RGB2HSV_FULL);

        // Blur the imgae
        Imgproc.GaussianBlur(mRGBAT,mRGBAT,new Size(5,5),0);

        // Run a inRange mask using the color and range
//        Scalar lower = new Scalar(perfect.val[0] - (range.val[0]/2), perfect.val[1] - (range.val[1]/2),perfect.val[2] - (range.val[2]/2));
//        Scalar upper = new Scalar(perfect.val[0] + (range.val[0]/2), perfect.val[1] + (range.val[1]/2),perfect.val[2] + (range.val[2]/2));
        Core.inRange(mRGBAT,new Scalar(29, 86, 6), new Scalar(64, 255, 255),mThresholded);
        Mat circles = new Mat();
        Imgproc.GaussianBlur(mThresholded, mThresholded, new Size(9,9),0,0);
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchey = new Mat();
        Imgproc.findContours(mThresholded, contours, hierarchey, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // Imgproc.drawContours(orig, contours, -1, color, 2, Imgproc.LINE_8, hierarchey, 2, new Point());

        if (change_method==0) {
            Imgproc.HoughCircles(mThresholded, circles, Imgproc.CV_HOUGH_GRADIENT, 2, mThresholded.height()/4, 500, 50, 0, 0);

            int rows = circles.rows();
            int elemSize = (int)circles.elemSize(); // Returns 12 (3 * 4bytes in a float)
            float[] data2 = new float[rows * elemSize/4];
            if (data2.length>0){
                circles.get(0, 0, data2); // Points to the first element and reads the whole thing
                // into data2
                for(int i=0; i<data2.length; i=i+3) {
                    Point center= new Point(data2[i], data2[i+1]);
                    if(show_mask) {
                        Imgproc.circle(mThresholded, center, (int) Math.round(data2[2]), new Scalar(0,255,0), 5, 8, 0 );
                    } else {
                        Imgproc.circle(orig, center, (int) Math.round(data2[2]), new Scalar(0,255,0), 5, 8, 0 );
                    }

                }
            }

            if(show_mask) {
                return mThresholded;
            }
            return orig;
        } else if(change_method==1) {
            MatOfPoint2f[] contoursPoly  = new MatOfPoint2f[contours.size()];
            Point[] centers = new Point[contours.size()];
            float[][] radius = new float[contours.size()][1];
            for (int i = 0; i < contours.size(); i++) {
                contoursPoly[i] = new MatOfPoint2f();
                Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), contoursPoly[i], 3, true);
                centers[i] = new Point();
                Imgproc.minEnclosingCircle(contoursPoly[i], centers[i], radius[i]);
            }

            List<MatOfPoint> contoursPolyList = null;
            if(show_mask) {
                contoursPolyList = new ArrayList<>(contoursPoly.length);
                for (MatOfPoint2f poly : contoursPoly) {
                    contoursPolyList.add(new MatOfPoint(poly.toArray()));
                }
            }

            for (int i = 0; i < contours.size(); i++) {
                Scalar color = new Scalar(255, 0, 0);
                if(show_mask) { Imgproc.drawContours(mThresholded, contoursPolyList, i, color); }
                if(radius[i][0] > minRadius) {
                    Imgproc.circle(orig, centers[i], (int) radius[i][0], color, 5);
                }
            }

            if (show_mask) { return mThresholded; }

            return orig;

        } else if(change_method == 2) {
            Point[] centers = new Point[contours.size()];

            double maxArea = 1000;
            float[] radius = new float[1];
            Point center = new Point();
            for (int i = 0; i < contours.size(); i++) {
                MatOfPoint c = contours.get(i);
                if (Imgproc.contourArea(c) > maxArea) {
                    MatOfPoint2f c2f = new MatOfPoint2f(c.toArray());
                    Imgproc.minEnclosingCircle(c2f, center, radius);
                }
            }
            if(show_mask) {
                Imgproc.circle(mThresholded, center, (int)radius[0], new Scalar(255, 0, 0), 5);
                return mThresholded;
            }
            Imgproc.circle(orig, center, (int)radius[0], new Scalar(255, 0, 0), 5);
            return orig;
        }
        return orig;
    }
}
