# Network Switch - Android App

A powerful Android application that allows users to switch between different network modes (5G Only, 5G Preferred, 4G Only) with a beautiful Material Design 3 interface.

## Features

- **Network Mode Switching**: Seamlessly switch between 5G Only, 5G Preferred, and 4G Only modes
- **Quick Settings Tile**: Access network modes directly from the Quick Settings panel
- **Permission Management**: Comprehensive permission handling with ADB and Shizuku support
- **Material Design 3**: Modern, beautiful UI following the latest Android design guidelines
- **Dark/Light Theme**: Automatic theme switching based on system preferences
- **Setup Guide**: Step-by-step instructions for granting required permissions

## Requirements

- **Minimum SDK**: Android 12 (API 31)
- **Target SDK**: Android 16 (API 36)
- **Permissions**: WRITE_SECURE_SETTINGS (granted via ADB or Shizuku)

## Installation & Setup

### Option 1: ADB Setup (Recommended for Developers)

1. Enable **Developer Options** on your Android device
2. Enable **USB Debugging** in Developer Options
3. Connect your device to a computer with ADB installed
4. Run the following command:
   ```bash
   adb shell pm grant com.networkswitcher android.permission.WRITE_SECURE_SETTINGS
   ```

### Option 2: Shizuku Setup (Recommended for Users)

1. Install [Shizuku](https://shizuku.rikka.app/) from Google Play Store
2. Start Shizuku service (follow Shizuku's setup guide)
3. Open Network Switch and grant permissions when prompted

## Architecture

The app follows modern Android development best practices:

- **MVVM Architecture**: ViewModel + LiveData for reactive UI
- **Repository Pattern**: Clean separation of data sources
- **Coroutines**: Asynchronous operations with proper lifecycle management
- **Dependency Injection**: Manual DI through Application class
- **Material Design 3**: Latest design system implementation

## Project Structure

```
app/src/main/java/com/networkswitcher/
├── NetworkSwitchApplication.kt          # Application class with DI setup
├── ui/
│   ├── MainActivity.kt                  # Main activity with network mode selection
│   ├── SetupGuideActivity.kt           # Permission setup guide
│   └── viewmodel/
│       ├── MainViewModel.kt            # Main screen logic
│       └── MainViewModelFactory.kt     # ViewModel factory
├── service/
│   └── NetworkSwitchTileService.kt     # Quick Settings Tile implementation
├── manager/
│   ├── NetworkModeManager.kt           # Core network mode switching logic
│   └── PermissionManager.kt            # Permission state management
├── data/
│   ├── model/
│   │   ├── NetworkMode.kt              # Network mode enum
│   │   └── PermissionState.kt          # Permission state enum
│   └── repository/
│       ├── SettingsRepository.kt       # App settings persistence
│       └── NetworkRepository.kt        # Network information
└── util/
    └── Resource.kt                     # Result wrapper class
```

## Network Mode Values

The app uses the following network mode values for Android's `preferred_network_mode` setting:

- **5G Only**: 27 (NR_ONLY)
- **5G Preferred**: 26 (NR_LTE_GSM_WCDMA)
- **4G Only**: 11 (LTE_ONLY)

## Building the Project

1. Clone the repository or extract the project files
2. Open in Android Studio Arctic Fox or later
3. Sync Gradle files
4. Build and run on device with Android 12+

### Dependencies

The project uses the following key dependencies:

- **AndroidX Core**: Latest core libraries
- **Material Design 3**: Modern UI components
- **Lifecycle Components**: ViewModel and LiveData
- **Shizuku API**: For elevated permission handling
- **Coroutines**: Asynchronous programming

## Testing

The project includes unit tests for core functionality:

```bash
./gradlew test
```

## Security Considerations

- The app requires `WRITE_SECURE_SETTINGS` permission, which can only be granted via ADB or Shizuku
- No sensitive data is stored or transmitted
- All operations are performed locally on the device
- Permission state is checked before each operation

## Compatibility

- **Minimum**: Android 12 (API 31)
- **Target**: Android 16 (API 36)
- **Tested on**: Android 12, 13, 14, 15
- **Architecture**: ARM64, ARM, x86_64

## Troubleshooting

### Permission Issues
- Ensure USB Debugging is enabled
- Verify ADB command was executed successfully
- Try revoking and re-granting the permission

### Network Mode Not Changing
- Check if your device supports the selected network mode
- Verify SIM card is inserted and active
- Some carriers may override network mode settings

### Quick Settings Tile Not Working
- Add the tile to Quick Settings manually
- Check permission status in the main app
- Restart the app if tile appears inactive

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- **Shizuku**: For providing elevated permission management
- **Material Design Team**: For the beautiful design system
- **Android Development Team**: For the comprehensive development platform

## Support

For issues and support:
1. Check the troubleshooting section
2. Review the setup guide in the app
3. Open an issue on GitHub with device info and logs

---

**Note**: This app modifies system-level network settings and requires special permissions. Use responsibly and ensure you understand the implications of changing network modes on your device.