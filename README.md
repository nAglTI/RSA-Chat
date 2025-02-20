# chat-frontend

This is a multiplatform frontend for [chat-backend](https://github.com/hypergonial/chat-backend).
It is written in Kotlin and uses [Ktor](https://ktor.io/) for networking and [Compose](https://www.jetbrains.com/compose-multiplatform/) for the UI.
It runs natively on Android and iOS, desktop via the JVM, and experimentally in a web browser with WebAssembly.

## Building

The recommended JDK version for this project is **OpenJDK 17**. Higher versions may not work, especially in release builds due to lacking Proguard support.

### Desktop

To run the project on your desktop:

```shell
./gradlew :composeApp:run
```

To build a distributable package:

```shell
./gradlew :composeApp:createDistributable
```

To create a release distributable:

```shell
./gradlew :composeApp:createReleaseDistributable
```

The output will be in `composeApp/build/compose/binaries/main/app`.

> [!WARNING]
> The release build of the desktop app is currently experimental, and may have issues due to the Proguard configuration.

### Android

Open the project in [Android Studio](https://developer.android.com/studio) and run the "Android App" configuration.

Alternatively, if you want to just build the APK and manually install it on a device, you can run:

```shell
./gradlew :composeApp:assembleRelease
```

or for a debug build:

```shell
./gradlew :composeApp:assembleDebug
```

The output will be in `composeApp/build/outputs/apk`.

### iOS

iOS development requires a Mac with Xcode installed. This build process is by far the most complicated one.

First, download [kdoctor](https://github.com/Kotlin/kdoctor) and run it to identify & fix any the issues with your setup.

Then open Android Studio, and either add or execute the iOS configuration.

> [!IMPORTANT]
> The iOS app is currently experimental and rarely tested due to not having a Mac available during regular development.

### Web

To run a development server:

```shell
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

For production, you should build the site and serve it with a proper web server:

```shell
./gradlew :composeApp:wasmJsBrowserDistribution
```

## Usage

Before logging in or registering, you need to set up the API endpoints the client should use.
This can be done by opening a "secret" configuration menu, which is done by clicking the logo on the login screen 8 times in a row.
After this is done, you can log in or register with the [chat-backend](https://github.com/hypergonial/chat-backend) server.

> [!IMPORTANT]
> The Android & iOS apps don't support API endpoints without TLS. This is because they require secure connections by default.

## Development

The recommended IDE for this project is [Android Studio](https://developer.android.com/studio). There are several run configurations set up for the different platforms.

## License

This project is licensed under GPL-3.0. See the [LICENSE](LICENSE) file for details.
