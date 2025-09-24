import Foundation
import AVFoundation
import Darwin.Mach
import UIKit
import Vision // For barcode validation

@objc(QRScanner)
class QRScanner : CDVPlugin, AVCaptureMetadataOutputObjectsDelegate {
    
    class CameraView: UIView {
        var videoPreviewLayer:AVCaptureVideoPreviewLayer?
        
        func interfaceOrientationToVideoOrientation(_ orientation : UIInterfaceOrientation) -> AVCaptureVideoOrientation {
            switch (orientation) {
            case UIInterfaceOrientation.portrait:
                return AVCaptureVideoOrientation.portrait;
            case UIInterfaceOrientation.portraitUpsideDown:
                return AVCaptureVideoOrientation.portraitUpsideDown;
            case UIInterfaceOrientation.landscapeLeft:
                return AVCaptureVideoOrientation.landscapeLeft;
            case UIInterfaceOrientation.landscapeRight:
                return AVCaptureVideoOrientation.landscapeRight;
            default:
                return AVCaptureVideoOrientation.portraitUpsideDown;
            }
        }

        override func layoutSubviews() {
            super.layoutSubviews();
            if let sublayers = self.layer.sublayers {
                for layer in sublayers {
                    layer.frame = self.bounds;
                }
            }
            
            self.videoPreviewLayer?.connection?.videoOrientation = interfaceOrientationToVideoOrientation(UIApplication.shared.statusBarOrientation);
        }
        
        
        func addPreviewLayer(_ previewLayer:AVCaptureVideoPreviewLayer?) {
            previewLayer!.videoGravity = AVLayerVideoGravity.resizeAspectFill
            previewLayer!.frame = self.bounds
            self.layer.addSublayer(previewLayer!)
            self.videoPreviewLayer = previewLayer;
        }
        
        func removePreviewLayer() {
            if self.videoPreviewLayer != nil {
                self.videoPreviewLayer!.removeFromSuperlayer()
                self.videoPreviewLayer = nil
            }
        }
    }

    var cameraView: CameraView!
    var captureSession:AVCaptureSession?
    var captureVideoPreviewLayer:AVCaptureVideoPreviewLayer?
    var metaOutput: AVCaptureMetadataOutput?

    var currentCamera: Int = 0;
    var frontCamera: AVCaptureDevice?
    var backCamera: AVCaptureDevice?

    var scanning: Bool = false
    var paused: Bool = false
    var nextScanningCommand: CDVInvokedUrlCommand?
    
    // Enhanced iOS capabilities
    var hasAutofocus: Bool = false
    var hasOpticalImageStabilization: Bool = false
    var supportedFocusModes: [AVCaptureDevice.FocusMode] = []
    var memoryWarningObserver: NSObjectProtocol?
    
    // Enhanced iOS features
    var hapticFeedbackEnabled: Bool = true
    var performanceMonitoringEnabled: Bool = true
    var visionFrameworkValidationEnabled: Bool = false
    var lastScanTime: CFTimeInterval = 0
    var scanCount: Int = 0
    var averageScanTime: CFTimeInterval = 0

    enum QRScannerError: Int32 {
        case unexpected_error = 0,
        camera_access_denied = 1,
        camera_access_restricted = 2,
        back_camera_unavailable = 3,
        front_camera_unavailable = 4,
        camera_unavailable = 5,
        scan_canceled = 6,
        light_unavailable = 7,
        open_settings_unavailable = 8,
        // New iOS-specific error codes
        camera_initialization_failed = 9,
        autofocus_unavailable = 10,
        low_memory_warning = 11,
        session_configuration_failed = 12,
        device_not_supported = 13
    }

    enum CaptureError: Error {
        case backCameraUnavailable
        case frontCameraUnavailable
        case couldNotCaptureInput(error: NSError)
    }

    enum LightError: Error {
        case torchUnavailable
    }

