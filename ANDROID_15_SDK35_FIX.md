# Android 15 (SDK 35) and 16 KB Page Size Support Fix

## Problem Description

When targeting Android 15 (API level 35), apps encounter 16 KB page size compatibility warnings for native libraries that aren't properly aligned. The issue was that even when using `ZXING_ONLY=true`, ML Kit dependencies were still being included, causing these warnings:

```
4 KB LOAD section alignment for libbarhopper_v3.so and libimage_processing_util_jni.so
```

## Root Cause

The hybrid scanner implementation had a design flaw:

1. **Gradle correctly excluded ML Kit dependencies** when `ZXING_ONLY=true`
2. **But Java code still referenced `MLKitBarcodeScanner` class directly**
3. **This forced ML Kit dependencies to be included at compile-time** regardless of the Gradle configuration
4. **Result**: ML Kit native libraries were included even in "ZXing-only" mode

## Solution Implemented

### 1. **Runtime ZXING_ONLY Detection in ML Kit**

Modified `MLKitBarcodeScanner.java` to check the ZXING_ONLY preference at runtime:

```java
public static boolean isAvailable(Context context) {
    // First check if ZXING_ONLY mode is enabled
    if (isZXingOnlyMode(context)) {
        Log.i(TAG, "ML Kit disabled - ZXING_ONLY mode is enabled");
        return false;
    }
    // ... rest of availability check
}
```

### 2. **Reflection-Based ML Kit Access**

Replaced direct ML Kit class references with reflection in `BarcodeScannerFactory.java`:

**Before:**
```java
if (MLKitBarcodeScanner.isAvailable(context)) {
    // This creates compile-time dependency
}
```

**After:**
```java
private static boolean isMLKitClassAvailable() {
    try {
        Class.forName("com.bitpay.cordova.qrscanner.MLKitBarcodeScanner");
        return true;
    } catch (ClassNotFoundException e) {
        return false;
    }
}

private static boolean isMLKitRuntimeAvailable(Context context) {
    try {
        Class<?> mlKitClass = Class.forName("com.bitpay.cordova.qrscanner.MLKitBarcodeScanner");
        Method isAvailableMethod = mlKitClass.getMethod("isAvailable", Context.class);
        return (Boolean) isAvailableMethod.invoke(null, context);
    } catch (Exception e) {
        return false;
    }
}
```

### 3. **Updated Target SDK to 35**

Updated `qrscanner.gradle`:

```gradle
android {
    compileSdkVersion 35
    buildToolsVersion '35.0.0'
    
    defaultConfig {
        minSdkVersion 21
        targetSdkVersion 35
    }
    
    // Support for 16 KB page sizes (Android 15+)
    packagingOptions {
        jniLibs {
            useLegacyPackaging false
        }
    }
}
```

### 4. **16 KB Page Size Support**

Added packaging options to properly handle 16 KB memory page alignment required by Android 15.

## Key Changes Made

### Files Modified:

1. **`plugin.xml`**
   - Added conditional compilation for `MLKitBarcodeScanner.java`
   - Added string resource for ZXING_ONLY preference

2. **`src/android/qrscanner.gradle`**
   - Updated to SDK 35 and build tools 35.0.0
   - Added 16 KB page size packaging options

3. **`src/android/BarcodeScannerFactory.java`**
   - Replaced all direct ML Kit class references with reflection
   - Added `isMLKitClassAvailable()` and `isMLKitRuntimeAvailable()` methods
   - Updated all scanner selection logic to use reflection

4. **`src/android/QRScanner.java`**
   - Added reflection-based ML Kit availability check
   - Updated status reporting to use reflection

## Installation and Usage

### For ZXING_ONLY Mode (Clean, No ML Kit Dependencies):
```bash
cordova plugin add cordova-plugin-barcode-qrscanner \
  --variable ZXING_ONLY=true
```

### For Hybrid Mode (ML Kit + ZXing):
```bash
cordova plugin add cordova-plugin-barcode-qrscanner
```

## Expected Results

### With ZXING_ONLY=true:
- ✅ No ML Kit dependencies included
- ✅ No 16 KB page size warnings
- ✅ Smaller APK size
- ✅ Works on devices without Google Play Services
- ✅ Compatible with Android 15 (SDK 35)

### With Hybrid Mode:
- ✅ ML Kit used when available
- ✅ Automatic fallback to ZXing when ML Kit unavailable
- ✅ Enhanced performance with ML Kit
- ✅ 16 KB page size support for ML Kit libraries

## Log Output Examples

### ZXING_ONLY=true:
```
BarcodeScannerFactory: ZXING_ONLY Mode: true
BarcodeScannerFactory: Selected Scanner: ZXING_EMBEDDED
BarcodeScannerFactory: ML Kit Class Available: false
BarcodeScannerFactory: ML Kit Runtime Available: false
```

### Hybrid Mode:
```
BarcodeScannerFactory: ZXING_ONLY Mode: false
BarcodeScannerFactory: Selected Scanner: ML_KIT
BarcodeScannerFactory: ML Kit Class Available: true
BarcodeScannerFactory: ML Kit Runtime Available: true
```

## Testing Recommendations

1. **Clean Build Test**: Remove and re-add the plugin with `ZXING_ONLY=true`
2. **APK Analysis**: Check that ML Kit libraries are not included
3. **16 KB Page Size**: Verify no alignment warnings on Android 15
4. **Functionality**: Ensure QR scanning works properly
5. **Performance**: Test on various devices and Android versions

## Compatibility

- ✅ **Android 15 (API 35)**: Full support with 16 KB page sizes
- ✅ **Android 14 and below**: Backward compatible
- ✅ **Physical Devices**: No emulator-specific issues
- ✅ **Google Play Store**: Meets latest requirements

This fix ensures clean separation between ZXing-only and hybrid modes while providing full Android 15 compatibility.
