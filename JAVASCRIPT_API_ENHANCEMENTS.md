# JavaScript API Enhancements

## 🎯 Overview
This document outlines the enhanced JavaScript API features that expose the new Android capabilities and provide better developer experience.

## ✨ New Status Fields

The `getStatus()` method now returns additional fields on Android devices:

### Enhanced Status Object
```javascript
QRScanner.getStatus(function(status) {
  console.log('Status:', status);
  // Output includes new fields:
  // {
  //   authorized: true,
  //   denied: false,
  //   restricted: false,
  //   prepared: true,
  //   scanning: false,
  //   previewing: true,
  //   showing: true,
  //   lightEnabled: false,
  //   canOpenSettings: true,
  //   canEnableLight: true,
  //   canChangeCamera: true,
  //   currentCamera: 0,
  //   // NEW ANDROID FIELDS:
  //   hasAutofocus: true,           // Hardware autofocus capability
  //   hasFrontCamera: true,         // Front camera availability
  //   hasBackCamera: true,          // Back camera availability
  //   shouldShowRationale: false    // Permission rationale needed
  // }
});
```

## 🚀 New Utility Methods

### 1. Hardware Capability Checking

#### `hasAutofocus(callback)`
Check if the device supports hardware autofocus:
```javascript
QRScanner.hasAutofocus(function(hasAutofocus) {
  if (hasAutofocus) {
    console.log('Device supports autofocus - better scanning performance');
  } else {
    console.log('No autofocus - may need better lighting');
  }
});
```

#### `hasFrontCamera(callback)`
Check front camera availability:
```javascript
QRScanner.hasFrontCamera(function(hasFront) {
  if (hasFront) {
    // Enable front camera button in UI
    enableFrontCameraButton();
  }
});
```

#### `hasBackCamera(callback)`
Check back camera availability:
```javascript
QRScanner.hasBackCamera(function(hasBack) {
  if (!hasBack) {
    // Handle devices without back camera (rare)
    showNoBackCameraWarning();
  }
});
```

### 2. Permission Handling

#### `shouldShowPermissionRationale(callback)`
Determine if you should explain why camera permission is needed:
```javascript
QRScanner.shouldShowPermissionRationale(function(shouldShow) {
  if (shouldShow) {
    // Show explanation dialog before requesting permission
    showPermissionExplanationDialog(function() {
      QRScanner.prepare(function(err, status) {
        // Handle permission result
      });
    });
  } else {
    // Direct permission request
    QRScanner.prepare(function(err, status) {
      // Handle permission result
    });
  }
});
```

### 3. Enhanced Camera Switching

#### `useCameraWithCheck(cameraIndex, callback)`
Safe camera switching with capability validation:
```javascript
// Switch to front camera with validation
QRScanner.useCameraWithCheck(1, function(err, status) {
  if (err) {
    console.log('Cannot switch to front camera:', err._message);
    // Fall back to back camera or show error
  } else {
    console.log('Successfully switched to front camera');
  }
});

// Switch to back camera with validation
QRScanner.useCameraWithCheck(0, function(err, status) {
  if (err) {
    console.log('Cannot switch to back camera:', err._message);
  } else {
    console.log('Successfully switched to back camera');
  }
});
```

## 🔧 New Error Codes

Enhanced error handling with new error types:

```javascript
QRScanner.prepare(function(err, status) {
  if (err) {
    switch (err.code) {
      case 9: // CAMERA_INITIALIZATION_FAILED
        console.log('Camera failed to initialize - try restarting app');
        break;
      case 10: // AUTOFOCUS_UNAVAILABLE
        console.log('Autofocus not available - manual focus may be needed');
        break;
      case 11: // LOW_MEMORY_WARNING
        console.log('Low memory - close other apps for better performance');
        break;
      default:
        console.log('Error:', err.name, '-', err._message);
    }
  }
});
```

### Complete Error Code Reference
```javascript
const QRScannerErrors = {
  UNEXPECTED_ERROR: 0,
  CAMERA_ACCESS_DENIED: 1,
  CAMERA_ACCESS_RESTRICTED: 2,
  BACK_CAMERA_UNAVAILABLE: 3,
  FRONT_CAMERA_UNAVAILABLE: 4,
  CAMERA_UNAVAILABLE: 5,
  SCAN_CANCELED: 6,
  LIGHT_UNAVAILABLE: 7,
  OPEN_SETTINGS_UNAVAILABLE: 8,
  // NEW ERROR CODES:
  CAMERA_INITIALIZATION_FAILED: 9,
  AUTOFOCUS_UNAVAILABLE: 10,
  LOW_MEMORY_WARNING: 11
};
```

## 📱 Practical Usage Examples

