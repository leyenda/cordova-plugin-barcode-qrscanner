/**
 * Hybrid Android Scanner - Example Usage
 * 
 * This example demonstrates how to use the new hybrid Android scanner
 * that automatically chooses between ML Kit (primary) and ZXing (fallback)
 */

// ========================================
// BASIC USAGE (No changes needed!)
// ========================================

// The scanner automatically selects the best available option
QRScanner.prepare(function(err, status) {
    if (err) {
        console.error('Scanner preparation failed:', err);
        return;
    }
    
    console.log('Scanner ready!');
    console.log('Using scanner:', status.scannerType);
    
    if (status.authorized) {
        startScanning();
    } else {
        // Handle permission request...
    }
});

function startScanning() {
    QRScanner.scan(function(err, text) {
        if (err) {
            console.error('Scan failed:', err);
        } else {
            console.log('Scanned:', text);
            // Process the scanned code...
        }
    });
    
    QRScanner.show(); // Show camera preview
}

// ========================================
// ENHANCED USAGE - Check Scanner Type
// ========================================

function initializeScannerWithInfo() {
    QRScanner.prepare(function(err, status) {
        if (err) {
            console.error('Scanner preparation failed:', err);
            return;
        }
        
        // Log detailed scanner information
        console.log('=== Scanner Information ===');
        console.log('Scanner Type:', status.scannerType);
        console.log('Using ML Kit:', status.usingMLKit);
        console.log('ML Kit Available:', status.mlKitAvailable);
        
        // Provide user feedback based on scanner type
        if (status.usingMLKit) {
            showUserMessage('🚀 Using high-performance ML Kit scanner');
        } else {
            showUserMessage('📱 Using reliable ZXing scanner');
        }
        
        // Continue with normal scanning...
        if (status.authorized) {
            startAdvancedScanning();
        }
    });
}

// ========================================
// PERFORMANCE MONITORING
// ========================================

function performanceAwareScanning() {
    const scanStartTime = Date.now();
    
    QRScanner.scan(function(err, text) {
        const scanDuration = Date.now() - scanStartTime;
        
        if (err) {
            console.error('Scan failed after', scanDuration + 'ms:', err);
            
            // Check if this was a fallback scenario
            QRScanner.getStatus(function(status) {
                if (!status.usingMLKit && status.mlKitAvailable) {
                    console.log('⚠️ Fallback to ZXing occurred during scan');
                }
            });
        } else {
            console.log('✅ Scan successful in', scanDuration + 'ms:', text);
            
            // Log performance metrics
            QRScanner.getStatus(function(status) {
                console.log('Performance - Scanner:', status.scannerType, 'Duration:', scanDuration + 'ms');
                
                // Expected performance ranges:
                // ML Kit: 50-80ms
                // ZXing: 150-300ms
                if (status.usingMLKit && scanDuration > 100) {
                    console.log('⚠️ ML Kit scan slower than expected');
                } else if (!status.usingMLKit && scanDuration > 400) {
                    console.log('⚠️ ZXing scan slower than expected');
                }
            });
        }
    });
}

// ========================================
// ERROR HANDLING & FALLBACK DETECTION
// ========================================

function robustScanningWithErrorHandling() {
    let scanAttempts = 0;
    const maxAttempts = 3;
    
    function attemptScan() {
        scanAttempts++;
        console.log('Scan attempt:', scanAttempts);
        
        QRScanner.scan(function(err, text) {
            if (err) {
                console.error('Scan error:', err);
                
                // Check scanner status after error
                QRScanner.getStatus(function(status) {
                    console.log('Scanner status after error:', {
                        scannerType: status.scannerType,
                        usingMLKit: status.usingMLKit,
                        mlKitAvailable: status.mlKitAvailable
                    });
                    
                    // Handle specific error types
                    switch (err.code) {
                        case 9: // CAMERA_INITIALIZATION_FAILED
                            if (scanAttempts < maxAttempts) {
                                console.log('Retrying scan after camera initialization failure...');
                                setTimeout(attemptScan, 1000);
                            } else {
                                showUserMessage('Camera initialization failed. Please restart the app.');
                            }
                            break;
                            
                        case 11: // LOW_MEMORY_WARNING
                            console.log('Low memory detected, pausing briefly...');
                            setTimeout(attemptScan, 2000);
                            break;
                            
                        default:
                            if (scanAttempts < maxAttempts) {
                                setTimeout(attemptScan, 500);
                            } else {
                                showUserMessage('Scanning failed. Please try again.');
                            }
                            break;
                    }
                });
            } else {
                console.log('✅ Scan successful:', text);
                processScanResult(text);
            }
        });
    }
    
    attemptScan();
}

// ========================================
// DEVICE CAPABILITY CHECKING
// ========================================

