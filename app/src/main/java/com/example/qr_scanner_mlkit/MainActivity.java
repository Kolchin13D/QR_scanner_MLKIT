package com.example.qr_scanner_mlkit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.util.Size;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import org.jetbrains.annotations.NotNull;

import java.util.List;
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
        CameraSelector cameraSelector = new CameraSelector
                .Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
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

//    public class ImageAnalyser implements ImageAnalysis.Analyzer {
//        private FragmentManager fragmentManager;
//        private BottomDialog bottomDialog;
//
//        public ImageAnalyser(FragmentManager fragmentManager) {
//            this.fragmentManager = fragmentManager;
//            bottomDialog = new BottomDialog();
//        }
//
//        @Override
//        public void analyze(@NonNull ImageProxy image) {
//            ScanBarCode(image);
//        }
//
//
//        private void ScanBarCode(ImageProxy image) {
//            @SuppressLint("UnsafeOptInUsageError") Image image1 = image.getImage();
//            assert image1 != null;
//            InputImage inputImage = InputImage
//                    .fromMediaImage(image1,
//                            image.getImageInfo()
//                                    .getRotationDegrees());
//
//            BarcodeScannerOptions options =
//                    new BarcodeScannerOptions.Builder()
//                            .setBarcodeFormats(
//                                    Barcode.FORMAT_QR_CODE,
//                                    Barcode.FORMAT_AZTEC)
//                            .build();
//
//            BarcodeScanner scanner = BarcodeScanning.getClient(options);
//            Task<List<Barcode>> result = scanner.process(inputImage)
//                    .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
//                        @Override
//                        public void onSuccess(List<Barcode> barcodes) {
//                            readerBarcodeData(barcodes);
//                        }
//                    })
//                    .addOnFailureListener(new OnFailureListener() {
//                        @Override
//                        public void onFailure(@NonNull Exception e) {
//                            Toast.makeText(getApplicationContext(), "Failed to read code", Toast.LENGTH_SHORT).show();
//                        }
//                    })
//                    .addOnCompleteListener(new OnCompleteListener<List<Barcode>>() {
//                        @Override
//                        public void onComplete(@NonNull @NotNull Task<List<Barcode>> task) {
//                            image.close();
//                        }
//                    });
//        }
//
//        private void readerBarcodeData(List<Barcode> barcodes) {
//            for (Barcode barcode : barcodes) {
//                Rect bounds = barcode.getBoundingBox();
//                Point[] corners = barcode.getCornerPoints();
//
//                String rawValue = barcode.getRawValue();
//
//                int valueType = barcode.getValueType();
//                // See API reference for complete list of supported types
//                switch (valueType) {
//                    case Barcode.TYPE_WIFI:
//                        String ssid = barcode.getWifi().getSsid();
//                        String password = barcode.getWifi().getPassword();
//                        int type = barcode.getWifi().getEncryptionType();
//                        break;
//                    case Barcode.TYPE_URL:
//                        if (!bottomDialog.isAdded()) {
//                            bottomDialog.show(fragmentManager, "");
//                        }
//                        bottomDialog.fetchURL(barcode.getUrl().getUrl());
//                        String title = barcode.getUrl().getTitle();
//                        String url = barcode.getUrl().getUrl();
//                        break;
//                }
//            }
//        }
//    }
}