### 1. Smart Permission Handling
```javascript
function requestCameraPermission() {
  QRScanner.shouldShowPermissionRationale(function(shouldShow) {
    if (shouldShow) {
      // Show custom explanation
      showDialog({
        title: 'Camera Permission Needed',
        message: 'This app needs camera access to scan QR codes. Your privacy is protected - no images are stored.',
        onConfirm: function() {
          QRScanner.prepare(handlePermissionResult);
        }
      });
    } else {
      QRScanner.prepare(handlePermissionResult);
    }
  });
}

function handlePermissionResult(err, status) {
  if (err) {
    if (err.code === 1) { // CAMERA_ACCESS_DENIED
      // Show settings button if permanently denied
      QRScanner.getStatus(function(status) {
        if (status.denied && !status.shouldShowRationale) {
          showSettingsDialog();
        }
      });
    }
  }
}
```

### 2. Adaptive UI Based on Hardware
```javascript
function setupCameraUI() {
  // Check available cameras
  QRScanner.getStatus(function(status) {
    // Enable/disable camera switch button
    if (status.canChangeCamera && status.hasFrontCamera && status.hasBackCamera) {
      enableCameraSwitchButton();
    }
    
    // Show autofocus indicator
    if (status.hasAutofocus) {
      showAutofocusIndicator();
    }
    
    // Enable flash button only for back camera
    if (status.currentCamera === 0 && status.canEnableLight) {
      enableFlashButton();
    } else {
      disableFlashButton();
    }
  });
}
```

### 3. Robust Camera Switching
```javascript
function switchToFrontCamera() {
  QRScanner.useCameraWithCheck(1, function(err, status) {
    if (err) {
      showToast('Front camera not available: ' + err._message);
      return;
    }
    
    // Update UI for front camera
    updateCameraUI('front');
    
    // Disable flash for front camera
    if (status.lightEnabled) {
      QRScanner.disableLight();
    }
  });
}

function switchToBackCamera() {
  QRScanner.useCameraWithCheck(0, function(err, status) {
    if (err) {
      showToast('Back camera not available: ' + err._message);
      return;
    }
    
    // Update UI for back camera
    updateCameraUI('back');
    
    // Enable flash button if available
    if (status.canEnableLight) {
      enableFlashButton();
    }
  });
}
```

### 4. Performance Monitoring
```javascript
function initializeScanner() {
  QRScanner.prepare(function(err, status) {
    if (err) {
      if (err.code === 11) { // LOW_MEMORY_WARNING
        // Suggest closing other apps
        showLowMemoryWarning();
      } else if (err.code === 9) { // CAMERA_INITIALIZATION_FAILED
        // Suggest restart
        showCameraInitFailedDialog();
      }
    }
  });
}

function showLowMemoryWarning() {
  showDialog({
    title: 'Performance Warning',
    message: 'Your device is low on memory. For better scanning performance, try closing other apps.',
    buttons: ['OK', 'Close Other Apps']
  });
}
```

## 🔄 Migration Guide

### From Previous Versions
All existing code continues to work unchanged. New features are additive:

```javascript
// ✅ Existing code still works
QRScanner.getStatus(function(status) {
  console.log('Camera ready:', status.prepared);
});

// ✅ New capabilities available
QRScanner.getStatus(function(status) {
  console.log('Camera ready:', status.prepared);
  // New fields available:
  if (status.hasAutofocus !== undefined) {
    console.log('Autofocus available:', status.hasAutofocus);
  }
});
```

### Recommended Upgrades
1. **Replace `useCamera()` with `useCameraWithCheck()`** for better error handling
2. **Use `shouldShowPermissionRationale()`** for better permission UX
3. **Handle new error codes** for more robust error handling
4. **Use hardware capability checks** for adaptive UI

## 📊 Browser/Platform Compatibility

| Feature | Android | iOS | Browser | Windows |
|---------|---------|-----|---------|---------|
| `hasAutofocus` | ✅ | ✅ | ❌ | ❌ |
| `hasFrontCamera` | ✅ | ✅ | ❌ | ❌ |
| `hasBackCamera` | ✅ | ✅ | ❌ | ❌ |
| `shouldShowRationale` | ✅ | ❌* | ❌ | ❌ |
| New error codes (9-11) | ✅ | ✅ | ❌ | ❌ |
| iOS-specific error codes (12-13) | ❌ | ✅ | ❌ | ❌ |
| `hasOpticalImageStabilization` | ❌ | ✅ | ❌ | ❌ |
| `supportedFocusModes` | ❌ | ✅ | ❌ | ❌ |
| `useCameraWithCheck()` | ✅ | ✅ | ❌ | ❌ |

*Android-specific feature, returns `undefined` on iOS

---

**Status**: ✅ All JavaScript enhancements implemented
**Compatibility**: ✅ Backward compatible with existing code
**Platform Support**: ✅ Enhanced Android support, graceful degradation on other platforms
