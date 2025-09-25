# ML Kit 16KB Page Size Issue - Current Status

## 🚨 **CONFIRMED: ML Kit Still Has 16KB Alignment Issues**

Based on the official [ML Kit GitHub issue #947](https://github.com/googlesamples/mlkit/issues/947), **Google has not yet fixed the 16KB page size alignment issue** even in the latest versions.

### **Affected Libraries:**
- `libbarhopper_v3.so`
- `libimage_processing_util_jni.so` 
- `libsurface_util_jni.so`

### **Impact:**
- ❌ **Android Gradle Plugin 8.5+** fails with `stripReleaseDebugSymbols`
- ❌ **Android 15 (SDK 35)** 16KB page size warnings
- ❌ **Release builds** may fail alignment verification

## ✅ **RECOMMENDED SOLUTION: Use ZXING_ONLY**

Given that Google hasn't resolved this issue, the plugin now **defaults to ZXING_ONLY=true** for maximum compatibility.

### **Default Installation (ZXING_ONLY):**
```bash
cordova plugin add cordova-plugin-barcode-qrscanner
```
- ✅ **No 16KB alignment issues**
- ✅ **Works with Android 15**
- ✅ **No Google Play Services dependencies**
- ✅ **Smaller APK size**
- ✅ **Builds successfully on all AGP versions**

### **If You Still Want ML Kit (At Your Own Risk):**
```bash
cordova plugin add cordova-plugin-barcode-qrscanner \
  --variable ZXING_ONLY=false \
  --variable MLKIT_VERSION=17.2.0
```

**⚠️ Warning**: You may still encounter 16KB alignment warnings and build issues.

## 📊 **Comparison: ZXing vs ML Kit**

| Feature | ZXing | ML Kit |
|---------|--------|--------|
| **16KB Alignment** | ✅ No issues | ❌ Known problems |
| **Android 15** | ✅ Full support | ⚠️ Warnings |
| **Build Stability** | ✅ Always works | ❌ May fail |
| **Dependencies** | ✅ Minimal | ❌ Google Play Services |
| **APK Size** | ✅ Smaller | ❌ Larger |
| **Performance** | ⭐⭐⭐ Good | ⭐⭐⭐⭐⭐ Better |
| **Reliability** | ⭐⭐⭐⭐⭐ Excellent | ⭐⭐⭐ Issues exist |

## 🔄 **Plugin Behavior Changes**

### **Before:**
- Default: Hybrid mode (ML Kit + ZXing)
- Users had to opt-in to ZXING_ONLY

### **After (Current):**
- **Default: ZXING_ONLY=true**
- Users must opt-in to ML Kit (with warnings)
- Prioritizes build stability over performance

## 🛠️ **For Existing Users**

### **If Currently Using ML Kit and Having Issues:**
```bash
cordova plugin remove cordova-plugin-barcode-qrscanner
cordova plugin add cordova-plugin-barcode-qrscanner
# Now defaults to ZXING_ONLY=true
cordova build android
```

### **To Check Your Current Mode:**
Look for these logs:
```
QRScanner: ZXing-only mode enabled (no ML Kit dependencies)
BarcodeScannerFactory: Selected Scanner: ZXING_EMBEDDED
```

## 📅 **Timeline & Updates**

- **May 2025**: Google ML Kit issue #947 reported
- **Current**: Issue still open, no fix available
- **Plugin Response**: Changed default to ZXING_ONLY for stability

### **When Google Fixes This:**
We'll update the plugin to:
1. Detect fixed ML Kit versions
2. Automatically use ML Kit when safe
3. Maintain ZXING_ONLY as fallback

## 🎯 **Bottom Line**

**For Android 15 and production apps**: Use ZXING_ONLY mode until Google resolves the 16KB alignment issues in ML Kit.

The plugin now prioritizes **build stability** and **Android 15 compatibility** over the performance benefits of ML Kit.

## 📚 **References**

- [ML Kit GitHub Issue #947](https://github.com/googlesamples/mlkit/issues/947)
- [Android 16KB Page Size Documentation](https://developer.android.com/guide/practices/page-sizes)
- [Android Gradle Plugin 8.5+ Release Notes](https://developer.android.com/studio/releases/gradle-plugin)
