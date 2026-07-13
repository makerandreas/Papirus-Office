# Keystore and Release Signing Security Guidelines

These guidelines describe how to securely sign Papirus Office APKs/AABs inside our automated GitHub Actions workflow without leaking the cryptographic keys or passwords.

## 1. Local Keystore Generation

Generate a secure release keystore locally using the `keytool` command from your JDK installation:

```bash
keytool -genkey -v -keystore my-upload-key.jks \
  -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

> **Warning:** Never commit the `.jks` file directly to the GitHub repository.

## 2. GitHub Secrets Integration

To enable automated release signing, configure the following secrets in your GitHub Repository under **Settings > Secrets and variables > Actions**:

| Secret Name | Description | Example / Format |
|-------------|-------------|------------------|
| `KEYSTORE_BASE64` | The complete `.jks` file encoded as Base64. | Output from `base64 -w 0 my-upload-key.jks` |
| `STORE_PASSWORD` | The password for opening the Keystore container. | Strong alphanumeric passphrase |
| `KEY_PASSWORD` | The password for the specific key alias (`upload`). | Alphanumeric passphrase |

## 3. How to Convert the Keystore to Base64

To generate the payload for `KEYSTORE_BASE64`:

- **Linux / macOS**:
  ```bash
  base64 -i my-upload-key.jks -o keystore_b64.txt
  # Copy content from keystore_b64.txt
  ```
- **Windows (PowerShell)**:
  ```powershell
  [Convert]::ToBase64String([IO.File]::ReadAllBytes("my-upload-key.jks")) > keystore_b64.txt
  ```

## 4. Keystore Extraction during CI

Our GitHub Actions workflow automatically checks for the presence of `${{ secrets.KEYSTORE_BASE64 }}`. If available, it decodes the binary, creates the local keystore file on the build runner, and signs the release APK/Bundle using the password environmental variables passed into Gradle:

```groovy
System.getenv("STORE_PASSWORD")
System.getenv("KEY_PASSWORD")
```

This prevents secrets from being stored as plain text inside `build.gradle.kts` and completely protects your private key.
