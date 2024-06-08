package com.example.imagepro.ModelUtilites;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import com.example.imagepro.R;
import com.example.imagepro.ml.AgeDetectionModel50epochsOpt;
import com.example.imagepro.ml.EmotionDetectionModel100epochsNoOpt;
import com.example.imagepro.ml.GenderDetectionModel50epochsNoOpt;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ModelUtilities {

    private static final String[] class_labels = {"Angry", "Disgust", "Fear", "Happy", "Neutral", "Sad", "Surprise"};

    // edit on this because the changed model is age
    public static void detectAge(Context context , Bitmap bitmap, Rect rect , Mat Frame) {
        try {
            // to use the old one
            AgeDetectionModel50epochsOpt model = AgeDetectionModel50epochsOpt.newInstance(context);
            // Creates inputs for reference.
            TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
            tensorImage.load(bitmap);
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 200, 200, 3}, DataType.FLOAT32);
            inputFeature0.loadBuffer(tensorImage.getBuffer());

            // Runs model inference and gets result.
            AgeDetectionModel50epochsOpt.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
            float[] arr = outputFeature0.getFloatArray();
            int age = Math.round(arr[0]);
            Imgproc.putText(Frame, "Age = " + age, new Point(rect.x, rect.y + rect.height + 80), 0, 1, new Scalar(0, 255, 0), 2);
            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
    }

    public static void detectGender(Context context , Bitmap bitmap, Rect rect , Mat Frame) {

        try {
            GenderDetectionModel50epochsNoOpt model = GenderDetectionModel50epochsNoOpt.newInstance(context);
            // Creates inputs for reference.
            TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
            tensorImage.load(bitmap);
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 200, 200, 3}, DataType.FLOAT32);
            inputFeature0.loadBuffer(tensorImage.getBuffer());
            // Runs model inference and gets result.
            GenderDetectionModel50epochsNoOpt.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
            float[] arr = outputFeature0.getFloatArray();
            String Gender;
            if (arr[0] >= 0.5) {
                Gender = "Female";
            } else {
                Gender = "Male";
            }
            Imgproc.putText(Frame, Gender, new Point(rect.x, rect.y + rect.height + 50), 0, 2, new Scalar(0, 255, 0), 2);
            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
    }

    public static void detectEmotion(Context context, Bitmap bitmap, Rect rect, Mat Frame) {
        try {
           EmotionDetectionModel100epochsNoOpt emotion_model = EmotionDetectionModel100epochsNoOpt.newInstance(context);
            TensorImage emotionTensorImage = new TensorImage(DataType.FLOAT32);
            emotionTensorImage.load(bitmap);
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 48, 48, 1}, DataType.FLOAT32);
            Log.i("ab_do", String.valueOf(inputFeature0.getFlatSize()));
            inputFeature0.loadBuffer(getByteBuffer(emotionTensorImage.getBitmap()));
            // Runs model inference and gets result.
            EmotionDetectionModel100epochsNoOpt.Outputs outputs = emotion_model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
            float[] arr = outputFeature0.getFloatArray();
            for (int i = 0; i < arr.length; i++) {
                Log.i("ab_do", "emotion " + i + arr[i]);
            }
            int index = getMaxElement(arr);
            String emotion = class_labels[index];
            Imgproc.putText(Frame, emotion, new Point(rect.x, rect.y - 10), 0, 2, new Scalar(0, 255, 0), 2);
            // Releases model resources if no longer used.
            emotion_model.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static ByteBuffer getByteBuffer(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        ByteBuffer mImgData = ByteBuffer
                .allocateDirect(4 * width * height);
        mImgData.order(ByteOrder.nativeOrder());
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int pixel : pixels) {
            mImgData.putFloat((float) (Color.red(pixel) / 255.0f));
        }
        return mImgData;
    }

    private static int getMaxElement(float[] arr) {
        int max_index = -1;
        float max_val = Integer.MIN_VALUE;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] >= max_val) {
                max_index = i;
                max_val = arr[i];
            }
        }
        return max_index;
    }

    public static CascadeClassifier LoadCascadeClassifierModel(Context context) {
        CascadeClassifier cascadeClassifier = null ;
        try {
            InputStream is = context.getResources().openRawResource(R.raw.haarcascade_frontalface_default);
            File cas = context.getDir("cascade", MODE_PRIVATE);
            File cas_file = new File(cas, "haarcascade_frontalface_default.xml");
            FileOutputStream outputStream = new FileOutputStream(cas_file);
            byte[] buffer = new byte[4096];
            int byteRead;
            while ((byteRead = is.read(buffer)) != -1) {
                outputStream.write(buffer, 0, byteRead);
            }
            is.close();
            outputStream.close();
            try {
                cascadeClassifier = new CascadeClassifier(cas_file.getAbsolutePath());
            }
            catch (Exception e) {
                return null ;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null ;
        }
        return cascadeClassifier ;
    }

}
