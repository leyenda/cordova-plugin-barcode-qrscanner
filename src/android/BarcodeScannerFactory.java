package com.bitpay.cordova.qrscanner;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LifecycleOwner;

/**
 * Factory class to create the appropriate barcode scanner
 * Uses ML Kit if available, falls back to ZXing Android Embedded
 */
public class BarcodeScannerFactory {
    private static final String TAG = "BarcodeScannerFactory";
    
    public enum ScannerType {
        ML_KIT,
        ZXING_EMBEDDED,
        AUTO // Automatically choose best available
    }
    
    public enum ScannerCapability {
        BASIC_SCANNING,
        TORCH_SUPPORT,
        CAMERA_SWITCHING,
        AUTOFOCUS,
        PERFORMANCE_OPTIMIZED
    }
    
    /**
     * Create the best available scanner
     */
    public static ScannerType getBestAvailableScanner(Context context) {
        // Check if ML Kit is available
        if (MLKitBarcodeScanner.isAvailable(context)) {
            Log.i(TAG, "ML Kit is available - using ML Kit scanner");
            return ScannerType.ML_KIT;
        } else {
            Log.i(TAG, "ML Kit not available - falling back to ZXing scanner");
            return ScannerType.ZXING_EMBEDDED;
        }
    }
    
    /**
     * Check if a specific scanner type is available
     */
    public static boolean isScannerAvailable(Context context, ScannerType scannerType) {
        switch (scannerType) {
            case ML_KIT:
                return MLKitBarcodeScanner.isAvailable(context);
            case ZXING_EMBEDDED:
                return true; // ZXing is always available
            case AUTO:
                return true; // Auto mode always works (falls back if needed)
            default:
                return false;
        }
    }
    
    /**
     * Get scanner capabilities
     */
    public static boolean hasCapability(Context context, ScannerType scannerType, ScannerCapability capability) {
        switch (scannerType) {
            case ML_KIT:
                return getMLKitCapabilities(capability);
            case ZXING_EMBEDDED:
                return getZXingCapabilities(capability);
            case AUTO:
                ScannerType bestScanner = getBestAvailableScanner(context);
                return hasCapability(context, bestScanner, capability);
            default:
                return false;
        }
    }
    
    private static boolean getMLKitCapabilities(ScannerCapability capability) {
        switch (capability) {
            case BASIC_SCANNING:
                return true;
            case TORCH_SUPPORT:
                return true;
            case CAMERA_SWITCHING:
                return true;
            case AUTOFOCUS:
                return true;
            case PERFORMANCE_OPTIMIZED:
                return true; // ML Kit is highly optimized
            default:
                return false;
        }
    }
    
    private static boolean getZXingCapabilities(ScannerCapability capability) {
        switch (capability) {
            case BASIC_SCANNING:
                return true;
            case TORCH_SUPPORT:
                return true;
            case CAMERA_SWITCHING:
                return true;
            case AUTOFOCUS:
                return true;
            case PERFORMANCE_OPTIMIZED:
                return false; // ZXing is less optimized than ML Kit
            default:
                return false;
        }
    }
    
    /**
     * Get performance rating (1-10, 10 being best)
     */
    public static int getPerformanceRating(Context context, ScannerType scannerType) {
        switch (scannerType) {
            case ML_KIT:
                return MLKitBarcodeScanner.isAvailable(context) ? 9 : 0;
            case ZXING_EMBEDDED:
                return 6; // Decent performance
            case AUTO:
                ScannerType bestScanner = getBestAvailableScanner(context);
                return getPerformanceRating(context, bestScanner);
            default:
                return 0;
        }
    }
    
    /**
     * Get scanner information
     */
    public static String getScannerInfo(Context context, ScannerType scannerType) {
        switch (scannerType) {
            case ML_KIT:
                if (MLKitBarcodeScanner.isAvailable(context)) {
                    return "Google ML Kit Barcode Scanning API - High performance, hardware accelerated";
                } else {
                    return "Google ML Kit not available (requires Google Play Services)";
                }
            case ZXING_EMBEDDED:
                return "ZXing Android Embedded - Reliable fallback scanner";
            case AUTO:
                ScannerType bestScanner = getBestAvailableScanner(context);
                return "Auto-selected: " + getScannerInfo(context, bestScanner);
            default:
                return "Unknown scanner type";
        }
    }
    
    /**
     * Get recommended scanner for specific use cases
     */
    public static ScannerType getRecommendedScanner(Context context, String useCase) {
        switch (useCase.toLowerCase()) {
            case "performance":
            case "speed":
            case "battery":
                return MLKitBarcodeScanner.isAvailable(context) ? ScannerType.ML_KIT : ScannerType.ZXING_EMBEDDED;
                
            case "compatibility":
            case "fallback":
                return ScannerType.ZXING_EMBEDDED;
                
            case "modern":
            case "latest":
                return ScannerType.ML_KIT;
                
            default:
                return ScannerType.AUTO;
        }
    }
    
    /**
     * Log scanner selection information
     */
    public static void logScannerSelection(Context context) {
        ScannerType bestScanner = getBestAvailableScanner(context);
        Log.i(TAG, "=== Barcode Scanner Selection ===");
        Log.i(TAG, "Selected Scanner: " + bestScanner);
        Log.i(TAG, "Scanner Info: " + getScannerInfo(context, bestScanner));
        Log.i(TAG, "Performance Rating: " + getPerformanceRating(context, bestScanner) + "/10");
        Log.i(TAG, "ML Kit Available: " + MLKitBarcodeScanner.isAvailable(context));
        Log.i(TAG, "ZXing Available: " + isScannerAvailable(context, ScannerType.ZXING_EMBEDDED));
        Log.i(TAG, "================================");
    }
}
