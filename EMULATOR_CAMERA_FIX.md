# Android Emulator Camera Fix

## Problem Description

When using the Android Studio emulator, the barcode scanner plugin was failing with the following error:

```
getCameraCharacteristics: 
java.lang.IllegalArgumentException: getCameraCharacteristics:1329: Unable to retrieve camera characteristics for unknown device 0: No such file or directory (-2)
```

## Root Cause

The issue occurred because the ML Kit camera implementation was incorrectly mapping camera IDs:

1. **Android Studio Emulator**: Has cameras with IDs like 1 and 10 (not 0 and 1)
2. **Plugin Logic**: Was assuming `cameraId == 0` meant back camera, `cameraId != 0` meant front camera
3. **Conflict**: When the plugin tried to use camera device ID 0 (which doesn't exist in the emulator), it caused the error

## Solution

### 1. Fixed Camera ID Mapping in MLKitBarcodeScanner.java

**Before:**
```java
CameraSelector cameraSelector = (cameraId == 0) ? 
    CameraSelector.DEFAULT_BACK_CAMERA : 
    CameraSelector.DEFAULT_FRONT_CAMERA;
```

**After:**
```java
private CameraSelector getCameraSelector(int cameraFacing) {
    if (cameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
        return CameraSelector.DEFAULT_FRONT_CAMERA;
    } else {
        return CameraSelector.DEFAULT_BACK_CAMERA;
    }
}
```

### 2. Added Camera Enumeration and Debug Logging

- Added `logAvailableCameras()` method to log all available cameras
- Added debug information to help troubleshoot camera issues
- Added fallback logic when preferred camera fails to initialize

### 3. Improved Error Handling

- Added try/catch with fallback camera selection
- Enhanced logging for better debugging
- Added specific error messages for camera initialization failures

## Key Changes Made

### MLKitBarcodeScanner.java
- Fixed `switchCamera()` method to use camera facing constants instead of raw IDs
- Added camera enumeration logging
- Added fallback camera selection
- Improved error messages and debugging

### QRScanner.java
- Added `logCameraDebugInfo()` method
- Fixed camera parameter passing to ML Kit scanner
- Enhanced debugging output

## Testing on Android Studio Emulator

### 1. Enable Camera in Emulator
1. Open Android Studio AVD Manager
2. Edit your emulator
3. Click "Advanced Settings"
4. Set "Front Camera" and "Back Camera" to "Webcam0" or "Emulated"

### 2. Expected Log Output
With the fix, you should see logs like:
```
MLKitBarcodeScanner: === Camera Debug Information ===
MLKitBarcodeScanner: Legacy Camera API - Number of cameras: 2
MLKitBarcodeScanner: Legacy Camera 1: facing=BACK orientation=90
MLKitBarcodeScanner: Legacy Camera 10: facing=FRONT orientation=270
MLKitBarcodeScanner: Current camera facing setting: BACK
QRScanner: Current camera ID setting: 0
QRScanner: Camera ID matches CAMERA_FACING_BACK (0)
```

### 3. Verification Steps
1. Build and run the app on Android Studio emulator
2. Initialize the barcode scanner
3. Check logcat for the debug information above
4. Verify camera preview appears without errors
5. Test camera switching functionality

## Compatibility

This fix maintains backward compatibility with:
- Physical Android devices
- Different emulator configurations
- Both ML Kit and ZXing scanner implementations

The plugin now properly handles:
- Emulators with non-standard camera IDs (1, 10, etc.)
- Missing cameras (graceful fallback)
- Camera initialization failures
- Better error reporting for debugging

## Future Improvements

Consider adding:
1. Camera capability detection before selection
2. User preference for camera selection
3. Runtime camera availability checking
4. More granular error codes for different failure scenarios
