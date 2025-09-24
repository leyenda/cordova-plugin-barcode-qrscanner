# Dependency Management & Version Conflicts

## 🚨 **The Problem**

Cordova plugins often depend on different versions of Google Play Services and AndroidX libraries, leading to:

- **Build conflicts** during compilation
- **Runtime crashes** due to version mismatches  
- **Dependency resolution failures** in Gradle
- **Incompatibility** between plugins

## 🛠️ **Our Solution**

This plugin provides **configurable dependency versions** to prevent conflicts with other plugins in your project.

## ⚙️ **Configurable Dependencies**

### **Default Versions (2024)**
```
Google Play Services Base: 18.2.0
ML Kit Barcode Scanning: 17.2.0
ZXing Android Embedded: 4.3.0
AndroidX AppCompat: 1.6.1
AndroidX Core: 1.12.0
CameraX: 1.3.1
```

### **How to Configure Versions**

#### **Method 1: During Plugin Installation**
```bash
# Install with specific Play Services version
cordova plugin add cordova-plugin-barcode-qrscanner \
  --variable PLAY_SERVICES_VERSION=18.1.0 \
  --variable MLKIT_VERSION=17.1.0

# Install with all custom versions
cordova plugin add cordova-plugin-barcode-qrscanner \
  --variable PLAY_SERVICES_VERSION=18.1.0 \
  --variable MLKIT_VERSION=17.1.0 \
  --variable ZXING_VERSION=4.2.0 \
  --variable ANDROIDX_APPCOMPAT_VERSION=1.5.1 \
  --variable ANDROIDX_CORE_VERSION=1.10.0 \
  --variable CAMERAX_VERSION=1.2.3
```

#### **Method 2: Via config.xml**
```xml
<plugin name="cordova-plugin-barcode-qrscanner" spec="^3.0.12">
    <variable name="PLAY_SERVICES_VERSION" value="18.1.0" />
    <variable name="MLKIT_VERSION" value="17.1.0" />
    <variable name="ZXING_VERSION" value="4.2.0" />
    <variable name="ANDROIDX_APPCOMPAT_VERSION" value="1.5.1" />
    <variable name="ANDROIDX_CORE_VERSION" value="1.10.0" />
    <variable name="CAMERAX_VERSION" value="1.2.3" />
</plugin>
```

#### **Method 3: Via package.json (Cordova 7+)**
```json
{
  "cordova": {
    "plugins": {
      "cordova-plugin-barcode-qrscanner": {
        "PLAY_SERVICES_VERSION": "18.1.0",
        "MLKIT_VERSION": "17.1.0",
        "ZXING_VERSION": "4.2.0",
        "ANDROIDX_APPCOMPAT_VERSION": "1.5.1",
        "ANDROIDX_CORE_VERSION": "1.10.0",
        "CAMERAX_VERSION": "1.2.3"
      }
    }
  }
}
```

## 🔍 **Finding the Right Versions**

### **Check Existing Dependencies**
```bash
# Check current Play Services version in your project
grep -r "play-services" platforms/android/app/build.gradle
grep -r "play-services" platforms/android/*/build.gradle

# Check AndroidX versions
grep -r "androidx" platforms/android/app/build.gradle
```

### **Common Plugin Compatibility**

#### **Firebase Plugins**
```bash
# If using cordova-plugin-firebase
--variable PLAY_SERVICES_VERSION=17.0.0
```

#### **Google Maps**
```bash
# If using cordova-plugin-googlemaps
--variable PLAY_SERVICES_VERSION=17.5.0
```

#### **Push Notifications**
```bash
# If using phonegap-plugin-push
--variable PLAY_SERVICES_VERSION=16.0.0
```

## 📊 **Version Compatibility Matrix**

| Plugin | Play Services | ML Kit | AndroidX |
|--------|---------------|--------|----------|
| **Firebase** | 17.0.0 - 18.0.0 | 16.0.0+ | 1.4.0+ |
| **Google Maps** | 17.5.0+ | Any | 1.5.0+ |
| **Push Notifications** | 16.0.0+ | Any | 1.3.0+ |
| **AdMob** | 18.0.0+ | Any | 1.6.0+ |

## 🚨 **Common Conflict Scenarios**

### **Scenario 1: Firebase Plugin Conflict**
```
Error: Duplicate class com.google.android.gms.common.api.Api
```

**Solution:**
```bash
cordova plugin add cordova-plugin-barcode-qrscanner \
  --variable PLAY_SERVICES_VERSION=17.0.0
```

### **Scenario 2: AndroidX Version Conflict**
```
Error: AndroidX artifact androidx.appcompat:appcompat:1.6.1 not found
```

**Solution:**
```bash
cordova plugin add cordova-plugin-barcode-qrscanner \
  --variable ANDROIDX_APPCOMPAT_VERSION=1.5.1 \
  --variable ANDROIDX_CORE_VERSION=1.9.0
```

