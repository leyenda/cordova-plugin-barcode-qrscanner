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
        // Check if ZXING_ONLY mode is enabled
        if (isZXingOnlyMode(context)) {
            Log.i(TAG, "ZXING_ONLY mode enabled - forcing ZXing scanner");
            return ScannerType.ZXING_EMBEDDED;
        }
        
        // Check if ML Kit is available (only if ML Kit classes are present)
        if (isMLKitClassAvailable() && isMLKitRuntimeAvailable(context)) {
            Log.i(TAG, "ML Kit is available - using ML Kit scanner");
            return ScannerType.ML_KIT;
        } else {
            Log.i(TAG, "ML Kit not available - falling back to ZXing scanner");
            return ScannerType.ZXING_EMBEDDED;
        }
    }
    
    /**
     * Check if ML Kit classes are available at compile time
     */
    private static boolean isMLKitClassAvailable() {
        try {
            Class.forName("com.bitpay.cordova.qrscanner.MLKitBarcodeScanner");
            return true;
        } catch (ClassNotFoundException e) {
            Log.i(TAG, "MLKitBarcodeScanner class not available - compiled in ZXING_ONLY mode");
            return false;
        }
    }
    
    /**
     * Check if ML Kit is available at runtime (only call if class exists)
     */
    private static boolean isMLKitRuntimeAvailable(Context context) {
        try {
            // Use reflection to avoid compile-time dependency
            Class<?> mlKitClass = Class.forName("com.bitpay.cordova.qrscanner.MLKitBarcodeScanner");
            java.lang.reflect.Method isAvailableMethod = mlKitClass.getMethod("isAvailable", Context.class);
            return (Boolean) isAvailableMethod.invoke(null, context);
        } catch (Exception e) {
            Log.w(TAG, "Error checking ML Kit runtime availability: " + e.getMessage());
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
                Log.i(TAG, "ZXING_ONLY preference found: " + zxingOnlyValue + " (parsed as: " + isZxingOnly + ")");
                return isZxingOnly;
            } else {
                Log.i(TAG, "ZXING_ONLY preference not found, defaulting to hybrid mode");
                return false;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error reading ZXING_ONLY preference: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if a specific scanner type is available
     */
    public static boolean isScannerAvailable(Context context, ScannerType scannerType) {
        switch (scannerType) {
            case ML_KIT:
                return isMLKitClassAvailable() && isMLKitRuntimeAvailable(context);
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
                return (isMLKitClassAvailable() && isMLKitRuntimeAvailable(context)) ? 9 : 0;
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
                if (isMLKitClassAvailable() && isMLKitRuntimeAvailable(context)) {
                    return "Google ML Kit Barcode Scanning API - High performance, hardware accelerated";
                } else {
                    return "Google ML Kit not available (requires Google Play Services or compiled in ZXING_ONLY mode)";
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
                return (isMLKitClassAvailable() && isMLKitRuntimeAvailable(context)) ? ScannerType.ML_KIT : ScannerType.ZXING_EMBEDDED;
                
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
        boolean zxingOnlyMode = isZXingOnlyMode(context);
        ScannerType bestScanner = getBestAvailableScanner(context);
        Log.i(TAG, "=== Barcode Scanner Selection ===");
        Log.i(TAG, "ZXING_ONLY Mode: " + zxingOnlyMode);
        Log.i(TAG, "Selected Scanner: " + bestScanner);
        Log.i(TAG, "Scanner Info: " + getScannerInfo(context, bestScanner));
        Log.i(TAG, "Performance Rating: " + getPerformanceRating(context, bestScanner) + "/10");
        Log.i(TAG, "ML Kit Class Available: " + isMLKitClassAvailable());
        Log.i(TAG, "ML Kit Runtime Available: " + (isMLKitClassAvailable() && isMLKitRuntimeAvailable(context)));
        Log.i(TAG, "ZXing Available: " + isScannerAvailable(context, ScannerType.ZXING_EMBEDDED));
        Log.i(TAG, "Performance Monitoring: ENABLED");
        Log.i(TAG, "================================");
    }
    
    /**
     * Performance monitoring utilities
     */
    public static class PerformanceMonitor {
        private static final String TAG = "ScannerPerformance";
        private long scanStartTime;
        private long initStartTime;
        private String scannerType;
        
        public PerformanceMonitor(String scannerType) {
            this.scannerType = scannerType;
        }
        
        public void startInitialization() {
            initStartTime = System.currentTimeMillis();
            Log.i(TAG, "[" + scannerType + "] Scanner initialization started");
        }
        
        public void endInitialization() {
            long initTime = System.currentTimeMillis() - initStartTime;
            Log.i(TAG, "[" + scannerType + "] Initialization time: " + initTime + "ms");
        }
        
        public void startScan() {
            scanStartTime = System.currentTimeMillis();
            Log.i(TAG, "[" + scannerType + "] Scan started");
        }
        
        public void endScan(String result) {
            long scanTime = System.currentTimeMillis() - scanStartTime;
            Log.i(TAG, "[" + scannerType + "] Scan completed in " + scanTime + "ms");
            Log.i(TAG, "[" + scannerType + "] Result length: " + (result != null ? result.length() : 0) + " chars");
            
            // Log performance category
            if (scanTime < 500) {
                Log.i(TAG, "[" + scannerType + "] Performance: EXCELLENT (<500ms)");
            } else if (scanTime < 1000) {
                Log.i(TAG, "[" + scannerType + "] Performance: GOOD (<1s)");
            } else if (scanTime < 2000) {
                Log.i(TAG, "[" + scannerType + "] Performance: AVERAGE (<2s)");
            } else {
                Log.i(TAG, "[" + scannerType + "] Performance: SLOW (>2s)");
            }
        }
        
        public void logMemoryUsage() {
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            long availableMemory = maxMemory - usedMemory;
            
            Log.i(TAG, "[" + scannerType + "] Memory - Used: " + (usedMemory / 1024 / 1024) + "MB");
            Log.i(TAG, "[" + scannerType + "] Memory - Available: " + (availableMemory / 1024 / 1024) + "MB");
        }
    }
}
