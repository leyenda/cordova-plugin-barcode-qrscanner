/**
 * Example Usage of Enhanced QRScanner JavaScript API
 * 
 * This file demonstrates how to use the new Android capabilities
 * and enhanced error handling in the QRScanner plugin.
 */

// Example 1: Smart Permission Handling
function initializeQRScanner() {
  console.log('🚀 Initializing QR Scanner with enhanced capabilities...');
  
  // Check if we should show permission rationale (Android only)
  QRScanner.shouldShowPermissionRationale(function(shouldShow) {
    console.log('📱 Should show permission rationale:', shouldShow);
    
    if (shouldShow) {
      // Show custom explanation dialog
      showPermissionExplanation(function() {
        requestCameraPermission();
      });
    } else {
      requestCameraPermission();
    }
  });
}

function requestCameraPermission() {
  QRScanner.prepare(function(err, status) {
    if (err) {
      console.error('❌ Permission error:', err.name, '-', err._message);
      handlePermissionError(err);
    } else {
      console.log('✅ Camera permission granted');
      setupCameraInterface(status);
    }
  });
}

// Example 2: Hardware Capability Detection
function setupCameraInterface(initialStatus) {
  console.log('🔧 Setting up camera interface...');
  
  // Get full status with new Android fields
  QRScanner.getStatus(function(status) {
    console.log('📊 Full status:', status);
    
    // Check hardware capabilities
    console.log('🔍 Hardware capabilities:');
    console.log('  - Autofocus:', status.hasAutofocus);
    console.log('  - Front camera:', status.hasFrontCamera);
    console.log('  - Back camera:', status.hasBackCamera);
    console.log('  - Can change camera:', status.canChangeCamera);
    console.log('  - Can enable light:', status.canEnableLight);
    
    // Setup UI based on capabilities
    setupUI(status);
  });
}

function setupUI(status) {
  // Enable camera switch button only if both cameras available
  if (status.canChangeCamera && status.hasFrontCamera && status.hasBackCamera) {
    console.log('📷 Enabling camera switch button');
    enableCameraSwitchButton();
  }
  
  // Show autofocus indicator
  if (status.hasAutofocus) {
    console.log('🎯 Device has autofocus - better scanning expected');
    showAutofocusIndicator(true);
  } else {
    console.log('⚠️ No autofocus - may need better lighting');
    showAutofocusIndicator(false);
  }
  
  // Setup flash button for back camera only
  if (status.currentCamera === 0 && status.canEnableLight) {
    enableFlashButton();
  } else {
    disableFlashButton();
  }
}

// Example 3: Enhanced Camera Switching
function switchCamera(cameraIndex) {
  console.log('🔄 Switching to camera:', cameraIndex === 0 ? 'back' : 'front');
  
  // Use enhanced camera switching with validation
  QRScanner.useCameraWithCheck(cameraIndex, function(err, status) {
    if (err) {
      console.error('❌ Camera switch failed:', err._message);
      showToast('Cannot switch camera: ' + err._message);
    } else {
      console.log('✅ Camera switched successfully');
      updateCameraUI(cameraIndex, status);
    }
  });
}

function updateCameraUI(cameraIndex, status) {
  // Update UI based on current camera
  if (cameraIndex === 1) { // Front camera
    console.log('📱 Switched to front camera');
    // Disable flash for front camera
    if (status.lightEnabled) {
      QRScanner.disableLight(function(err, status) {
        if (!err) {
          console.log('💡 Flash disabled for front camera');
        }
      });
    }
    disableFlashButton();
  } else { // Back camera
    console.log('📷 Switched to back camera');
    // Enable flash button if available
    if (status.canEnableLight) {
      enableFlashButton();
    }
  }
}

