# `jniLibs` — Phase 2 native drop-in (intentionally empty)

Place per-ABI LibreOfficeKit builds here:

```
app/src/main/jniLibs/
├── arm64-v8a/
│   ├── liblo-native-code.so
│   ├── libc++_shared.so
│   └── libnss3.so (+ rest of the dependency chain)
├── armeabi-v7a/
├── x86_64/
└── x86/
```

- `LokitEngine` / `LibreOfficeCore` probe for `lo-native-code` (stock
  LibreOffice-Android soname) at startup, then legacy `libreoffice-core`.
- Until these files exist the app runs its pure-Kotlin engine and reports
  simulated mode — see `docs/LOKIT_INTEGRATION.md` for how to obtain the
  binaries, wire the JNI facade, and satisfy license obligations.
- This README is documentation only and is not packaged into the APK.
