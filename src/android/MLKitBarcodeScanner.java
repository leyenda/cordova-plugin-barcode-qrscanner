package com.bitpay.cordova.qrscanner;

import android.content.Context;
import android.util.Log;
import android.hardware.Camera;
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
    private androidx.camera.core.Camera cameraX;
    private Preview preview;
    private ImageAnalysis imageAnalysis;
    private BarcodeScanner barcodeScanner;
    private ExecutorService cameraExecutor;
    private ScanResultCallback callback;
    private boolean isScanning = false;
    private int currentCameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
    private BarcodeScannerFactory.PerformanceMonitor performanceMonitor;
    
    public interface ScanResultCallback {
        void onScanResult(String result);
        void onScanError(Exception error);
    }
    
    public MLKitBarcodeScanner(Context context, LifecycleOwner lifecycleOwner) {
        this.context = context;
        this.lifecycleOwner = lifecycleOwner;
        this.cameraExecutor = Executors.newSingleThreadExecutor();
        this.performanceMonitor = new BarcodeScannerFactory.PerformanceMonitor("ML_KIT");
        
        performanceMonitor.startInitialization();
        
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
        performanceMonitor.endInitialization();
        performanceMonitor.logMemoryUsage();
        Log.i(TAG, "ML Kit Barcode Scanner initialized with performance monitoring");
    }
    
    /**
     * Check if Google Play Services is available for ML Kit
     * Returns false if ZXING_ONLY mode is enabled
     */
    public static boolean isAvailable(Context context) {
        // First check if ZXING_ONLY mode is enabled
        if (isZXingOnlyMode(context)) {
            Log.i(TAG, "ML Kit disabled - ZXING_ONLY mode is enabled");
            return false;
        }
        
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
     * Check if ZXING_ONLY mode is enabled via plugin configuration
     */
    private static boolean isZXingOnlyMode(Context context) {
        try {
            // Get the string resource that was set by the plugin.xml
            int resourceId = context.getResources().getIdentifier(
                "qrscanner_zxing_only", "string", context.getPackageName());
            
            if (resourceId != 0) {
                String zxingOnlyValue = context.getResources().getString(resourceId);
                boolean isZxingOnly = "true".equalsIgnoreCase(zxingOnlyValue);
                Log.i(TAG, "ZXING_ONLY preference in MLKit class: " + zxingOnlyValue + " (parsed as: " + isZxingOnly + ")");
                return isZxingOnly;
            } else {
                Log.i(TAG, "ZXING_ONLY preference not found in MLKit class, defaulting to hybrid mode");
                return false;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error reading ZXING_ONLY preference in MLKit class: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Initialize the camera and preview
     */
    public void initializeCamera(PreviewView previewView, ScanResultCallback callback) {
        this.previewView = previewView;
        this.callback = callback;
        
        // Log available cameras for debugging
        logAvailableCameras();
        
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
     * Log available cameras for debugging
     */
    private void logAvailableCameras() {
        try {
            Log.i(TAG, "=== Camera Debug Information ===");
            
            // Log legacy camera API information
            int numCameras = Camera.getNumberOfCameras();
            Log.i(TAG, "Legacy Camera API - Number of cameras: " + numCameras);
            
            for (int i = 0; i < numCameras; i++) {
                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(i, info);
                String facing = (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) ? "BACK" : "FRONT";
                Log.i(TAG, "Legacy Camera " + i + ": facing=" + facing + " orientation=" + info.orientation);
            }
            
            Log.i(TAG, "Current camera facing setting: " + 
                (currentCameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK ? "BACK" : "FRONT"));
            Log.i(TAG, "===============================");
            
        } catch (Exception e) {
            Log.w(TAG, "Error logging camera information: " + e.getMessage(), e);
        }
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
        
        // Select camera based on current facing direction
        CameraSelector cameraSelector = getCameraSelector(currentCameraFacing);
        
        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll();
            
            // Log camera selection attempt
            Log.i(TAG, "Attempting to bind camera with selector: " + 
                (currentCameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK ? "BACK" : "FRONT"));
            
            // Bind use cases to camera
            cameraX = cameraProvider.bindToLifecycle(
                lifecycleOwner, cameraSelector, preview, imageAnalysis);
                
            Log.i(TAG, "Camera started successfully with facing: " + 
                (currentCameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK ? "back" : "front"));
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting camera: " + e.getMessage(), e);
            Log.e(TAG, "Camera selector was: " + 
                (currentCameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK ? "DEFAULT_BACK_CAMERA" : "DEFAULT_FRONT_CAMERA"));
            
            // Try fallback to any available camera if the preferred one fails
            if (currentCameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                Log.w(TAG, "Back camera failed, trying front camera as fallback");
                try {
                    CameraSelector fallbackSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                    cameraX = cameraProvider.bindToLifecycle(
                        lifecycleOwner, fallbackSelector, preview, imageAnalysis);
                    currentCameraFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;
                    Log.i(TAG, "Successfully fell back to front camera");
                    return;
                } catch (Exception fallbackException) {
                    Log.e(TAG, "Front camera fallback also failed: " + fallbackException.getMessage(), fallbackException);
                }
            }
            
            if (callback != null) {
                callback.onScanError(e);
            }
        }
    }
    
    /**
     * Switch camera (front/back)
     * @param cameraFacing Camera.CameraInfo.CAMERA_FACING_BACK or Camera.CameraInfo.CAMERA_FACING_FRONT
     */
    public void switchCamera(int cameraFacing) {
        Log.i(TAG, "Switching camera to facing: " + 
            (cameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK ? "back" : "front"));
            
        currentCameraFacing = cameraFacing;
        
        if (cameraProvider != null) {
            CameraSelector cameraSelector = getCameraSelector(cameraFacing);
                
            try {
                // Stop scanning during camera switch
                boolean wasScanning = isScanning;
                if (wasScanning) {
                    stopScanning();
                }
                
                cameraProvider.unbindAll();
                cameraX = cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, imageAnalysis);
                Log.i(TAG, "Camera switched successfully to: " + 
                    (cameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK ? "back" : "front"));
                
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
     * Get appropriate CameraSelector for the given camera facing direction
     */
    private CameraSelector getCameraSelector(int cameraFacing) {
        if (cameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            return CameraSelector.DEFAULT_FRONT_CAMERA;
        } else {
            return CameraSelector.DEFAULT_BACK_CAMERA;
        }
    }
    
    /**
     * Enable/disable torch
     */
    public void setTorchEnabled(boolean enabled) {
        if (cameraX != null && cameraX.getCameraInfo().hasFlashUnit()) {
            cameraX.getCameraControl().enableTorch(enabled);
            Log.i(TAG, "Torch " + (enabled ? "enabled" : "disabled"));
        }
    }
    
    /**
     * Start scanning
     */
    public void startScanning() {
        isScanning = true;
        performanceMonitor.startScan();
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
        return cameraX != null && cameraX.getCameraInfo().hasFlashUnit();
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
                            performanceMonitor.endScan(rawValue);
                            performanceMonitor.logMemoryUsage();
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
