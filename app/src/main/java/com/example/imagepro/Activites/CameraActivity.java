package com.example.imagepro.Activites;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.imagepro.ModelUtilites.ModelUtilities;
import com.example.imagepro.R;
import com.example.imagepro.ml.EmotionDetectionModel100epochsNoOpt;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.BFMatcher;
import org.opencv.features2d.Feature2D;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.tensorflow.lite.support.image.TensorImage;

public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "CameraActivity";
    private CameraBridgeViewBase mOpenCvCameraView; //exists as is in opencv samples
    private int mCameraId = 0; //add this one
    int PERMISSIONS_REQUEST_CAMERA = 0;
    private Mat mRgba, mGray;
    private CascadeClassifier cascadeClassifier;
    private final BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface
                    .SUCCESS) {
                Log.i(TAG, "OpenCv Is loaded");
                mOpenCvCameraView.enableView();
            }
            super.onManagerConnected(status);
        }
    };
    Mat image_roi;
    Mat image_roi_gray;
    Mat mRbg;
    MatOfRect Faces;
    int height;
    int faceSize;
    Rect rectCrop;
    Bitmap bitmap;
    Bitmap bitmap_gray;
    TensorImage emotionTensorImage;
    EmotionDetectionModel100epochsNoOpt emotion_model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initWindow();
        // if camera permission is not given it will ask for it on device
        if (ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(CameraActivity.this, new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CAMERA);
        }

        setContentView(R.layout.activity_camera);
        init();
    }

    private void init() {
        mOpenCvCameraView = findViewById(R.id.frame_Surface);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        //mOpenCvCameraView.setScaleX(-1);
        mOpenCvCameraView.setCvCameraViewListener(this);
        ImageView switchCamera = findViewById(R.id.swap);
        ImageView close = findViewById(R.id.exit);
        switchCamera.setOnClickListener(v -> swapCamera());
        close.setOnClickListener(view -> finish());
        LoadModel();
    }

    private void initWindow() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
    }


    private void LoadModel() {
        cascadeClassifier = ModelUtilities.LoadCascadeClassifierModel(this);
        if (cascadeClassifier == null) recreate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            //if load success
            Log.d(TAG, "Opencv initialization is done");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            //if not loaded
            Log.d(TAG, "Opencv is not loaded. try again");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        }
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
            mOpenCvCameraView.setCvCameraViewListener((CameraBridgeViewBase.CvCameraViewListener2) null);
        }
        if (mRgba != null)
            mRgba.release();
        if (mGray != null)
            mGray.release();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.destroyDrawingCache();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
            mOpenCvCameraView.setCvCameraViewListener((CameraBridgeViewBase.CvCameraViewListener2) null);
        }
        if (mRgba != null)
            mRgba.release();
        if (mGray != null)
            mGray.release();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_32F);
    }

    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();
        Core.flip(mRgba, mRgba, 1);
        Thread thread = new Thread() {
            @Override
            public void run() {
                getFaces(mRgba);
            }
        };
        thread.start();
        return mRgba;
    }

    private void getFaces(Mat mRgba) {
        Core.flip(mRgba.t(), mRgba, 1);
        mRbg = new Mat();
        Imgproc.cvtColor(mRgba, mRbg, Imgproc.COLOR_RGBA2RGB);
        height = mRbg.height();
        faceSize = (int) (height * 0.1);
        Faces = new MatOfRect();
        if (cascadeClassifier != null) {
            cascadeClassifier.detectMultiScale(mRbg, Faces, 1.3, 5, 2, new Size(faceSize, faceSize), new Size());
        }
        for (Rect rect : Faces.toArray()) {
            Imgproc.rectangle(mRgba, rect.tl(), rect.br(), new Scalar(255, 0, 0, 255), 2);
            rectCrop = new Rect(rect.x, rect.y, rect.width, rect.height);
            image_roi = new Mat(mRgba, rectCrop);
            bitmap = Bitmap.createBitmap(image_roi.cols(), image_roi.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(image_roi, bitmap);
            bitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true);
            image_roi_gray = new Mat(mGray, rectCrop);
            Imgproc.resize(image_roi_gray , image_roi_gray , new Size(48,48) , 0 , 0 , Imgproc.INTER_AREA);
            //Core.divide(1.0 / 255.0, image_roi_gray, image_roi_gray);
            bitmap_gray = Bitmap.createBitmap(image_roi_gray.cols(), image_roi_gray.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(image_roi_gray, bitmap_gray);
            bitmap_gray = Bitmap.createScaledBitmap(bitmap, 48, 48, true);
            ModelUtilities.detectEmotion(this ,bitmap_gray, rect , mRgba);
            ModelUtilities.detectAge(this , bitmap, rect , mRgba);
            ModelUtilities.detectGender(this ,bitmap, rect , mRgba);
            bitmap = null;
            bitmap_gray = null;
            Faces = null ;
            mRbg.release();
            image_roi_gray.release();
            image_roi.release();
            rectCrop = null ;
        }
        Core.flip(mRgba.t(), mRgba, 0);
    }


    private void swapCamera() {
        mCameraId = mCameraId ^ 1; //bitwise not operation to flip 1 to 0 and vice versa
        mOpenCvCameraView.disableView();
        mOpenCvCameraView.setCameraIndex(mCameraId);
        mOpenCvCameraView.enableView();
    }

}