<p align="center">
  <img src="https://reseam.app/logo.svg" alt="Reseam logo" width="96">
</p>

<h1 align="center">Tristan Patches</h1>

<p align="center">A <a href="https://reseam.app">Reseam</a> patch made by Tristan</p>
<p align="center">
  <a href="https://reseam.app/add/?url=https://github.com/tristan23612/reseam-patches"><img src="./.github/assets/add-to-reseam.svg" alt="Add to Reseam" height="48"></a>
</p>

## Patches

<!-- patches:start -->
### Dcinside
 - Supported versions: 5-3-25
<!-- patches:end -->

## Setup

- JDK 17.
- Android SDK, with `ANDROID_HOME` pointing at it.
- The `reseam` CLI, on `PATH` or at the path in `RESEAM_BIN`. Use the version that matches the plugin version in `settings.gradle.kts`. Releases ship Linux x64 and Windows x64 binaries; on other platforms [build it from source](https://reseam.app/docs/cli/install/).
- A signing key: `reseam bundle keygen --out ~/.reseam/bundle-signing.key`. It prints the public key users will trust. Keep the key file private and out of the repository.
- 
## Build and test

```shell
./gradlew bundle
reseam bundle list build/reseam/tristan-patches.reseam
reseam patch app.apk \
  --bundle build/reseam/example-patches.reseam \
  --trust 7334652ea221f7eb75c811809bf61901a34100bd2405bf4436677dcf89dd183a \
  --enable "Enable debugging" \
  --output patched.apk
```

## Release

[Latest Release](https://github.com/tristan23612/reseam-patches/releases)

## License

AGPL-3.0-or-later.
