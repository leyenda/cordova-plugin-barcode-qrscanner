# Plugin Conflict Resolution Guide

## 🚨 **Detected Plugin Conflicts**

Based on your build logs, you have multiple plugins that use Google Play Services:

- `cordova-plugin-background-fetch`
- `cordova-background-geolocation` 
- Google Maps plugins (Play Services Location 20.0.0)
- `cordova-plugin-barcode-qrscanner` (this plugin)

## 🔧 **Solution: Version Alignment**

### **Option 1: Match Existing Versions (Recommended)**

Since your other plugins are using Google Play Services Location 20.0.0, install the QR scanner with compatible versions:

```bash
cordova plugin remove cordova-plugin-barcode-qrscanner
cordova plugin add cordova-plugin-barcode-qrscanner \
  --variable PLAY_SERVICES_VERSION=18.0.0 \
  --variable MLKIT_VERSION=17.2.0 \
  --variable CAMERAX_VERSION=1.3.1
```

### **Option 2: Use ZXING_ONLY (No Conflicts)**

Avoid all Google Play Services conflicts:

```bash
cordova plugin remove cordova-plugin-barcode-qrscanner
cordova plugin add cordova-plugin-barcode-qrscanner \
  --variable ZXING_ONLY=true
```

### **Option 3: Force Latest Versions**

If you want to use the latest ML Kit with 16KB support, you'll need to update ALL your plugins:

```bash
# Update background geolocation
cordova plugin remove cordova-background-geolocation
cordova plugin add cordova-background-geolocation \
  --variable GOOGLE_PLAY_SERVICES_LOCATION_VERSION=21.0.0

# Update QR scanner with latest versions
cordova plugin remove cordova-plugin-barcode-qrscanner
cordova plugin add cordova-plugin-barcode-qrscanner \
  --variable PLAY_SERVICES_VERSION=18.5.0 \
  --variable MLKIT_VERSION=17.3.0
```

## 📋 **Current Plugin Versions Detected**

From your build logs:
- **Google Play Services Location**: 20.0.0
- **Google Maps Play Services**: 19.0.0
- **Background Geolocation**: Using Play Services 20.+

## ⚠️ **Build Error Fix Applied**

I've fixed the immediate build error by removing the incompatible `keepDebugSymbols()` method. Your build should now work with:

```gradle
packagingOptions {
    jniLibs {
        useLegacyPackaging false  // This enables 16KB support
    }
}
```

## 🎯 **Recommended Approach**

For your current setup, I recommend **Option 1** with matched versions:

```bash
cordova plugin remove cordova-plugin-barcode-qrscanner
cordova plugin add cordova-plugin-barcode-qrscanner \
  --variable PLAY_SERVICES_VERSION=18.0.0 \
  --variable MLKIT_VERSION=17.2.0
```

This will:
- ✅ **Avoid version conflicts** with your existing plugins
- ✅ **Still provide ML Kit functionality** 
- ✅ **Build successfully** without errors
- ✅ **Work with Android 15** (though without the latest 16KB optimizations)

## 🔍 **Debugging Version Conflicts**

To see all your Play Services versions:

```bash
cd platforms/android
./gradlew app:dependencies | grep "com.google.android.gms"
```

## 🚀 **Testing After Fix**

1. **Clean build**:
   ```bash
   cordova clean android
   cordova build android
   ```

2. **Check logs** for:
   ```
   QRScanner: Hybrid mode enabled (ML Kit + ZXing)
   QRScanner: ML Kit version: 17.2.0
   QRScanner: Play Services version: 18.0.0
   ```

3. **Verify functionality**:
   - QR scanning works
   - No build errors
   - No runtime crashes

The key is maintaining version consistency across all your plugins that use Google Play Services!
