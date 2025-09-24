package com.bitpay.cordova.qrscanner;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ML Kit Barcode Scanner Implementation
 * Provides high-performance barcode scanning using Google ML Kit
 */
public class MLKitBarcodeScanner {
    private static final String TAG = "MLKitBarcodeScanner";
    
    private Context context;
    private LifecycleOwner lifecycleOwner;
    private PreviewView previewView;
    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private Preview preview;
    private ImageAnalysis imageAnalysis;
    private BarcodeScanner barcodeScanner;
    private ExecutorService cameraExecutor;
    private ScanResultCallback callback;
    private boolean isScanning = false;
    
    public interface ScanResultCallback {
        void onScanResult(String result);
        void onScanError(Exception error);
    }
    
    public MLKitBarcodeScanner(Context context, LifecycleOwner lifecycleOwner) {
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
        this.cameraExecutor = Executors.newSingleThreadExecutor();
        
        // Configure ML Kit barcode scanner
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_CODE_128
            )
            .build();
            
        this.barcodeScanner = BarcodeScanning.getClient(options);
        Log.i(TAG, "ML Kit Barcode Scanner initialized");
    }
    
    /**
     * Check if Google Play Services is available for ML Kit
     */
    public static boolean isAvailable(Context context) {
        try {
            GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
            int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context);
            boolean available = resultCode == ConnectionResult.SUCCESS;
            Log.i(TAG, "Google Play Services available: " + available);
            return available;
        } catch (Exception e) {
            Log.w(TAG, "Error checking Google Play Services availability: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Initialize the camera and preview
     */
    public void initializeCamera(PreviewView previewView, ScanResultCallback callback) {
        this.previewView = previewView;
        this.callback = callback;
        
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
            ProcessCameraProvider.getInstance(context);
            
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                startCamera();
                Log.i(TAG, "Camera initialized successfully");
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error initializing camera: " + e.getMessage(), e);
                if (callback != null) {
                    callback.onScanError(e);
                }
            }
        }, ContextCompat.getMainExecutor(context));
    }
    
    /**
     * Start the camera and image analysis
     */
    private void startCamera() {
        // Preview
        preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        
        // Image analysis for barcode scanning
        imageAnalysis = new ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build();
            
        imageAnalysis.setAnalyzer(cameraExecutor, new BarcodeAnalyzer());
        
        // Select back camera as default
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        
        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll();
            
            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner, cameraSelector, preview, imageAnalysis);
                
            Log.i(TAG, "Camera started successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting camera: " + e.getMessage(), e);
            if (callback != null) {
                callback.onScanError(e);
            }
        }
    }
    
    /**
     * Switch camera (front/back)
     */
    public void switchCamera(int cameraId) {
        if (cameraProvider != null) {
            CameraSelector cameraSelector = (cameraId == 0) ? 
                CameraSelector.DEFAULT_BACK_CAMERA : 
                CameraSelector.DEFAULT_FRONT_CAMERA;
                
            try {
                // Stop scanning during camera switch
                boolean wasScanning = isScanning;
                if (wasScanning) {
                    stopScanning();
                }
                
                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, imageAnalysis);
                Log.i(TAG, "Camera switched to: " + (cameraId == 0 ? "back" : "front"));
                
                // Resume scanning if it was active
                if (wasScanning) {
                    startScanning();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error switching camera: " + e.getMessage(), e);
                if (callback != null) {
                    callback.onScanError(e);
                }
            }
        }
    }
    
    /**
     * Enable/disable torch
     */
    public void setTorchEnabled(boolean enabled) {
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            camera.getCameraControl().enableTorch(enabled);
            Log.i(TAG, "Torch " + (enabled ? "enabled" : "disabled"));
        }
    }
    
    /**
     * Start scanning
     */
    public void startScanning() {
        isScanning = true;
        Log.i(TAG, "Started scanning");
    }
    
    /**
     * Stop scanning
     */
    public void stopScanning() {
        isScanning = false;
        Log.i(TAG, "Stopped scanning");
    }
    
    /**
     * Check if torch is available
     */
    public boolean hasTorch() {
        return camera != null && camera.getCameraInfo().hasFlashUnit();
    }
    
    /**
     * Release resources
     */
    public void release() {
        isScanning = false;
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        if (barcodeScanner != null) {
            barcodeScanner.close();
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        Log.i(TAG, "Resources released");
    }
    
    /**
     * Image analyzer for barcode detection
     */
    private class BarcodeAnalyzer implements ImageAnalysis.Analyzer {
        @Override
        public void analyze(@NonNull ImageProxy imageProxy) {
            if (!isScanning) {
                imageProxy.close();
                return;
            }
            
            @SuppressWarnings("UnsafeOptInUsageError")
            InputImage image = InputImage.fromMediaImage(
                imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());
                
            barcodeScanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    for (Barcode barcode : barcodes) {
                        String rawValue = barcode.getRawValue();
                        if (rawValue != null && !rawValue.isEmpty() && isScanning) {
                            isScanning = false; // Stop scanning after first result
                            Log.i(TAG, "Barcode detected: " + barcode.getFormat());
                            if (callback != null) {
                                callback.onScanResult(rawValue);
                            }
                            break;
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Barcode detection failed: " + e.getMessage(), e);
                    if (callback != null) {
                        callback.onScanError(e);
                    }
                })
                .addOnCompleteListener(task -> imageProxy.close());
        }
    }
}