### **Scenario 3: ML Kit Version Conflict**
```
Error: Could not resolve com.google.mlkit:barcode-scanning:17.2.0
```

**Solution:**
```bash
cordova plugin add cordova-plugin-barcode-qrscanner \
  --variable MLKIT_VERSION=17.0.0
```

## 🔧 **Advanced Configuration**

### **Gradle Properties Override**
Create `platforms/android/gradle.properties`:
```properties
# Override plugin dependency versions
playServicesVersion=18.1.0
mlKitVersion=17.1.0
zxingVersion=4.2.0
appCompatVersion=1.5.1
coreVersion=1.10.0
cameraXVersion=1.2.3
```

### **Build Script Configuration**
Add to `platforms/android/build-extras.gradle`:
```gradle
// Force specific dependency versions
configurations.all {
    resolutionStrategy {
        force 'com.google.android.gms:play-services-base:18.1.0'
        force 'androidx.appcompat:appcompat:1.5.1'
        force 'androidx.core:core:1.10.0'
    }
}
```

## 🧪 **Testing Version Compatibility**

### **1. Clean Build Test**
```bash
# Remove and re-add platform
cordova platform remove android
cordova platform add android

# Clean build
cordova clean android
cordova build android
```

### **2. Dependency Analysis**
```bash
# Check final dependency versions
cd platforms/android
./gradlew app:dependencies --configuration implementation
```

### **3. Runtime Testing**
```javascript
// Test hybrid scanner functionality
QRScanner.prepare(function(err, status) {
    console.log('Scanner Type:', status.scannerType);
    console.log('ML Kit Available:', status.mlKitAvailable);
    console.log('Using ML Kit:', status.usingMLKit);
});
```

## 📋 **Troubleshooting Guide**

### **Build Fails with Dependency Conflicts**

1. **Identify conflicting versions:**
   ```bash
   cordova build android --verbose
   ```

2. **Check other plugin dependencies:**
   ```bash
   cordova plugin list
   grep -r "play-services" plugins/*/plugin.xml
   ```

3. **Use compatible versions:**
   ```bash
   cordova plugin remove cordova-plugin-barcode-qrscanner
   cordova plugin add cordova-plugin-barcode-qrscanner \
     --variable PLAY_SERVICES_VERSION=17.0.0
   ```

### **Runtime Crashes**

1. **Check device logs:**
   ```bash
   adb logcat | grep -E "(QRScanner|MLKit|ZXing)"
   ```

2. **Verify scanner selection:**
   ```javascript
   QRScanner.getStatus(function(status) {
       console.log('Scanner details:', {
           type: status.scannerType,
           mlKitAvailable: status.mlKitAvailable,
           usingMLKit: status.usingMLKit
       });
   });
   ```

## 🎯 **Recommendations**

### **For New Projects**
- Use **default versions** (most recent, well-tested)
- Only customize if you encounter conflicts

### **For Existing Projects**
- **Audit existing dependencies** before adding the plugin
- Use **lowest common denominator** versions
- Test thoroughly on multiple devices

### **For Enterprise Projects**
- **Pin specific versions** for consistency
- Use **build-extras.gradle** for global version control
- Implement **automated testing** for dependency changes

## 🚀 **Migration Guide**

### **From Older Versions**
```bash
# Remove old plugin
cordova plugin remove cordova-plugin-barcode-qrscanner

# Add new version with compatible dependencies
cordova plugin add cordova-plugin-barcode-qrscanner \
  --variable PLAY_SERVICES_VERSION=17.0.0 \
  --variable ANDROIDX_APPCOMPAT_VERSION=1.4.0

# Clean and rebuild
cordova clean android
cordova build android
```

### **Updating Dependencies**
```bash
# Check current versions
cordova plugin list

# Remove and re-add with new versions
cordova plugin remove cordova-plugin-barcode-qrscanner
cordova plugin add cordova-plugin-barcode-qrscanner \
  --variable PLAY_SERVICES_VERSION=18.2.0
```

## 📞 **Support**

### **Common Issues**
- **Build conflicts**: Use compatible versions from the matrix above
- **Runtime crashes**: Check device logs and scanner status
- **Feature unavailable**: Verify ML Kit availability on device

### **Debug Information**
When reporting issues, include:
```bash
# Plugin version
cordova plugin list | grep barcode-qrscanner

# Dependency versions
grep -r "play-services\|mlkit\|androidx" platforms/android/app/build.gradle

# Scanner status
# JavaScript: QRScanner.getStatus()
```

---

**Remember**: The hybrid scanner system automatically falls back to ZXing if ML Kit is unavailable, ensuring your app always works regardless of dependency conflicts! 🛡️
