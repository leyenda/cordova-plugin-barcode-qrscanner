module.exports = function createQRScanner(cordova){
// The native implementations should return their status as ['string':'string']
// dictionaries. Boolean values are encoded to '0' and '1', respectively.
function stringToBool(string) {
  switch (string) {
    case '1':
      return true;
    case '0':
      return false;
    default:
    throw new Error('QRScanner plugin returned an invalid boolean number-string: ' + string);
  }
}

// Converts the returned ['string':'string'] dictionary to a status object.
function convertStatus(statusDictionary) {
  var status = {
    authorized: stringToBool(statusDictionary.authorized),
    denied: stringToBool(statusDictionary.denied),
    restricted: stringToBool(statusDictionary.restricted),
    prepared: stringToBool(statusDictionary.prepared),
    scanning: stringToBool(statusDictionary.scanning),
    previewing: stringToBool(statusDictionary.previewing),
    showing: stringToBool(statusDictionary.showing),
    lightEnabled: stringToBool(statusDictionary.lightEnabled),
    canOpenSettings: stringToBool(statusDictionary.canOpenSettings),
    canEnableLight: stringToBool(statusDictionary.canEnableLight),
    canChangeCamera: stringToBool(statusDictionary.canChangeCamera),
    currentCamera: parseInt(statusDictionary.currentCamera)
  };

  // Add new Android-specific status fields if available
  if (statusDictionary.hasAutofocus !== undefined) {
    status.hasAutofocus = stringToBool(statusDictionary.hasAutofocus);
  }
  if (statusDictionary.hasFrontCamera !== undefined) {
    status.hasFrontCamera = stringToBool(statusDictionary.hasFrontCamera);
  }
  if (statusDictionary.hasBackCamera !== undefined) {
    status.hasBackCamera = stringToBool(statusDictionary.hasBackCamera);
  }
  if (statusDictionary.shouldShowRationale !== undefined) {
    status.shouldShowRationale = stringToBool(statusDictionary.shouldShowRationale);
  }
  
  // Add new iOS-specific status fields if available
  if (statusDictionary.hasOpticalImageStabilization !== undefined) {
    status.hasOpticalImageStabilization = stringToBool(statusDictionary.hasOpticalImageStabilization);
  }
  if (statusDictionary.supportedFocusModes !== undefined) {
    status.supportedFocusModes = parseInt(statusDictionary.supportedFocusModes) || 0;
  }
  
  // Enhanced iOS features
  if (statusDictionary.hapticFeedbackEnabled !== undefined) {
    status.hapticFeedbackEnabled = stringToBool(statusDictionary.hapticFeedbackEnabled);
  }
  if (statusDictionary.performanceMonitoringEnabled !== undefined) {
    status.performanceMonitoringEnabled = stringToBool(statusDictionary.performanceMonitoringEnabled);
  }
  if (statusDictionary.visionFrameworkValidationEnabled !== undefined) {
    status.visionFrameworkValidationEnabled = stringToBool(statusDictionary.visionFrameworkValidationEnabled);
  }
  if (statusDictionary.scanCount !== undefined) {
    status.scanCount = parseInt(statusDictionary.scanCount) || 0;
  }
  if (statusDictionary.lastScanTime !== undefined) {
    status.lastScanTime = parseInt(statusDictionary.lastScanTime) || 0;
  }
  if (statusDictionary.averageScanTime !== undefined) {
    status.averageScanTime = parseInt(statusDictionary.averageScanTime) || 0;
  }
  if (statusDictionary.iosVersion !== undefined) {
    status.iosVersion = statusDictionary.iosVersion;
  }
  if (statusDictionary.deviceModel !== undefined) {
    status.deviceModel = statusDictionary.deviceModel;
  }
  if (statusDictionary.hapticFeedbackAvailable !== undefined) {
    status.hapticFeedbackAvailable = stringToBool(statusDictionary.hapticFeedbackAvailable);
  }
  
  // Add hybrid scanner status fields (Android)
  if (statusDictionary.usingMLKit !== undefined) {
    status.usingMLKit = stringToBool(statusDictionary.usingMLKit);
  }
  if (statusDictionary.scannerType !== undefined) {
    status.scannerType = statusDictionary.scannerType;
  }
  if (statusDictionary.mlKitAvailable !== undefined) {
    status.mlKitAvailable = stringToBool(statusDictionary.mlKitAvailable);
  }

  return status;
}

// Simple utility method to ensure the background is transparent. Used by the
// plugin to force re-rendering immediately after the native webview background
// is made transparent.
function clearBackground() {
  var body = document.body;
  if (body.style) {
    body.style.backgroundColor = 'rgba(0,0,0,0.01)';
    body.style.backgroundImage = '';
    setTimeout(function() {
      body.style.backgroundColor = 'transparent';
    }, 1);
    if (body.parentNode && body.parentNode.style) {
      body.parentNode.style.backgroundColor = 'transparent';
      body.parentNode.style.backgroundImage = '';
    }
  }
}

function errorCallback(callback) {
  if (!callback) {
    return null;
  }
  return function(error) {
    var errorCode = parseInt(error);
    var QRScannerError = {};
    switch (errorCode) {
      case 0:
        QRScannerError = {
          name: 'UNEXPECTED_ERROR',
          code: 0,
          _message: 'QRScanner experienced an unexpected error.'
        };
        break;
      case 1:
        QRScannerError = {
          name: 'CAMERA_ACCESS_DENIED',
          code: 1,
          _message: 'The user denied camera access.'
        };
        break;
      case 2:
        QRScannerError = {
          name: 'CAMERA_ACCESS_RESTRICTED',
          code: 2,
          _message: 'Camera access is restricted.'
        };
        break;
      case 3:
        QRScannerError = {
          name: 'BACK_CAMERA_UNAVAILABLE',
          code: 3,
          _message: 'The back camera is unavailable.'
        };
        break;
      case 4:
        QRScannerError = {
          name: 'FRONT_CAMERA_UNAVAILABLE',
          code: 4,
          _message: 'The front camera is unavailable.'
        };
        break;
      case 5:
        QRScannerError = {
          name: 'CAMERA_UNAVAILABLE',
          code: 5,
          _message: 'The camera is unavailable.'
        };
        break;
      case 6:
        QRScannerError = {
          name: 'SCAN_CANCELED',
          code: 6,
          _message: 'Scan was canceled.'
        };
        break;
      case 7:
        QRScannerError = {
          name: 'LIGHT_UNAVAILABLE',
          code: 7,
          _message: 'The device light is unavailable.'
        };
        break;
      case 8:
        // Open settings is only available on iOS 8.0+.
        QRScannerError = {
          name: 'OPEN_SETTINGS_UNAVAILABLE',
          code: 8,
          _message: 'The device is unable to open settings.'
        };
        break;
      case 9:
        QRScannerError = {
          name: 'CAMERA_INITIALIZATION_FAILED',
          code: 9,
          _message: 'Camera initialization failed. This may be due to low memory or hardware issues.'
        };
        break;
      case 10:
        QRScannerError = {
          name: 'AUTOFOCUS_UNAVAILABLE',
          code: 10,
          _message: 'Autofocus is not available on this device.'
        };
        break;
      case 11:
        QRScannerError = {
          name: 'LOW_MEMORY_WARNING',
          code: 11,
          _message: 'Low memory warning. Camera performance may be affected.'
        };
        break;
      case 12:
        QRScannerError = {
          name: 'SESSION_CONFIGURATION_FAILED',
          code: 12,
          _message: 'Camera session configuration failed. Please try again.'
        };
        break;
      case 13:
        QRScannerError = {
          name: 'DEVICE_NOT_SUPPORTED',
          code: 13,
          _message: 'This device does not support the required camera features.'
        };
        break;
      default:
        QRScannerError = {
          name: 'UNEXPECTED_ERROR',
          code: 0,
          _message: 'QRScanner returned an invalid error code.'
        };
        break;
    }
    callback(QRScannerError);
  };
}

function successCallback(callback) {
  if (!callback) {
    return null;
  }
  return function(statusDict) {
    callback(null, convertStatus(statusDict));
  };
}

function doneCallback(callback, clear) {
  if (!callback) {
    return null;
  }
  return function(statusDict) {
    if (clear) {
      clearBackground();
    }
    callback(convertStatus(statusDict));
  };
}

return {
  prepare: function(callback) {
    cordova.exec(successCallback(callback), errorCallback(callback), 'QRScanner', 'prepare', []);
  },
  destroy: function(callback) {
    cordova.exec(doneCallback(callback, true), null, 'QRScanner', 'destroy', []);
  },
  scan: function(callback) {
    if (!callback) {
      throw new Error('No callback provided to scan method.');
    }
    var success = function(result) {
      callback(null, result);
    };
    cordova.exec(success, errorCallback(callback), 'QRScanner', 'scan', []);
  },
  cancelScan: function(callback) {
    cordova.exec(doneCallback(callback), null, 'QRScanner', 'cancelScan', []);
  },
  show: function(callback) {
    cordova.exec(doneCallback(callback, true), null, 'QRScanner', 'show', []);
  },
  hide: function(callback) {
    cordova.exec(doneCallback(callback, true), null, 'QRScanner', 'hide', []);
  },
  pausePreview: function(callback) {
    cordova.exec(doneCallback(callback), null, 'QRScanner', 'pausePreview', []);
  },
  resumePreview: function(callback) {
    cordova.exec(doneCallback(callback), null, 'QRScanner', 'resumePreview', []);
  },
  enableLight: function(callback) {
    cordova.exec(successCallback(callback), errorCallback(callback), 'QRScanner', 'enableLight', []);
  },
  disableLight: function(callback) {
    cordova.exec(successCallback(callback), errorCallback(callback), 'QRScanner', 'disableLight', []);
  },
  useCamera: function(index, callback) {
    cordova.exec(successCallback(callback), errorCallback(callback), 'QRScanner', 'useCamera', [index]);
  },
  useFrontCamera: function(callback) {
    var frontCamera = 1;
    if (callback) {
      this.useCamera(frontCamera, callback);
    } else {
      cordova.exec(null, null, 'QRScanner', 'useCamera', [frontCamera]);
    }
  },
  useBackCamera: function(callback) {
    var backCamera = 0;
    if (callback) {
      this.useCamera(backCamera, callback);
    } else {
      cordova.exec(null, null, 'QRScanner', 'useCamera', [backCamera]);
    }
  },
  openSettings: function(callback) {
    if (callback) {
      cordova.exec(successCallback(callback), errorCallback(callback), 'QRScanner', 'openSettings', []);
    } else {
      cordova.exec(null, null, 'QRScanner', 'openSettings', []);
    }
  },
  getStatus: function(callback) {
    if (!callback) {
      throw new Error('No callback provided to getStatus method.');
    }
    cordova.exec(doneCallback(callback), null, 'QRScanner', 'getStatus', []);
  },

  // New utility methods for enhanced Android capabilities
  
  /**
   * Check if the device has hardware autofocus capability
   * @param {Function} callback - Callback function with status object
   */
  hasAutofocus: function(callback) {
    this.getStatus(function(status) {
      if (callback) {
        callback(status.hasAutofocus === true);
      }
    });
  },

  /**
   * Check if the device has a front camera
   * @param {Function} callback - Callback function with boolean result
   */
  hasFrontCamera: function(callback) {
    this.getStatus(function(status) {
      if (callback) {
        callback(status.hasFrontCamera === true);
      }
    });
  },

  /**
   * Check if the device has a back camera
   * @param {Function} callback - Callback function with boolean result
   */
  hasBackCamera: function(callback) {
    this.getStatus(function(status) {
      if (callback) {
        callback(status.hasBackCamera === true);
      }
    });
  },

  /**
   * Check if we should show permission rationale to the user
   * This is useful for Android to determine if we should explain why we need camera permission
   * @param {Function} callback - Callback function with boolean result
   */
  shouldShowPermissionRationale: function(callback) {
    this.getStatus(function(status) {
      if (callback) {
        callback(status.shouldShowRationale === true);
      }
    });
  },

      /**
       * Enhanced camera switching with capability checking
       * @param {number} cameraIndex - Camera index (0 = back, 1 = front)
       * @param {Function} callback - Callback function
       */
      useCameraWithCheck: function(cameraIndex, callback) {
        var self = this;
        this.getStatus(function(status) {
          var canSwitch = false;
          var errorMessage = null;

          if (cameraIndex === 0 && status.hasBackCamera) {
            canSwitch = true;
          } else if (cameraIndex === 1 && status.hasFrontCamera) {
            canSwitch = true;
          } else if (cameraIndex === 0) {
            errorMessage = 'Back camera is not available on this device';
          } else if (cameraIndex === 1) {
            errorMessage = 'Front camera is not available on this device';
          } else {
            errorMessage = 'Invalid camera index: ' + cameraIndex;
          }

          if (canSwitch) {
            self.useCamera(cameraIndex, callback);
          } else if (callback) {
            callback({
              name: 'CAMERA_UNAVAILABLE',
              code: 5,
              _message: errorMessage
            });
          }
        });
      },

      // ========================================
      // Enhanced iOS Features
      // ========================================

      /**
       * Configure haptic feedback (iOS only)
       * @param {boolean} enabled - Enable or disable haptic feedback
       * @param {Function} callback - Callback function with status
       */
      configureHapticFeedback: function(enabled, callback) {
        if (typeof enabled !== 'boolean') {
          if (callback) {
            callback({
              name: 'INVALID_ARGUMENT',
              code: 0,
              _message: 'Haptic feedback setting must be a boolean'
            });
          }
          return;
        }
        
        cordova.exec(callback, callback, 'QRScanner', 'configureHapticFeedback', [enabled]);
      },

      /**
       * Configure performance monitoring (iOS only)
       * @param {boolean} enabled - Enable or disable performance monitoring
       * @param {Function} callback - Callback function with status
       */
      configurePerformanceMonitoring: function(enabled, callback) {
        if (typeof enabled !== 'boolean') {
          if (callback) {
            callback({
              name: 'INVALID_ARGUMENT',
              code: 0,
              _message: 'Performance monitoring setting must be a boolean'
            });
          }
          return;
        }
        
        cordova.exec(callback, callback, 'QRScanner', 'configurePerformanceMonitoring', [enabled]);
      },

      /**
       * Configure Vision Framework validation (iOS 11+ only)
       * @param {boolean} enabled - Enable or disable Vision Framework validation
       * @param {Function} callback - Callback function with status
       */
      configureVisionFrameworkValidation: function(enabled, callback) {
        if (typeof enabled !== 'boolean') {
          if (callback) {
            callback({
              name: 'INVALID_ARGUMENT',
              code: 0,
              _message: 'Vision Framework validation setting must be a boolean'
            });
          }
          return;
        }
        
        cordova.exec(callback, callback, 'QRScanner', 'configureVisionFrameworkValidation', [enabled]);
      },

      /**
       * Get performance metrics (iOS only)
       * @param {Function} callback - Callback function with metrics object
       */
      getPerformanceMetrics: function(callback) {
        cordova.exec(callback, callback, 'QRScanner', 'getPerformanceMetrics', []);
      },

      /**
       * Check if haptic feedback is available (iOS only)
       * @param {Function} callback - Callback function with boolean result
       */
      isHapticFeedbackAvailable: function(callback) {
        this.getStatus(function(status) {
          if (callback) {
            callback(status.hapticFeedbackAvailable === true);
          }
        });
      },

      /**
       * Get iOS device information
       * @param {Function} callback - Callback function with device info
       */
      getDeviceInfo: function(callback) {
        this.getStatus(function(status) {
          if (callback) {
            callback({
              iosVersion: status.iosVersion,
              deviceModel: status.deviceModel,
              hapticFeedbackAvailable: status.hapticFeedbackAvailable === true,
              hasOpticalImageStabilization: status.hasOpticalImageStabilization === true,
              supportedFocusModes: status.supportedFocusModes || 0
            });
          }
        });
      }
};
};
