package com.example.screenrecorder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.VideoView;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE = 1000;
    private static final int REQUEST_PERMISSION = 1001;

    MediaRecorder mediaRecorder;
    private static final SparseIntArray ORIENTAITON = new SparseIntArray();
    private MediaProjectionManager mediaProjectionManager;
    MediaProjection mediaProjection;
    VirtualDisplay virtualDisplay;
    MediaProjectionCallback mediaProjectionCallback;
    private int mScreenDensity;
    private RelativeLayout rootlayout;
    private static final int DISPLAY_WIDTH = 720;
    private static final int DISPLAY_HEIGHT = 1280;


    static {
        ORIENTAITON.append(Surface.ROTATION_0,90);
        ORIENTAITON.append(Surface.ROTATION_90,0);
        ORIENTAITON.append(Surface.ROTATION_180,270);
        ORIENTAITON.append(Surface.ROTATION_270,180);
    }

    private ToggleButton toggleButton;
    private VideoView videoView;
    String videourl = "";

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        mediaRecorder = new MediaRecorder();
        mediaProjectionManager = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        toggleButton = findViewById(R.id.tooglebtn);
        videoView = findViewById(R.id.videoView);
        rootlayout = findViewById(R.id.rootlayout);
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                +ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED){
                    if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    ||
                            ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,Manifest.permission.RECORD_AUDIO)){
                        toggleButton.setChecked(false);
                        Snackbar.make(rootlayout,"Permissions", BaseTransientBottomBar.LENGTH_INDEFINITE)
                                .setAction("ENABLE", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO},REQUEST_PERMISSION
                                        );
                                    }
                                }).show();


                    }
                    else{
                        ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                        Manifest.permission.RECORD_AUDIO},
                                REQUEST_PERMISSION);
                    }
                }
                else{
                    toggleScreenshare(v);
                }

            }

        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void toggleScreenshare(View v) {
        if(((ToggleButton)v).isChecked()){
            initRecorder();
            recordScreen();
        }
        else {
            mediaRecorder.stop();
            mediaRecorder.reset();
            stoprecordScreen();

            videoView.setVisibility(View.VISIBLE);
            videoView.setVideoURI(Uri.parse(videourl));
            videoView.start();

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void recordScreen() {
        if(mediaProjection == null){
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(),REQUEST_CODE);
            return;
        }
        virtualDisplay= createvirtualdisplay();
        mediaRecorder.start();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private VirtualDisplay createvirtualdisplay() {
    return mediaProjection.createVirtualDisplay("MainActivity",DISPLAY_WIDTH,DISPLAY_HEIGHT,mScreenDensity
    , DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder.getSurface(),null,null);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initRecorder() {
        try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

            videourl = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    + "/EDMTRecord_" + new SimpleDateFormat("dd-MM-yyyy-hh_mm_ss")
                    .format(new Date()) +
                    ".mp4";
            mediaRecorder.setOutputFile(videourl);
            mediaRecorder.setVideoSize(DISPLAY_WIDTH,DISPLAY_HEIGHT);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setVideoEncodingBitRate(512*1000);
            mediaRecorder.setVideoFrameRate(30);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int orientation = ORIENTAITON.get(rotation+90);
            mediaRecorder.setOrientationHint(orientation);
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode != REQUEST_CODE){
            Toast.makeText(this, "Unknown Error", Toast.LENGTH_SHORT).show();
        return;
        }
        if(resultCode != RESULT_OK){
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            toggleButton.setChecked(false);
            return;
        }
        mediaProjectionCallback = new MediaProjectionCallback();
        mediaProjection =mediaProjectionManager.getMediaProjection(resultCode,data);
        mediaProjection.registerCallback(mediaProjectionCallback,null);
        virtualDisplay = createvirtualdisplay();
        mediaRecorder.start();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            if(toggleButton.isChecked()){
                toggleButton.setChecked(false);
                mediaRecorder.stop();
                mediaRecorder.reset();
            }
            mediaProjection = null;
            stoprecordScreen();
            super.onStop();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void stoprecordScreen() {
    if(virtualDisplay == null)
        return;
    virtualDisplay.release();
    destroymediaprojection();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void destroymediaprojection() {
    if(mediaProjection != null){
        mediaProjection.unregisterCallback(mediaProjectionCallback);
        mediaProjection.stop();
        mediaProjection = null;

    }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case REQUEST_PERMISSION:
            {
                if((grantResults.length>0) && (grantResults[0]+grantResults[1]==PackageManager.PERMISSION_GRANTED)){
                    toggleScreenshare(toggleButton);
                }
                else {
                    toggleButton.setChecked(false);
                    Snackbar.make(rootlayout,"Permissions", BaseTransientBottomBar.LENGTH_INDEFINITE)
                            .setAction("ENABLE", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                                            Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO},REQUEST_PERMISSION
                                    );
                                }
                            }).show();
                }
                return;
            }
        }
    }
}