package com.example.qr_scanner_mlkit;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Size;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ListenableFuture cameraProvider;
    private ExecutorService cameraExecutor;
    private PreviewView previewView;
    private ImageAnalyser analyser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        this.getWindow().setFlags(1024, 1024);

        cameraExecutor = Executors.newSingleThreadExecutor();
        cameraProvider = ProcessCameraProvider.getInstance(this);

        analyser = new ImageAnalyser(getSupportFragmentManager());


        //backgound process to check permission
        cameraProvider.addListener(new Runnable() {
            @Override
            public void run() {

                try {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.CAMERA) != (PackageManager.PERMISSION_GRANTED)) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.CAMERA}, 101);
                    } else {
                        ProcessCameraProvider processCameraProvider = (ProcessCameraProvider) cameraProvider.get();
                        bindPreview(processCameraProvider);
                    }
                } catch (ExecutionException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        }, ContextCompat.getMainExecutor(this));

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 101 && grantResults.length > 0) {
            ProcessCameraProvider processCameraProvider = null;
            try {
                processCameraProvider = (ProcessCameraProvider) cameraProvider.get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            bindPreview(processCameraProvider);
        }
    }

    private void bindPreview(ProcessCameraProvider processCameraProvider) {

        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        ImageCapture imageCapture = new ImageCapture.Builder().build();
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, analyser);
        processCameraProvider.unbindAll();
        processCameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);

    }
}