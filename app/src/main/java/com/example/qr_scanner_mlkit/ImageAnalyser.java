package com.example.qr_scanner_mlkit;

import android.annotation.SuppressLint;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.Image;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ImageAnalyser implements ImageAnalysis.Analyzer {
    private FragmentManager fragmentManager;
    private BottomDialog bottomDialog;

    public ImageAnalyser(FragmentManager fragmentManager, BottomDialog bottomDialog) {
        this.fragmentManager = fragmentManager;
        bottomDialog = new BottomDialog();
    }

    public ImageAnalyser(FragmentManager supportFragmentManager) {
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {
        ScanBarCode(image);
    }

    private void ScanBarCode(ImageProxy image) {
        @SuppressLint("UnsafeOptInUsageError") Image image1 = image.getImage();
        assert image1 != null;
        InputImage inputImage = InputImage
                .fromMediaImage(image1,
                        image.getImageInfo()
                                .getRotationDegrees());

        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                                Barcode.FORMAT_QR_CODE,
                                Barcode.FORMAT_AZTEC)
                        .build();

        BarcodeScanner scanner = BarcodeScanning.getClient(options);
        Task<List<Barcode>> result = scanner.process(inputImage)
                .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                    @Override
                    public void onSuccess(List<Barcode> barcodes) {
                        readerBarcodeData(barcodes);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(bottomDialog.getContext(), "Failed to read code", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<List<Barcode>>() {
                    @Override
                    public void onComplete(@NonNull @NotNull Task<List<Barcode>> task) {
                        image.close();
                    }
                });
    }

    private void readerBarcodeData(List<Barcode> barcodes) {

        for (Barcode barcode : barcodes) {
            Rect bounds = barcode.getBoundingBox();
            Point[] corners = barcode.getCornerPoints();

            String rawValue = barcode.getRawValue();
            int valueType = barcode.getValueType();

            switch (valueType) {

                case Barcode.TYPE_WIFI:

                    String ssid = barcode.getWifi().getSsid();
                    String password = barcode.getWifi().getPassword();
                    int type = barcode.getWifi().getEncryptionType();
                    break;

                case Barcode.TYPE_URL:

                    if (!bottomDialog.isAdded()) {
                        bottomDialog.show(fragmentManager, "");
                    }

                    bottomDialog.fetchURL(barcode.getUrl().getUrl());
                    String title = barcode.getUrl().getTitle();
                    String url = barcode.getUrl().getUrl();
                    break;

            }

        }

    }
}
