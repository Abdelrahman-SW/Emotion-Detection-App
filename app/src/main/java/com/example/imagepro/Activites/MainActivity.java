package com.example.imagepro.Activites;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.imagepro.R;

public class MainActivity extends AppCompatActivity {
    Button take , upload , realtime , ipCamera;
    ActivityResultLauncher<Intent> startActivityIntent ;
    ActivityResultLauncher<String> activityResultRegistry ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ContextCompat.checkSelfPermission(getApplicationContext(), "android.permission.CAMERA") != 0) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.CAMERA"}, 100);
        }
        initRegisters();
        init();
        setListeners();
    }

    private void initRegisters() {
        startActivityIntent = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        // Add same code that you want to add in onActivityResult method
                        if (result.getData()!=null && result.getData().getExtras()!=null) {
                            Intent intent = new Intent(getBaseContext(), resultActivity.class);
                            intent.setAction("take");
                            intent.putExtra("bundle", result.getData().getExtras());
                            startActivity(intent);
                        }
                    }
                });

        activityResultRegistry = registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri result) {
                if (result!=null) {
                    Intent intent = new Intent(getBaseContext(), resultActivity.class);
                    intent.setAction("upload");
                    intent.setData(result);
                    startActivity(intent);
                }
            }
        });
    }

    private void setListeners() {
        ipCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getBaseContext() , IpCamera.class));
            }
        });
        realtime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getBaseContext() , CameraActivity.class));
            }
        });
        take.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityIntent.launch(new Intent("android.media.action.IMAGE_CAPTURE"));
                //MainActivity.this.startActivityForResult(new Intent("android.media.action.IMAGE_CAPTURE"), 200);

            }
        });
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               activityResultRegistry.launch("image/*");
            }
        });
    }

    private void init() {
        take = findViewById(R.id.button2);
        upload = findViewById(R.id.button3);
        realtime = findViewById(R.id.button);
        ipCamera = findViewById(R.id.ipCamera);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 100)
        {
            if (data!=null) {
                Intent intent = new Intent(getBaseContext(), resultActivity.class);
                intent.setAction("upload");
                intent.setData(data.getData());
                startActivity(intent);
            }
        }
        else if (requestCode == 200) {
            if (data!=null && data.getExtras()!=null) {
                Intent intent = new Intent(getBaseContext(), resultActivity.class);
                intent.setAction("take");
                intent.putExtra("bundle", data.getExtras());
                startActivity(intent);
            }
        }
    }
}