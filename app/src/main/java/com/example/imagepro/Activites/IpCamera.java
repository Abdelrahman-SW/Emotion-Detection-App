package com.example.imagepro.Activites;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.imagepro.R;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

public class IpCamera extends AppCompatActivity {
    Mat live = new Mat() ;
    VideoCapture videoCapture ;
    ImageView view ;
    Bitmap src ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        setContentView(R.layout.activity_ip_camera);
        view = findViewById(R.id.view);
        videoCapture = new VideoCapture();
        videoCapture.open(getString(R.string.ip_address));
        if (!videoCapture.isOpened()) {
            Log.i("ab_do" , "ERROR CONNECTING TO CAMERA");
            Toast.makeText(this, "ERROR CONNECTING TO CAMERA", Toast.LENGTH_LONG).show();
            //return;
        } else {
            Log.i("ab_do" , "video is captured!");
        }
        while (videoCapture.read(live)) {
            if (src!=null) src = null ;
            src = Bitmap.createBitmap(live.cols(), live.rows(), Bitmap.Config.RGB_565);
            Utils.matToBitmap(live, src);
            view.setImageBitmap(src);
        }
    }

    @Override
    protected void onDestroy() {
        videoCapture.release();
        src = null ;
        live.release();
        super.onDestroy();
    }
}