function checkDeviceCapabilities() {
    QRScanner.getStatus(function(status) {
        console.log('=== Device Capabilities ===');
        
        // Scanner capabilities
        console.log('ML Kit Available:', status.mlKitAvailable);
        console.log('Current Scanner:', status.scannerType);
        console.log('Using ML Kit:', status.usingMLKit);
        
        // Hardware capabilities
        console.log('Has Autofocus:', status.hasAutofocus);
        console.log('Has Front Camera:', status.hasFrontCamera);
        console.log('Has Back Camera:', status.hasBackCamera);
        console.log('Can Enable Light:', status.canEnableLight);
        
        // Provide recommendations based on capabilities
        if (status.mlKitAvailable && status.usingMLKit) {
            console.log('✅ Optimal setup: ML Kit with hardware acceleration');
        } else if (status.mlKitAvailable && !status.usingMLKit) {
            console.log('⚠️ ML Kit available but not used - check Google Play Services');
        } else {
            console.log('📱 Using ZXing fallback - reliable but slower performance');
        }
        
        // Hardware-specific optimizations
        if (!status.hasAutofocus) {
            console.log('⚠️ No autofocus - scanning may be slower');
            showUserMessage('Hold device steady for better scanning');
        }
        
        if (!status.canEnableLight) {
            console.log('⚠️ No flash available for low-light scanning');
        }
    });
}

// ========================================
// ADAPTIVE SCANNING STRATEGY
// ========================================

function adaptiveScanningStrategy() {
    QRScanner.getStatus(function(status) {
        let scanTimeout;
        let scanStrategy;
        
        if (status.usingMLKit) {
            // ML Kit: Fast scanning, shorter timeout
            scanTimeout = 5000; // 5 seconds
            scanStrategy = 'fast';
            console.log('Using fast ML Kit scanning strategy');
        } else {
            // ZXing: Slower scanning, longer timeout
            scanTimeout = 10000; // 10 seconds  
            scanStrategy = 'patient';
            console.log('Using patient ZXing scanning strategy');
        }
        
        // Start scanning with appropriate timeout
        const scanTimer = setTimeout(function() {
            console.log('Scan timeout reached for', scanStrategy, 'strategy');
            QRScanner.cancelScan(function() {
                showUserMessage('Scan timeout. Please try again with better lighting.');
            });
        }, scanTimeout);
        
        QRScanner.scan(function(err, text) {
            clearTimeout(scanTimer);
            
            if (err) {
                console.error('Adaptive scan failed:', err);
                handleScanError(err, scanStrategy);
            } else {
                console.log('✅ Adaptive scan successful with', scanStrategy, 'strategy:', text);
                processScanResult(text);
            }
        });
    });
}

// ========================================
// UTILITY FUNCTIONS
// ========================================

function showUserMessage(message) {
    // Your UI notification implementation
    console.log('User Message:', message);
    
    // Example: Show toast, alert, or in-app notification
    if (window.plugins && window.plugins.toast) {
        window.plugins.toast.showShortBottom(message);
    } else {
        alert(message);
    }
}

function processScanResult(text) {
    console.log('Processing scan result:', text);
    
    // Your business logic here
    // e.g., validate QR code, make API calls, navigate to different screen
    
    // Example: Parse different types of QR codes
    if (text.startsWith('http')) {
        console.log('URL detected:', text);
        // Handle URL
    } else if (text.includes('@')) {
        console.log('Email detected:', text);
        // Handle email
    } else {
        console.log('Generic text:', text);
        // Handle generic text
    }
}

function handleScanError(err, strategy) {
    console.error('Scan error with', strategy, 'strategy:', err);
    
    // Strategy-specific error handling
    if (strategy === 'fast' && err.code === 11) {
        // ML Kit low memory - switch to patient mode
        console.log('Switching to patient scanning due to memory constraints');
        setTimeout(function() {
            // Retry with ZXing if possible
            adaptiveScanningStrategy();
        }, 2000);
    } else {
        // Generic error handling
        showUserMessage('Scanning failed. Please try again.');
    }
}

// ========================================
// INITIALIZATION EXAMPLE
// ========================================

// Example initialization when your app starts
document.addEventListener('deviceready', function() {
    console.log('Device ready - initializing hybrid scanner');
    
    // Check capabilities first
    checkDeviceCapabilities();
    
    // Initialize scanner with detailed info
    initializeScannerWithInfo();
    
    // Set up UI event handlers
    document.getElementById('scanButton').addEventListener('click', function() {
        performanceAwareScanning();
    });
    
    document.getElementById('robustScanButton').addEventListener('click', function() {
        robustScanningWithErrorHandling();
    });
    
    document.getElementById('adaptiveScanButton').addEventListener('click', function() {
        adaptiveScanningStrategy();
    });
});

// ========================================
// MIGRATION FROM OLD VERSION
// ========================================

/*
 * MIGRATION NOTES:
 * 
 * ✅ NO BREAKING CHANGES - Your existing code will work unchanged
 * ✅ NEW FEATURES - Access to scanner type and performance info
 * ✅ BETTER PERFORMANCE - Automatic ML Kit usage when available
 * ✅ BETTER RELIABILITY - Automatic fallback to ZXing when needed
 * 
 * OLD CODE (still works):
 * QRScanner.prepare(callback);
 * QRScanner.scan(callback);
 * 
 * NEW ENHANCED CODE (optional):
 * QRScanner.getStatus(function(status) {
 *     console.log('Scanner type:', status.scannerType);
 *     console.log('Using ML Kit:', status.usingMLKit);
 * });
 */
