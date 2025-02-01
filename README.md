# chat-frontend

This is a multiplatform frontend for [chat-backend](https://github.com/hypergonial/chat-backend).
It is written in Kotlin and uses [Ktor](https://ktor.io/) for networking and [Compose](https://www.jetbrains.com/compose-multiplatform/) for the UI.
It runs natively on Android and iOS, desktop via the JVM, and experimentally in a web browser with WebAssembly.

## Building

### Desktop

To run the project on your desktop:

```shell
./gradlew :composeApp:run
```

To build a distributable package:

```shell
./gradlew :composeApp:createDistributable
```

### Android

Open the project in [Android Studio](https://developer.android.com/studio) and run the "Android App" configuration.

### iOS

iOS development requires a Mac with Xcode installed.
The iOS build is untested due to me unfortunately not having a Mac, and may not work out of the box. Contributions are welcome to fix issues here.

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
> The Android app doesn't support API endpoints without TLS. This is because Android requires secure connections by default.
> If you want to use an insecure connection, you need to add a `networkSecurityConfig` in the `AndroidManifest.xml` file.

## Development

The recommended IDE for this project is [Android Studio](https://developer.android.com/studio). There are several run configurations set up for the different platforms.

## License

This project is licensed under GPL-3.0. See the [LICENSE](LICENSE) file for details.
