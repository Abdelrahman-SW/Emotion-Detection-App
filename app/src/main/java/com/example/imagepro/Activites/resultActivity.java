package com.example.imagepro.Activites;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.imagepro.ModelUtilites.ModelUtilities;
import com.example.imagepro.R;
import com.example.imagepro.databinding.ActivityResultBinding;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.IOException;

public class resultActivity extends AppCompatActivity {
    private Bitmap bitmap ;
    Mat image_roi;
    Mat image_roi_gray;
    MatOfRect Faces;
    int height;
    int faceSize;
    Rect rectCrop;
    Bitmap bitmap_gray;
    ActivityResultBinding binding ;
    private CascadeClassifier cascadeClassifier;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
        getFaces(bitmap);
    }

    private void init() {
        cascadeClassifier = ModelUtilities.LoadCascadeClassifierModel(this);
        if (cascadeClassifier == null) recreate();
        if (getIntent().getAction().equals("upload")) {
            //binding.imageView.setImageURI(getIntent().getData());
            Uri uri = getIntent().getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if (getIntent().getAction().equals("take")) {
            Bundle bundle ;
            bundle = getIntent().getBundleExtra("bundle");
            bitmap = (Bitmap) bundle.get("data");
            //binding.imageView.setImageBitmap(bitmap);
        }
        binding.back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }


    private void getFaces(Bitmap bitmap) {
        Mat src_image = new Mat();
        Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, src_image);
        Core.copyMakeBorder(src_image, src_image , 150, 150, 150, 150, 16 , new Scalar(255 , 255 , 255));
        height = src_image.height();
        faceSize = (int) (height * 0.1);
        Faces = new MatOfRect();
        if (cascadeClassifier != null) {
            cascadeClassifier.detectMultiScale(src_image, Faces, 1.3, 5, 2, new Size(faceSize, faceSize), new Size());
        }
        if (Faces.toArray().length == 0) {
            binding.imageView.setImageResource(R.drawable.magnifying_glass);
            binding.noFace.setVisibility(View.VISIBLE);
            return;
        }
        for (Rect rect : Faces.toArray()) {
            Imgproc.rectangle(src_image, rect.tl(), rect.br(), new Scalar(255, 0, 0, 255), 2);
            //binding.imageView.setImageBitmap(src);
            rectCrop = new Rect(rect.x, rect.y, rect.width, rect.height);
            image_roi = new Mat(src_image, rectCrop);
            bitmap = Bitmap.createBitmap(image_roi.cols(), image_roi.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(image_roi, bitmap);
            bitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true);
            image_roi_gray = new Mat(src_image, rectCrop);
            Imgproc.resize(image_roi_gray , image_roi_gray , new Size(48,48) , 0 , 0 , Imgproc.INTER_AREA);
            //Core.divide(1.0 / 255, image_roi_gray, image_roi_gray);
            bitmap_gray = Bitmap.createBitmap(image_roi_gray.cols(), image_roi_gray.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(image_roi_gray, bitmap_gray);
            bitmap_gray = Bitmap.createScaledBitmap(bitmap, 48, 48, true);
            ModelUtilities.detectEmotion(this , bitmap_gray , rect , src_image);
            ModelUtilities.detectAge(this , bitmap , rect , src_image);
            ModelUtilities.detectGender(this , bitmap  , rect , src_image);
            bitmap_gray = null;
            Faces = null ;
            if (image_roi_gray!=null)
            image_roi_gray.release();
            if (image_roi!=null)
            image_roi.release();
            rectCrop = null ;
        }
        Bitmap src = Bitmap.createBitmap(src_image.cols(), src_image.rows(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(src_image, src);
        binding.imageView.setImageBitmap(src);
    }


}