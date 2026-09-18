# Keystore and Release Signing Security Guidelines

These guidelines describe how to securely sign Papirus Office APKs/AABs locally and inside the automated GitHub Actions workflow without leaking keys or passwords.

> **Debug builds need no keystore.** `assembleDebug` uses the standard auto-generated debug key. Only `assembleRelease` is signed, and only when a keystore file is present — otherwise Gradle produces an unsigned APK instead of failing.

## 1. Local Keystore Generation

Generate a secure release keystore locally using the `keytool` command from your JDK installation:

```bash
keytool -genkey -v -keystore papirus-release.jks \
  -alias papirus_key -keyalg RSA -keysize 2048 -validity 10000
```

Place the file at `app/papirus-release.jks` (gitignored). Pass the credentials via environment variables or Gradle properties — never in source:

```bash
export KEYSTORE_PASSWORD='…' KEY_ALIAS='papirus_key' KEY_PASSWORD='…'
./gradlew assembleRelease
```

> **Warning:** Never commit the `.jks` file to the repository.

## 2. GitHub Secrets Integration

To enable automated release signing, configure the following secrets under **Settings > Secrets and variables > Actions** (names must match `.github/workflows/build.yml` and `app/build.gradle.kts` exactly):

| Secret Name | Description |
|-------------|-------------|
| `SIGNING_KEY` | The complete `.jks` file encoded as Base64. Decoded to `app/papirus-release.jks` on the runner. |
| `KEYSTORE_PASSWORD` | Password for opening the keystore container. |
| `KEY_ALIAS` | Key alias inside the keystore (e.g. `papirus_key`). |
| `KEY_PASSWORD` | Password for the key alias. |
| `GOOGLE_SERVICES_JSON` | (Optional) `google-services.json` encoded as Base64 for Firebase builds. |

## 3. How to Convert the Keystore to Base64

To generate the payload for `SIGNING_KEY`:

- **Linux / macOS**:
  ```bash
  base64 -w 0 papirus-release.jks -o keystore_b64.txt
  # Copy content from keystore_b64.txt
  ```
- **Windows (PowerShell)**:
  ```powershell
  [Convert]::ToBase64String([IO.File]::ReadAllBytes("papirus-release.jks")) > keystore_b64.txt
  ```

## 4. Keystore Extraction during CI

The workflow decodes `${{ secrets.SIGNING_KEY }}` (when present) into `app/papirus-release.jks` and passes the password secrets to Gradle as environment variables:

```kotlin
System.getenv("KEYSTORE_PASSWORD")
System.getenv("KEY_ALIAS")
System.getenv("KEY_PASSWORD")
```

No fallback passwords exist anywhere in the build scripts or the workflow: a missing credential fails the signing step loudly instead of silently signing with a publicly known password.
