# Android release process

The GitHub workflow builds an optimized, resource-shrunk, signed release APK and Android App Bundle. It verifies the APK signature and includes SHA256 checksums, the public signing certificate, and R8 mapping in the release artifact. Instrumentation runs against the release build on Android API 26 and 35.

## Signing

Private signing material is stored in encrypted repository secrets:

- `VERDANT_SIGNING_KEYSTORE`: base64-encoded PKCS12 keystore.
- `VERDANT_SIGNING_PASSWORD`: its store/key password.

The alias is `verdant`. The workflow writes the keystore into a temporary ignored `.signing` directory and removes it after the job. Keystore files, passwords, and owner backups must never be committed or uploaded as public artifacts. The app's owner has a separate local private signing backup.

For a local release build, set `VERDANT_SIGNING_FILE` to the private PKCS12 path and `VERDANT_SIGNING_PASSWORD` in the process environment. Then run:

```sh
./gradlew :androidApp:assembleRelease :androidApp:bundleRelease
```

Do not print the password or pass it as a source-controlled Gradle property. Preserve the signing material for future APK updates. A previously installed development APK may have a different signing certificate; export a backup before uninstalling it to install the signed release.

## Versioning and publishing

Increment `versionCode` and update `versionName` in `androidApp/build.gradle.kts` for each published update. Preserve the application ID `com.centiox.verdant` and signing identity.

The APK supports direct installation. The AAB is the artifact for Play Console. Publishing to Google Play additionally requires the owner's Play Console account, Play App Signing configuration, store listing, publicly hosted privacy policy/contact details, declarations, and store review. This workflow builds artifacts and tests them; it does not publish to a store automatically.

The version-1 JSON backup schema remains compatible with previous Verdant backups. Android migrates the earlier Kotlin plaintext ledger to encrypted storage only after validation and a successful encrypted write. Tauri backups can be restored through Settings.
