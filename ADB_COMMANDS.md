# ADB Commands for Network Switch

This document contains the ADB commands needed to grant the required permissions for the Network Switch app.

## Prerequisites

1. **Enable Developer Options**:
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times
   - Developer Options will appear in Settings

2. **Enable USB Debugging**:
   - Go to Settings → Developer Options
   - Enable "USB Debugging"

3. **Install ADB**:
   - Download Android SDK Platform Tools
   - Add ADB to your system PATH
   - Or use Android Studio's built-in ADB

## Required Commands

### Grant WRITE_SECURE_SETTINGS Permission

This is the main permission required for the app to function:

```bash
adb shell pm grant com.networkswitcher android.permission.WRITE_SECURE_SETTINGS
```

### Verify Permission (Optional)

Check if the permission was granted successfully:

```bash
adb shell dumpsys package com.networkswitcher | grep "WRITE_SECURE_SETTINGS"
```

### Alternative: Grant via dumpsys (If above doesn't work)

```bash
adb shell settings put secure enabled_accessibility_services com.networkswitcher
```

## Troubleshooting Commands

### Check Device Connection

```bash
adb devices
```
Expected output should show your device as "device" (not "unauthorized").

### Check App Installation

```bash
adb shell pm list packages | grep networkswitcher
```

### View Current Network Mode

```bash
adb shell settings get global preferred_network_mode
```

### View Current Network Mode (Slot 1)

```bash
adb shell settings get global preferred_network_mode1
```

### Manually Set Network Mode (For Testing)

**5G Preferred:**
```bash
adb shell settings put global preferred_network_mode 26
adb shell settings put global preferred_network_mode1 26
```

**4G Only:**
```bash
adb shell settings put global preferred_network_mode 11
adb shell settings put global preferred_network_mode1 11
```

**5G Only:**
```bash
adb shell settings put global preferred_network_mode 27
adb shell settings put global preferred_network_mode1 27
```

## Revoke Permission (If Needed)

To remove the permission:

```bash
adb shell pm revoke com.networkswitcher android.permission.WRITE_SECURE_SETTINGS
```

## Wireless ADB (Android 11+)

For wireless debugging without USB cable:

1. **Enable Wireless Debugging** in Developer Options
2. **Connect via IP**:
   ```bash
   adb connect <device-ip>:5555
   ```
3. **Run commands as normal**

## Network Mode Values Reference

| Mode | Value | Description |
|------|-------|-------------|
| GSM Only | 1 | 2G only |
| GSM/WCDMA | 0 | 2G/3G |
| WCDMA Only | 2 | 3G only |
| GSM/WCDMA/LTE | 9 | 2G/3G/4G |
| LTE Only | 11 | 4G only |
| LTE/GSM/WCDMA | 12 | 4G/3G/2G |
| NR_LTE_GSM_WCDMA | 26 | 5G/4G/3G/2G (5G Preferred) |
| NR_ONLY | 27 | 5G only |

## Common Issues

### "Permission Denial" Error
- Ensure Developer Options and USB Debugging are enabled
- Try different USB cable or port
- Check if device requires authorization prompt

### "Device Unauthorized"
- Check device for authorization prompt
- Revoke USB debugging authorizations and try again
- Ensure ADB is the latest version

### "Package Not Found"
- Ensure the app is installed: `adb install app-debug.apk`
- Check package name is correct

### Permission Not Taking Effect
- Restart the Network Switch app
- Toggle airplane mode and back
- Reboot device if necessary

## Security Notes

- `WRITE_SECURE_SETTINGS` is a system-level permission
- Only grant to trusted applications
- Permission persists until app is uninstalled or manually revoked
- Some OEMs may restrict this permission regardless

## Alternative: Using Shizuku

Instead of ADB, you can use Shizuku for a user-friendly permission management:

1. Install Shizuku from Play Store
2. Start Shizuku service (requires ADB once for setup)
3. Use Network Switch app to request permission through Shizuku

This method is recommended for non-technical users as it provides a GUI for permission management.