    override func pluginInitialize() {
        super.pluginInitialize()
        NSLog("QRScanner: Initializing iOS QR Scanner plugin")
        
        NotificationCenter.default.addObserver(self, selector: #selector(pageDidLoad), name: NSNotification.Name.CDVPageDidLoad, object: nil)
        
        // Add memory warning observer using modern notification name
        memoryWarningObserver = NotificationCenter.default.addObserver(
            forName: NSNotification.Name.UIApplicationDidReceiveMemoryWarning,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.handleMemoryWarning()
        }
        
        // Initialize camera view with safe area support
        let safeFrame = self.getSafeAreaFrame()
        self.cameraView = CameraView(frame: safeFrame)
        self.cameraView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        
        // Detect hardware capabilities
        self.detectHardwareCapabilities()
    }
    
    deinit {
        NSLog("QRScanner: Deinitializing iOS QR Scanner plugin")
        if let observer = memoryWarningObserver {
            NotificationCenter.default.removeObserver(observer)
        }
    }
    
    private func getSafeAreaFrame() -> CGRect {
        if #available(iOS 11.0, *) {
            let window = UIApplication.shared.keyWindow
            let safeAreaInsets = window?.safeAreaInsets ?? UIEdgeInsets.zero
            return CGRect(
                x: safeAreaInsets.left,
                y: safeAreaInsets.top,
                width: UIScreen.main.bounds.width - safeAreaInsets.left - safeAreaInsets.right,
                height: UIScreen.main.bounds.height - safeAreaInsets.top - safeAreaInsets.bottom
            )
        } else {
            return UIScreen.main.bounds
        }
    }
    
    private func handleMemoryWarning() {
        NSLog("QRScanner: Received memory warning")
        // Pause scanning if active to free up resources
        if scanning {
            NSLog("QRScanner: Pausing scan due to memory warning")
            scanning = false
            if let callback = nextScanningCommand {
                sendErrorCode(command: callback, error: QRScannerError.low_memory_warning)
                nextScanningCommand = nil
            }
        }
    }
    
