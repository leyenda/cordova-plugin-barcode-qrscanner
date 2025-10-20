# cordova-plugin-barcode-qrscanner

> A fast, energy efficient, highly-configurable QR code + barcode scanner for Cordova / Ionic apps – available for the iOS, Android, Windows, and browser platforms.

> Support also CODE_39, CODE_93, CODE_128 standards

> Supports Android API level 32
---

Works as of **January 2023**.

Original project & documentation: https://www.npmjs.com/package/cordova-plugin-qrscanner

```
ionic@6
cordova@11
cordova-ios@6
cordova-android@11
angular@14
```

Original plugin/repo is `https://github.com/bitpay/cordova-plugin-qrscanner`. It was not working with the latest Android/iOS version. _It failed during XCode build._

Other packages on npm with apparent solutions kept installing the original plugin. _So they fail during XCode build, too._

---

**This is a fork of https://github.com/gianluigitrontini/cordova-plugin-qrscanner-nbs. That fork was forked from https://github.com/v1934/cordova-plugin-qrscanner-11, with a commit taken from https://github.com/NoahSun/cordova-plugin-qrscanner.**

**This plugin is working with the latest android and ios platform and contains fixes for compilation errors due to incompatibilities with Swift 5 and Android API level 32**

## 🚀 **Latest Enhancements (2025)**

This fork includes comprehensive Android modernization and JavaScript API enhancements:

### Android Improvements
- ✅ **Hybrid Scanner System** - Intelligent ML Kit + ZXing fallback for optimal performance
- ✅ **Google ML Kit Integration** - 3x faster scanning with hardware acceleration
- ✅ **Automatic Fallback** - Seamless ZXing fallback when ML Kit unavailable
- ✅ **Fixed critical NullPointerException** that caused crashes during camera lifecycle
- ✅ **AndroidX migration** - Updated from deprecated Support Library 
- ✅ **Modern dependencies** - Target SDK 34, latest build tools
- ✅ **Enhanced permission handling** - Better UX with rationale support
- ✅ **Performance optimizations** - Memory monitoring, autofocus detection
- ✅ **Robust error handling** - New error codes and comprehensive logging

### iOS Improvements
- ✅ **Haptic Feedback System** - Tactile confirmation for successful scans (iOS 10+)
- ✅ **Performance Monitoring** - Real-time scan metrics and optimization insights
- ✅ **Vision Framework Integration** - Optional enhanced validation (iOS 11+)
- ✅ **Advanced Hardware Detection** - Autofocus, OIS, and focus mode capabilities
- ✅ **Memory Management** - Proactive memory monitoring and low-memory handling
- ✅ **Enhanced Camera Controls** - iOS version-specific optimizations
- ✅ **Comprehensive Status Reporting** - Device info, performance metrics, feature availability
- ✅ **Safe Area Support** - Modern iPhone X+ layout compatibility

### JavaScript API Enhancements
- ✅ **New status fields** - Hardware capability detection (`hasAutofocus`, `hasFrontCamera`, etc.)
- ✅ **Enhanced error codes** - Better error categorization and handling
- ✅ **Utility methods** - Smart camera switching, capability checking
- ✅ **Permission helpers** - `shouldShowPermissionRationale()` for better UX
- ✅ **Backward compatibility** - All existing code continues to work

## 🚨 **Avoiding Dependency Conflicts**

**IMPORTANT**: This plugin uses Google Play Services and AndroidX libraries. To avoid conflicts:

### **Default Installation (Recommended)**
```bash
cordova plugin add cordova-plugin-barcode-qrscanner
```

### **With Firebase Plugin**
```bash
cordova plugin add cordova-plugin-barcode-qrscanner \
  --variable PLAY_SERVICES_VERSION=17.0.0
```

### **ZXing-Only Mode (No Google Dependencies)**
```bash
cordova plugin add cordova-plugin-barcode-qrscanner \
  --variable ZXING_ONLY=true
```

**📋 See [Dependency Management](DEPENDENCY_MANAGEMENT.md) for complete conflict resolution guide.**

### Documentation
- 📋 **[Dependency Management](DEPENDENCY_MANAGEMENT.md)** - **🚨 IMPORTANT** - Avoiding version conflicts
- 📋 **[Hybrid Android Implementation](HYBRID_ANDROID_IMPLEMENTATION.md)** - ML Kit + ZXing hybrid scanner system
- 📋 **[Enhanced iOS Implementation](ENHANCED_IOS_IMPLEMENTATION.md)** - Advanced iOS features and optimizations
- 📋 **[Hybrid Example Usage](HYBRID_EXAMPLE_USAGE.js)** - Examples using the new hybrid Android scanner
- 📋 **[iOS Example Usage](IOS_EXAMPLE_USAGE.js)** - Examples using enhanced iOS features
- 📋 **[Android Improvements Guide](ANDROID_IMPROVEMENTS.md)** - Detailed Android technical improvements
- 📋 **[iOS Improvements Guide](IOS_IMPROVEMENTS.md)** - Comprehensive iOS Swift enhancements
- 📋 **[JavaScript API Enhancements](JAVASCRIPT_API_ENHANCEMENTS.md)** - New features and usage examples
- 📋 **[Example Usage](EXAMPLE_USAGE.js)** - Practical implementation examples

---

If you're using Ionic, switch to Capacitor.