// Example 4: Comprehensive Error Handling
function handlePermissionError(err) {
  switch (err.code) {
    case 1: // CAMERA_ACCESS_DENIED
      console.log('📵 Camera access denied');
      // Check if permanently denied
      QRScanner.getStatus(function(status) {
        if (status.denied && !status.shouldShowRationale) {
          showSettingsDialog();
        } else {
          showPermissionDeniedDialog();
        }
      });
      break;
      
    case 2: // CAMERA_ACCESS_RESTRICTED
      console.log('🔒 Camera access restricted');
      showRestrictedDialog();
      break;
      
    case 5: // CAMERA_UNAVAILABLE
      console.log('📷 Camera unavailable');
      showCameraUnavailableDialog();
      break;
      
    case 9: // CAMERA_INITIALIZATION_FAILED (new)
      console.log('⚠️ Camera initialization failed');
      showInitFailedDialog();
      break;
      
    case 11: // LOW_MEMORY_WARNING (new)
      console.log('💾 Low memory warning');
      showLowMemoryDialog();
      break;
      
    default:
      console.log('❓ Unexpected error:', err.name);
      showGenericErrorDialog(err);
  }
}

// Example 5: Performance-Aware Scanning
function startScanning() {
  console.log('🔍 Starting QR code scanning...');
  
  QRScanner.scan(function(err, text) {
    if (err) {
      if (err.code === 11) { // LOW_MEMORY_WARNING
        console.log('⚠️ Performance may be affected due to low memory');
        showLowMemoryWarning();
      } else {
        console.error('❌ Scan error:', err._message);
      }
    } else {
      console.log('✅ QR code scanned:', text);
      handleScannedCode(text);
    }
  });
}

// Example 6: Utility Functions for Hardware Detection
function checkHardwareCapabilities() {
  console.log('🔍 Checking hardware capabilities...');
  
  // Individual capability checks
  QRScanner.hasAutofocus(function(hasAutofocus) {
    console.log('🎯 Autofocus available:', hasAutofocus);
    if (!hasAutofocus) {
      showAutofocusWarning();
    }
  });
  
  QRScanner.hasFrontCamera(function(hasFront) {
    console.log('📱 Front camera available:', hasFront);
    toggleFrontCameraButton(hasFront);
  });
  
  QRScanner.hasBackCamera(function(hasBack) {
    console.log('📷 Back camera available:', hasBack);
    if (!hasBack) {
      showNoBackCameraWarning();
    }
  });
}

// Mock UI functions (replace with your actual UI implementation)
function showPermissionExplanation(callback) {
  console.log('📋 Showing permission explanation dialog');
  // Your UI dialog implementation
  setTimeout(callback, 1000); // Simulate user interaction
}

function enableCameraSwitchButton() {
  console.log('🔄 Camera switch button enabled');
}

function showAutofocusIndicator(hasAutofocus) {
  console.log('🎯 Autofocus indicator:', hasAutofocus ? 'enabled' : 'disabled');
}

function enableFlashButton() {
  console.log('💡 Flash button enabled');
}

function disableFlashButton() {
  console.log('💡 Flash button disabled');
}

function showToast(message) {
  console.log('📱 Toast:', message);
}

function showSettingsDialog() {
  console.log('⚙️ Showing settings dialog');
  // Offer to open device settings
  QRScanner.openSettings();
}

function showLowMemoryWarning() {
  console.log('💾 Showing low memory warning');
}

function showInitFailedDialog() {
  console.log('⚠️ Showing camera init failed dialog');
}

function handleScannedCode(text) {
  console.log('📄 Processing scanned code:', text);
}

// Usage Example:
// Call this when your app starts
document.addEventListener('deviceready', function() {
  console.log('📱 Device ready - initializing QR scanner...');
  
  // Check capabilities first
  checkHardwareCapabilities();
  
  // Initialize scanner
  initializeQRScanner();
}, false);

// Export for testing
if (typeof module !== 'undefined' && module.exports) {
  module.exports = {
    initializeQRScanner,
    switchCamera,
    checkHardwareCapabilities,
    startScanning
  };
}