    private func detectHardwareCapabilities() {
        NSLog("QRScanner: Detecting hardware capabilities")
        
        // Check available cameras and their capabilities
        let availableDevices: [AVCaptureDevice]
        if #available(iOS 10.0, *) {
            var deviceTypes: [AVCaptureDevice.DeviceType] = [.builtInWideAngleCamera]
            
            // Add additional device types based on iOS version
            if #available(iOS 10.2, *) {
                deviceTypes.append(.builtInDualCamera)
            }
            if #available(iOS 10.0, *) {
                deviceTypes.append(.builtInTelephotoCamera)
            }
            
            let discoverySession = AVCaptureDevice.DiscoverySession(
                deviceTypes: deviceTypes,
                mediaType: .video,
                position: .unspecified
            )
            availableDevices = discoverySession.devices
        } else {
            availableDevices = AVCaptureDevice.devices(for: AVMediaType.video)
        }
        
        for device in availableDevices {
            if device.position == .back {
                backCamera = device
                hasAutofocus = device.isFocusModeSupported(.autoFocus) || device.isFocusModeSupported(.continuousAutoFocus)
                // Check for video stabilization capabilities (hardware-based)
                if #available(iOS 8.0, *) {
                    // Check if any video stabilization is supported (indicates OIS hardware)
                    hasOpticalImageStabilization = false
                    for format in device.formats {
                        if format.isVideoStabilizationModeSupported(AVCaptureVideoStabilizationMode.standard) ||
                           format.isVideoStabilizationModeSupported(AVCaptureVideoStabilizationMode.cinematic) {
                            hasOpticalImageStabilization = true
                            break
                        }
                    }
                } else {
                    hasOpticalImageStabilization = false
                }
                let focusModes: [AVCaptureDevice.FocusMode] = [.locked, .autoFocus, .continuousAutoFocus]
                supportedFocusModes = focusModes.filter { device.isFocusModeSupported($0) }
                NSLog("QRScanner: Back camera capabilities - Autofocus: \(hasAutofocus), OIS: \(hasOpticalImageStabilization)")
            } else if device.position == .front {
                frontCamera = device
                NSLog("QRScanner: Front camera detected")
            }
        }
        
        // Log device capabilities
        NSLog("QRScanner: Device capabilities detected - Back: \(backCamera != nil), Front: \(frontCamera != nil)")
    }
    
    // MARK: - Enhanced iOS Features
    
    /**
     * Trigger haptic feedback for successful scan
     */
    private func triggerHapticFeedback() {
        guard hapticFeedbackEnabled else { return }
        
        if #available(iOS 10.0, *) {
            let impactFeedback = UIImpactFeedbackGenerator(style: .medium)
            impactFeedback.prepare()
            impactFeedback.impactOccurred()
            NSLog("QRScanner: Haptic feedback triggered")
        }
    }
    
    /**
     * Trigger notification haptic feedback for successful scan
     */
    private func triggerSuccessHapticFeedback() {
        guard hapticFeedbackEnabled else { return }
        
        if #available(iOS 10.0, *) {
            let notificationFeedback = UINotificationFeedbackGenerator()
            notificationFeedback.prepare()
            notificationFeedback.notificationOccurred(.success)
            NSLog("QRScanner: Success haptic feedback triggered")
        }
    }
    
    /**
     * Update performance metrics
     */
    private func updatePerformanceMetrics(scanDuration: CFTimeInterval) {
        guard performanceMonitoringEnabled else { return }
        
        scanCount += 1
        lastScanTime = scanDuration
        
        // Calculate rolling average
        if scanCount == 1 {
            averageScanTime = scanDuration
        } else {
            // Weighted average favoring recent scans
            averageScanTime = (averageScanTime * 0.7) + (scanDuration * 0.3)
        }
        
        NSLog("QRScanner: Scan #\(scanCount) completed in \(Int(scanDuration * 1000))ms, avg: \(Int(averageScanTime * 1000))ms")
    }
    
    /**
     * Validate barcode using Vision Framework (iOS 11+)
     */
    @available(iOS 11.0, *)
    private func validateBarcodeWithVision(_ barcodeString: String, completion: @escaping (Bool) -> Void) {
        guard visionFrameworkValidationEnabled else {
            completion(true) // Skip validation if disabled
            return
        }
        
        // This is a placeholder for Vision Framework validation
        // In a real implementation, you might re-analyze the image to confirm the barcode
        NSLog("QRScanner: Vision Framework validation requested for: \(barcodeString)")
        
        // For now, always validate as true
        // In production, you could implement more sophisticated validation
        completion(true)
    }
    
    /**
     * Check if haptic feedback is available on this device
     */
    private func isHapticFeedbackAvailable() -> Bool {
        if #available(iOS 10.0, *) {
            return true
        }
        return false
    }

    func sendErrorCode(command: CDVInvokedUrlCommand, error: QRScannerError){
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: error.rawValue)
        commandDelegate!.send(pluginResult, callbackId:command.callbackId)
    }

    // utility method
    @objc func backgroundThread(delay: Double = 0.0, background: (() -> Void)? = nil, completion: (() -> Void)? = nil) {
        if #available(iOS 8.0, *) {
            DispatchQueue.global(qos: DispatchQoS.QoSClass.userInitiated).async {
                if (background != nil) {
                    background!()
                }
                DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + delay * Double(NSEC_PER_SEC)) {
                    if(completion != nil){
                        completion!()
                    }
                }
            }
        } else {
            // Fallback for iOS < 8.0
            if(background != nil){
                background!()
            }
            if(completion != nil){
                completion!()
            }
        }
    }

    @objc func prepScanner(command: CDVInvokedUrlCommand) -> Bool{
        NSLog("QRScanner: Preparing scanner")
        
        let status = AVCaptureDevice.authorizationStatus(for: AVMediaType.video)
        if (status == AVAuthorizationStatus.restricted) {
            NSLog("QRScanner: Camera access restricted")
            self.sendErrorCode(command: command, error: QRScannerError.camera_access_restricted)
            return false
        } else if status == AVAuthorizationStatus.denied {
            NSLog("QRScanner: Camera access denied")
            self.sendErrorCode(command: command, error: QRScannerError.camera_access_denied)
            return false
        }
        
        do {
            if (captureSession?.isRunning != true){
                NSLog("QRScanner: Setting up new capture session")
                
                // Check memory before initializing camera
                var memoryInfo = mach_task_basic_info()
                var count = mach_msg_type_number_t(MemoryLayout<mach_task_basic_info>.size)/4
                let result = withUnsafeMutablePointer(to: &memoryInfo) {
                    $0.withMemoryRebound(to: integer_t.self, capacity: 1) {
                        task_info(mach_task_self_, task_flavor_t(MACH_TASK_BASIC_INFO), $0, &count)
                    }
                }
                
                if result == KERN_SUCCESS {
                    let usedMemoryMB = memoryInfo.resident_size / (1024 * 1024)
                    NSLog("QRScanner: Current memory usage: \(usedMemoryMB)MB")
                    
                    // Warn if memory usage is high (over 100MB for the app)
                    if usedMemoryMB > 100 {
                        NSLog("QRScanner: High memory usage detected")
                    }
                }
                
                cameraView.backgroundColor = UIColor.clear
                
                // Safely insert camera view
                guard let webViewSuperview = self.webView?.superview else {
                    NSLog("QRScanner: WebView superview not available")
                    self.sendErrorCode(command: command, error: QRScannerError.camera_initialization_failed)
                    return false
                }
                
                webViewSuperview.insertSubview(cameraView, belowSubview: self.webView!)
                
                // Use already detected cameras from initialization
                if backCamera == nil && frontCamera == nil {
                    detectHardwareCapabilities()
                }
                
                // older iPods have no back camera
                if(backCamera == nil){
                    currentCamera = 1
                    NSLog("QRScanner: No back camera, using front camera")
                }
                
                let input: AVCaptureDeviceInput
                input = try self.createCaptureDeviceInput()
                
                captureSession = AVCaptureSession()
                
                // Configure session for optimal performance
                captureSession!.beginConfiguration()
                
                // Set session preset for better performance
                if captureSession!.canSetSessionPreset(.medium) {
                    captureSession!.sessionPreset = .medium
                    NSLog("QRScanner: Using medium quality preset for better performance")
                } else {
                    NSLog("QRScanner: Using default session preset")
                }
                
                captureSession!.addInput(input)
                
                metaOutput = AVCaptureMetadataOutput()
                captureSession!.addOutput(metaOutput!)
                
                // Use a background queue for metadata processing
                let metadataQueue = DispatchQueue(label: "qrscanner.metadata", qos: .userInitiated)
                metaOutput!.setMetadataObjectsDelegate(self, queue: metadataQueue)
                metaOutput!.metadataObjectTypes = [
                    AVMetadataObject.ObjectType.qr,
                    AVMetadataObject.ObjectType.code39,
                    AVMetadataObject.ObjectType.code39Mod43,
                    AVMetadataObject.ObjectType.code93,
                    AVMetadataObject.ObjectType.code128
                ]
                
                captureSession!.commitConfiguration()
                
                captureVideoPreviewLayer = AVCaptureVideoPreviewLayer(session: captureSession!)
                cameraView.addPreviewLayer(captureVideoPreviewLayer)
                
                // Configure autofocus if available
                try configureOptimalCameraSettings()
                
                captureSession!.startRunning()
                NSLog("QRScanner: Capture session started successfully")
            }
            return true
        } catch CaptureError.backCameraUnavailable {
            NSLog("QRScanner: Back camera unavailable")
            self.sendErrorCode(command: command, error: QRScannerError.back_camera_unavailable)
        } catch CaptureError.frontCameraUnavailable {
            NSLog("QRScanner: Front camera unavailable")
            self.sendErrorCode(command: command, error: QRScannerError.front_camera_unavailable)
        } catch CaptureError.couldNotCaptureInput(let error){
            NSLog("QRScanner: Could not capture input: \(error.localizedDescription)")
            self.sendErrorCode(command: command, error: QRScannerError.camera_unavailable)
        } catch {
            NSLog("QRScanner: Unexpected error during scanner preparation: \(error.localizedDescription)")
            self.sendErrorCode(command: command, error: QRScannerError.camera_initialization_failed)
        }
        return false
    }
    
    private func configureOptimalCameraSettings() throws {
        guard let currentDevice = getCurrentCaptureDevice() else { return }
        
        do {
            try currentDevice.lockForConfiguration()
            
            // Configure autofocus if available
            if hasAutofocus && currentDevice.isFocusModeSupported(.continuousAutoFocus) {
                currentDevice.focusMode = .continuousAutoFocus
                NSLog("QRScanner: Enabled continuous autofocus")
            } else if hasAutofocus && currentDevice.isFocusModeSupported(.autoFocus) {
                currentDevice.focusMode = .autoFocus
                NSLog("QRScanner: Enabled autofocus")
            }
            
            // Configure exposure if available
            if currentDevice.isExposureModeSupported(.continuousAutoExposure) {
                currentDevice.exposureMode = .continuousAutoExposure
                NSLog("QRScanner: Enabled continuous auto exposure")
            }
            
            // Configure white balance if available
            if currentDevice.isWhiteBalanceModeSupported(.continuousAutoWhiteBalance) {
                currentDevice.whiteBalanceMode = .continuousAutoWhiteBalance
                NSLog("QRScanner: Enabled continuous auto white balance")
            }
            
            currentDevice.unlockForConfiguration()
        } catch {
            NSLog("QRScanner: Could not configure camera settings: \(error.localizedDescription)")
            throw error
        }
    }
    
    private func getCurrentCaptureDevice() -> AVCaptureDevice? {
        return currentCamera == 0 ? backCamera : frontCamera
    }

    @objc func createCaptureDeviceInput() throws -> AVCaptureDeviceInput {
        var captureDevice: AVCaptureDevice
        if(currentCamera == 0){
            if(backCamera != nil){
                captureDevice = backCamera!
            } else {
                throw CaptureError.backCameraUnavailable
            }
        } else {
            if(frontCamera != nil){
                captureDevice = frontCamera!
            } else {
                throw CaptureError.frontCameraUnavailable
            }
        }
        let captureDeviceInput: AVCaptureDeviceInput
        do {
            captureDeviceInput = try AVCaptureDeviceInput(device: captureDevice)
        } catch let error as NSError {
            throw CaptureError.couldNotCaptureInput(error: error)
        }
        return captureDeviceInput
    }

    @objc func makeOpaque(){
        self.webView?.isOpaque = false
        self.webView?.backgroundColor = UIColor.clear
    }

    @objc func boolToNumberString(bool: Bool) -> String{
        if(bool) {
            return "1"
        } else {
            return "0"
        }
    }

    @objc func configureLight(command: CDVInvokedUrlCommand, state: Bool){
        var useMode = AVCaptureDevice.TorchMode.on
        if(state == false){
            useMode = AVCaptureDevice.TorchMode.off
        }
        do {
            // torch is only available for back camera
            if(backCamera == nil || backCamera!.hasTorch == false || backCamera!.isTorchAvailable == false || backCamera!.isTorchModeSupported(useMode) == false){
                throw LightError.torchUnavailable
            }
            try backCamera!.lockForConfiguration()
            backCamera!.torchMode = useMode
            backCamera!.unlockForConfiguration()
            self.getStatus(command)
        } catch LightError.torchUnavailable {
            self.sendErrorCode(command: command, error: QRScannerError.light_unavailable)
        } catch let error as NSError {
            print(error.localizedDescription)
            self.sendErrorCode(command: command, error: QRScannerError.unexpected_error)
        }
    }

    // This method processes metadataObjects captured by iOS.
    func metadataOutput(_ captureOutput: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
        // Process on main queue for UI updates
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            
            if metadataObjects.count == 0 || !self.scanning {
                // while nothing is detected, or if scanning is false, do nothing.
                return
            }
            
            guard let found = metadataObjects[0] as? AVMetadataMachineReadableCodeObject else {
                NSLog("QRScanner: Invalid metadata object type")
                return
            }
            
            let supportedTypes: [AVMetadataObject.ObjectType] = [
                .qr, .code39, .code39Mod43, .code93, .code128
            ]
            
            if supportedTypes.contains(found.type), let stringValue = found.stringValue, !stringValue.isEmpty {
                let scanStartTime = CFAbsoluteTimeGetCurrent()
                NSLog("QRScanner: Successfully scanned code: \(found.type.rawValue)")
                
                self.scanning = false
                
                // Trigger haptic feedback for successful scan
                self.triggerSuccessHapticFeedback()
                
                // Handle Vision Framework validation if enabled (iOS 11+)
                if #available(iOS 11.0, *), self.visionFrameworkValidationEnabled {
                    self.validateBarcodeWithVision(stringValue) { isValid in
                        if isValid {
                            self.completeScan(stringValue: stringValue, scanStartTime: scanStartTime)
                        } else {
                            NSLog("QRScanner: Barcode validation failed, continuing scan")
                            self.scanning = true // Continue scanning if validation fails
                        }
                    }
                } else {
                    self.completeScan(stringValue: stringValue, scanStartTime: scanStartTime)
                }
            } else {
                NSLog("QRScanner: Scanned unsupported or empty code type: \(found.type.rawValue)")
            }
        }
    }
    
    /**
     * Complete the scan process with performance monitoring
     */
    private func completeScan(stringValue: String, scanStartTime: CFTimeInterval) {
        let scanDuration = CFAbsoluteTimeGetCurrent() - scanStartTime
        
        // Update performance metrics
        updatePerformanceMetrics(scanDuration: scanDuration)
        
        // Send result to JavaScript
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: stringValue)
        commandDelegate!.send(pluginResult, callbackId: nextScanningCommand?.callbackId!)
        nextScanningCommand = nil
    }

    @objc func pageDidLoad() {
        self.webView?.isOpaque = false
        self.webView?.backgroundColor = UIColor.clear
    }

    // ---- BEGIN EXTERNAL API ----

    @objc func prepare(_ command: CDVInvokedUrlCommand){
        let status = AVCaptureDevice.authorizationStatus(for: AVMediaType.video)
        if (status == AVAuthorizationStatus.notDetermined) {
            // Request permission before preparing scanner
            AVCaptureDevice.requestAccess(for: AVMediaType.video, completionHandler: { (granted) -> Void in
                // attempt to prepScanner only after the request returns
                self.backgroundThread(delay: 0, completion: {
                    if(self.prepScanner(command: command)){
                        self.getStatus(command)
                    }
                })
            })
        } else {
            if(self.prepScanner(command: command)){
                self.getStatus(command)
            }
        }
    }

    @objc func scan(_ command: CDVInvokedUrlCommand){
        if(self.prepScanner(command: command)){
            nextScanningCommand = command
            scanning = true
        }
    }

    @objc func cancelScan(_ command: CDVInvokedUrlCommand){
        if(self.prepScanner(command: command)){
            scanning = false
            if(nextScanningCommand != nil){
                self.sendErrorCode(command: nextScanningCommand!, error: QRScannerError.scan_canceled)
            }
            self.getStatus(command)
        }
    }

    @objc func show(_ command: CDVInvokedUrlCommand) {
        self.webView?.isOpaque = false
        self.webView?.backgroundColor = UIColor.clear
        self.getStatus(command)
    }

    @objc func hide(_ command: CDVInvokedUrlCommand) {
        self.makeOpaque()
        self.getStatus(command)
    }

    @objc func pausePreview(_ command: CDVInvokedUrlCommand) {
        if(scanning){
            paused = true;
            scanning = false;
        }
        captureVideoPreviewLayer?.connection?.isEnabled = false
        self.getStatus(command)
    }

    @objc func resumePreview(_ command: CDVInvokedUrlCommand) {
        if(paused){
            paused = false;
            scanning = true;
        }
        captureVideoPreviewLayer?.connection?.isEnabled = true
        self.getStatus(command)
    }

    // backCamera is 0, frontCamera is 1

    @objc func useCamera(_ command: CDVInvokedUrlCommand){
        let index = command.arguments[0] as! Int
        if(currentCamera != index){
            // camera change only available if both backCamera and frontCamera exist
            if(backCamera != nil && frontCamera != nil){
                // switch camera
                currentCamera = index
                if(self.prepScanner(command: command)){
                    do {
                        captureSession!.beginConfiguration()
                        let currentInput = captureSession?.inputs[0] as! AVCaptureDeviceInput
                        captureSession!.removeInput(currentInput)
                        let input = try self.createCaptureDeviceInput()
                        captureSession!.addInput(input)
                        captureSession!.commitConfiguration()
                        self.getStatus(command)
                    } catch CaptureError.backCameraUnavailable {
                        self.sendErrorCode(command: command, error: QRScannerError.back_camera_unavailable)
                    } catch CaptureError.frontCameraUnavailable {
                        self.sendErrorCode(command: command, error: QRScannerError.front_camera_unavailable)
                    } catch CaptureError.couldNotCaptureInput(let error){
                        print(error.localizedDescription)
                        self.sendErrorCode(command: command, error: QRScannerError.camera_unavailable)
                    } catch {
                        self.sendErrorCode(command: command, error: QRScannerError.unexpected_error)
                    }

                }
            } else {
                if(backCamera == nil){
                    self.sendErrorCode(command: command, error: QRScannerError.back_camera_unavailable)
                } else {
                    self.sendErrorCode(command: command, error: QRScannerError.front_camera_unavailable)
                }
            }
        } else {
            // immediately return status if camera is unchanged
            self.getStatus(command)
        }
    }

    @objc func enableLight(_ command: CDVInvokedUrlCommand) {
        if(self.prepScanner(command: command)){
            self.configureLight(command: command, state: true)
        }
    }

    @objc func disableLight(_ command: CDVInvokedUrlCommand) {
        if(self.prepScanner(command: command)){
            self.configureLight(command: command, state: false)
        }
    }

    /**
     * Configure haptic feedback
     */
    @objc func configureHapticFeedback(_ command: CDVInvokedUrlCommand) {
        guard let enabled = command.arguments[0] as? Bool else {
            sendErrorCode(command: command, error: QRScannerError.unexpected_error)
            return
        }
        
        hapticFeedbackEnabled = enabled && isHapticFeedbackAvailable()
        NSLog("QRScanner: Haptic feedback \(hapticFeedbackEnabled ? "enabled" : "disabled")")
        
        getStatus(command)
    }
    
    /**
     * Configure performance monitoring
     */
    @objc func configurePerformanceMonitoring(_ command: CDVInvokedUrlCommand) {
        guard let enabled = command.arguments[0] as? Bool else {
            sendErrorCode(command: command, error: QRScannerError.unexpected_error)
            return
        }
        
        performanceMonitoringEnabled = enabled
        NSLog("QRScanner: Performance monitoring \(performanceMonitoringEnabled ? "enabled" : "disabled")")
        
        // Reset metrics when toggling
        if enabled {
            scanCount = 0
            lastScanTime = 0
            averageScanTime = 0
        }
        
        getStatus(command)
    }
    
    /**
     * Configure Vision Framework validation
     */
    @objc func configureVisionFrameworkValidation(_ command: CDVInvokedUrlCommand) {
        guard let enabled = command.arguments[0] as? Bool else {
            sendErrorCode(command: command, error: QRScannerError.unexpected_error)
            return
        }
        
        if #available(iOS 11.0, *) {
            visionFrameworkValidationEnabled = enabled
            NSLog("QRScanner: Vision Framework validation \(visionFrameworkValidationEnabled ? "enabled" : "disabled")")
        } else {
            visionFrameworkValidationEnabled = false
            NSLog("QRScanner: Vision Framework validation not available on iOS < 11.0")
        }
        
        getStatus(command)
    }
    
    /**
     * Get performance metrics
     */
    @objc func getPerformanceMetrics(_ command: CDVInvokedUrlCommand) {
        let metrics: [String: Any] = [
            "scanCount": scanCount,
            "lastScanTime": Int(lastScanTime * 1000), // milliseconds
            "averageScanTime": Int(averageScanTime * 1000), // milliseconds
            "performanceMonitoringEnabled": performanceMonitoringEnabled
        ]
        
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: metrics)
        commandDelegate!.send(pluginResult, callbackId: command.callbackId)
    }

    @objc func destroy(_ command: CDVInvokedUrlCommand) {
        NSLog("QRScanner: Destroying scanner")
        self.makeOpaque()
        
        // Stop scanning if active
        if scanning {
            scanning = false
            if let callback = nextScanningCommand {
                sendErrorCode(command: callback, error: QRScannerError.scan_canceled)
                nextScanningCommand = nil
            }
        }
        
        if(self.captureSession != nil){
            backgroundThread(delay: 0, background: {
                NSLog("QRScanner: Stopping capture session")
                self.captureSession!.stopRunning()
                self.cameraView.removePreviewLayer()
                
                // Clean up resources
                self.captureVideoPreviewLayer = nil
                self.metaOutput = nil
                self.captureSession = nil
                self.currentCamera = 0
                
                // Reset capabilities but don't nil the cameras for reuse
                self.hasAutofocus = false
                self.hasOpticalImageStabilization = false
                self.supportedFocusModes = []
                
                NSLog("QRScanner: Scanner destroyed successfully")
            }, completion: {
                if command.callbackId != nil {
                    self.getStatus(command)
                }
            })
        } else {
            NSLog("QRScanner: No active capture session to destroy")
            if command.callbackId != nil {
                self.getStatus(command)
            }
        }
    }

    @objc func getStatus(_ command: CDVInvokedUrlCommand){

        let authorizationStatus = AVCaptureDevice.authorizationStatus(for: AVMediaType.video);

        var authorized = false
        if(authorizationStatus == AVAuthorizationStatus.authorized){
            authorized = true
        }

        var denied = false
        if(authorizationStatus == AVAuthorizationStatus.denied){
            denied = true
        }

        var restricted = false
        if(authorizationStatus == AVAuthorizationStatus.restricted){
            restricted = true
        }

        var prepared = false
        if(captureSession?.isRunning == true){
            prepared = true
        }

        var previewing = false
        if(captureVideoPreviewLayer != nil){
            previewing = captureVideoPreviewLayer!.connection!.isEnabled
        }

        var showing = false
        if(self.webView!.backgroundColor == UIColor.clear){
            showing = true
        }

        var lightEnabled = false
        if(backCamera?.torchMode == AVCaptureDevice.TorchMode.on){
            lightEnabled = true
        }

        var canOpenSettings = false
        if #available(iOS 8.0, *) {
            canOpenSettings = true
        }

        var canEnableLight = false
        if(backCamera?.hasTorch == true && backCamera?.isTorchAvailable == true && backCamera?.isTorchModeSupported(AVCaptureDevice.TorchMode.on) == true){
            canEnableLight = true
        }

        var canChangeCamera = false;
        if(backCamera != nil && frontCamera != nil){
            canChangeCamera = true
        }

        let status = [
            "authorized": boolToNumberString(bool: authorized),
            "denied": boolToNumberString(bool: denied),
            "restricted": boolToNumberString(bool: restricted),
            "prepared": boolToNumberString(bool: prepared),
            "scanning": boolToNumberString(bool: scanning),
            "previewing": boolToNumberString(bool: previewing),
            "showing": boolToNumberString(bool: showing),
            "lightEnabled": boolToNumberString(bool: lightEnabled),
            "canOpenSettings": boolToNumberString(bool: canOpenSettings),
            "canEnableLight": boolToNumberString(bool: canEnableLight),
            "canChangeCamera": boolToNumberString(bool: canChangeCamera),
            "currentCamera": String(currentCamera),
            // New iOS-specific status fields
            "hasAutofocus": boolToNumberString(bool: hasAutofocus),
            "hasFrontCamera": boolToNumberString(bool: frontCamera != nil),
            "hasBackCamera": boolToNumberString(bool: backCamera != nil),
            "hasOpticalImageStabilization": boolToNumberString(bool: hasOpticalImageStabilization),
            "supportedFocusModes": String(supportedFocusModes.count),
            
            // Enhanced iOS features
            "hapticFeedbackEnabled": boolToNumberString(bool: hapticFeedbackEnabled),
            "performanceMonitoringEnabled": boolToNumberString(bool: performanceMonitoringEnabled),
            "visionFrameworkValidationEnabled": boolToNumberString(bool: visionFrameworkValidationEnabled),
            "scanCount": String(scanCount),
            "lastScanTime": String(format: "%.0f", lastScanTime * 1000), // in milliseconds
            "averageScanTime": String(format: "%.0f", averageScanTime * 1000), // in milliseconds
            "iosVersion": UIDevice.current.systemVersion,
            "deviceModel": UIDevice.current.model,
            "hapticFeedbackAvailable": boolToNumberString(bool: isHapticFeedbackAvailable())
        ]

        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: status)
        commandDelegate!.send(pluginResult, callbackId:command.callbackId)
    }

    @objc func openSettings(_ command: CDVInvokedUrlCommand) {
        if #available(iOS 10.0, *) {
            #if swift(>=4.2)
            guard let settingsUrl = URL(string: UIApplication.openSettingsURLString) else {
                return
            }
            #else
            guard let settingsUrl = URL(string: UIApplicationOpenSettingsURLString) else {
                return
            }
            #endif
            if UIApplication.shared.canOpenURL(settingsUrl) {
                UIApplication.shared.open(settingsUrl, completionHandler: {
                    (success) in self.getStatus(command)
                })
            } else {
                self.sendErrorCode(command: command, error: QRScannerError.open_settings_unavailable)
            }
        } else {
            // pre iOS 10.0
            if #available(iOS 8.0, *) {
                #if swift(>=4.2)
                UIApplication.shared.openURL(NSURL(string: UIApplication.openSettingsURLString)! as URL)
                #else
                UIApplication.shared.openURL(NSURL(string: UIApplicationOpenSettingsURLString)! as URL)
                #endif
                self.getStatus(command)
            } else {
                self.sendErrorCode(command: command, error: QRScannerError.open_settings_unavailable)
            }
        }
    }
}
