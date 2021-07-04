package com.example.opencv_boxball;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.opencv_boxball.helper_modules.ball_detection;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link tab1#newInstance} factory method to
 * create an instance of this fragment.
 */
public class tab1 extends Fragment implements CvCameraViewListener2 {
    private static final String TAG = "tab1";

    private ball_detection ballDetector = new ball_detection();

    private int minRadius = 50;

    private JavaCameraView javaCameraView;
    Mat mRGBA, mRGBAT;
    int phone_orientation = 0;

    private Switch mask_switch;
    private Switch method_switch;

    private BaseLoaderCallback mLoaderCallback;


    public tab1() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment tab1.
     */
    // TODO: Rename and change types and number of parameters
    public static tab1 newInstance(String param1, String param2) {
        tab1 fragment = new tab1();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_tab1, container, false);

        String [] values = {"Hough Circles","minEnclosingCircle1", "minEnclosingCircle2"};
        Spinner spinner = (Spinner) v.findViewById(R.id.spinner1);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                ballDetector.setChange_method(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                ballDetector.setChange_method(0);
            }
        });

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        javaCameraView = (JavaCameraView) view.findViewById(R.id.java_camera_view);
        Log.i(TAG, String.valueOf(javaCameraView));
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);

        mask_switch = view.findViewById(R.id.switch1);
        mask_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ballDetector.setShow_mask(isChecked);
            }
        });

        method_switch = view.findViewById(R.id.switch2);
        method_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            }
        });
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.i(TAG, "orientation changed to landscape");
            phone_orientation = 90;
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            Log.i(TAG, "orientation changed to portrait");
            phone_orientation = 90;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (javaCameraView != null) {
            javaCameraView.disableView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mLoaderCallback = new BaseLoaderCallback(getActivity().getApplicationContext()) {
            @Override
            public void onManagerConnected(int status) {
                if (status == BaseLoaderCallback.SUCCESS) {
                    Log.i(TAG, "OpenCV loaded successfully");
                    javaCameraView.enableView();
                } else {
                    super.onManagerConnected(status);
                }
            }
        };
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
//            mLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, getActivity().getApplicationContext(), mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, getActivity().getApplicationContext(), mLoaderCallback);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (javaCameraView != null) {
            javaCameraView.disableView();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.d(TAG, String.format("width: %d, height: %d", width, height));
        mRGBA = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRGBA.release();
    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    //public native int imgProcess(long inputFrame, long imageGray, int sHmin, int sSmin, int sVmin, int sHmax, int sSmax, int sVmax);
    public native int imgProcess(long inputFrame, long imageGray);

    @Override
    public Mat onCameraFrame(CvCameraViewFrame frame) {
        mRGBA = frame.rgba();
        mRGBAT = mRGBA.t();
        Core.flip(mRGBA.t(), mRGBAT, 1);
        Imgproc.resize(mRGBAT, mRGBAT, mRGBA.size());

        return ballDetector.detectCircle(mRGBAT, minRadius);
    }
}