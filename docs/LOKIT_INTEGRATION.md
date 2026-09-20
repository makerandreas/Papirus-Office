
> **Status: Phase 1 complete — simulated.** `LokitEngine` (in `core/lokit/`) is a
> pure-Kotlin mock that mirrors the LibreOffice-Android LOKit surface. It runs
> the app fully in-memory with zero JNI dependencies and reports itself as
> **simulated mode** in the UI. This document defines the exact seams for the
> Phase 2 production native bridge.

## 1. What exists today (Phase 1)

| File | Role |
|------|------|
| `core/lokit/LokitEngine.kt` | Engine facade: lifecycle (init/exit), document open/save, undo/redo, status callbacks. Pure Kotlin, no JNI. |
| `core/lokit/LokitEngine.kt` | Status codes and event model mirroring LOKit (`LOKIT_EVENT_*`, `LOKIT_STATUS_*` enums). |
| `core/LibreOfficeCore.kt` | High-level wrapper that both editor modules (Cellina/Slidia/Inky) already call; delegates to `LokitEngine`. |
| `app/src/main/jniLibs/` | Empty drop-in directory (see its README) where production `.so` files will land. |

No `System.loadLibrary` calls exist anywhere in the codebase yet. The app runs
and ships in simulated mode.

## 2. Phase 2 architecture

```
┌─────────────────────────────────────────────────────────────┐
│ Kotlin (LokitEngine)                                        │
│   - public API stays identical                              │
│   - internal `NativeBridge` interface is implemented by:    │
│       * SimulatedBridge (today, default)                    │
│       * JniBridge (Phase 2) -> JNI -> C++ facade            │
├─────────────────────────────────────────────────────────────┤
│ C/C++ facade (liblokit-facade.so)                           │
│   - extern "C" functions, stable ABI                       │
│   - owns the LibreOfficeKit instance                        │
├─────────────────────────────────────────────────────────────┤
│ LibreOffice Android port                                    │
│   - liblo-native-code.so  (stock LO-Android soname)         │
│   - libc++_shared.so, libnss3.so, ... (dependency chain)    │
│   - resources/ (fonts, icons, dictionaries)                 │
└─────────────────────────────────────────────────────────────┘
```

The Kotlin `LokitEngine` public API is the **frozen contract**: Phase 2 must
not change signatures used by `core/LibreOfficeCore.kt`.

### 2.1 C ABI sketch

```c
// lokit_facade.h — stable, opaque-handle ABI
typedef void* lo_engine_t;

int   lo_engine_create(const char* userdata_dir, const char* workdir);
void  lo_engine_destroy(lo_engine_t e);
int   lo_engine_init(lo_engine_t e);          // returns LOKIT_STATUS_*
void  lo_engine_exit(lo_engine_t e);
int   lo_engine_open(lo_engine_t e, const char* uri);
int   lo_engine_save_as(lo_engine_t e, const char* uri, int filter);
int   lo_engine_undo(lo_engine_t e);
int   lo_engine_redo(lo_engine_t e);
void  lo_engine_set_callback(lo_engine_t e,
                             void (*cb)(int event_code, const char* payload, void* user),
                             void* user);
```

### 2.2 JNI bridge (Kotlin side)

```kotlin
internal class JniBridge(context: Context) : NativeBridge {
    static { System.loadLibrary("lokit-facade") }   // loads the C facade
    // lo_engine_* externs map 1:1 to NativeBridge methods
}
```

`LokitEngine` selects the bridge at startup:

```kotlin
val bridge = if (nativeAvailable(context)) JniBridge(context) else SimulatedBridge()
val isSimulated = bridge is SimulatedBridge
```

`nativeAvailable()` probes for the stock soname first (see §3), then legacy
names, without loading the library.

## 3. Native library loading & soname strategy

The stock LibreOffice-Android build ships **`liblo-native-code.so`**. Legacy
community builds used `libreoffice-core.so`. The loader must try:

1. `liblo-native-code` (stock, preferred)
2. `libreoffice-core` (legacy)

using `android.content.res.AssetManager` / `System.loadLibrary` in that order.

**soname warning:** if you re-link a custom `libc++_shared.so` against a
different libc++, the loader will fail with
`UnsatisfiedLinkError` *at load time* (DT_NEEDED resolution). Keep the
libc++_shared.so you ship **bit-identical** to the one used when the main
engine `.so` was linked; do not mix toolchains.

Per-ABI `.so` drop-in layout (see `app/src/main/jniLibs/README.md`):

```
jniLibs/<abi>/
  liblokit-facade.so     (your C facade)
  liblo-native-code.so   (stock LO-Android engine)
  libc++_shared.so
  libnss3.so + rest of the dependency chain
```

## 4. Getting the binaries

1. Build (or obtain) a LibreOffice Android port for your ABIs
   (`arm64-v8a` first, then `armeabi-v7a`; `x86_64` for emulator dev).
2. Copy the engine `.so` + dependency chain into `app/src/main/jniLibs/<abi>/`.
3. Add your C facade (`liblokit-facade.so`) built from the same toolchain.
4. Keep total APK size in mind — the LO-Android engine is large; consider
   `jniLibs.useLegacyPackaging = true` only if you hit split-APK size issues.

## 5. `userdata` / `workdir` strategy (Android)

LOKit needs a writable `UserInstallation` directory and a work directory:

```
context.filesDir/lokit/
  user/     -> UserInstallation (profile, settings)
  work/     -> temp workdir (lock files, temp imports)
  resources -> bundled LO resources unpacked from assets (fonts, icons)
```

- Create these on first run, before `lo_engine_init`.
- `LOKIT_EVENT_START_FILE_*` events may reference temp files under `work/`.
- Never point `UserInstallation` at external/SAF storage (permissions +
  reliability).

## 6. Event handling on Android (Looper)

LOKit delivers `LOKIT_EVENT_*` from its internal thread. The C facade callback
must **dispatch to the Android main Looper** via a `Handler(Looper.getMainLooper())`
posted from the JNI boundary:

- Never block the LOKit callback thread (it stalls the engine).
- UI-facing events (undo state, view changes, status) marshal to main.
- `LOKIT_EVENT_EXIT` -> clean `lo_engine_exit` + release the facade handle.

## 7. Memory budget (Phase 6 alignment)

See `docs/PHASE6_MEMORY_PLAN.md`. Native mode multiplies the memory surface
(engine + document tree + rendering). Keep:

- One engine instance per process (LOKit is not multi-process safe).

- Cap open documents (recommend max 3 concurrent).
- `lo_engine_exit()` on app backgrounding > 30 s to reclaim the engine.
- Monitor with the in-app memory screen; native RSS should stay under
  ~350 MB on a 6 GB device for the bundled sample files.

## 8. Licensing & compliance

The LibreOffice engine is **MPL-2.0** (with AGPL/GPL components in some ports).
Shaping obligations for this repo:

- Ship the `LICENSE` (MPL-2.0) and a `NOTICE` listing the LO components.
- Document the exact LO version + build commit in release notes.
- If you distribute a *modified* LO-Android build, publish the source of your
  modifications (see `KEYSTORE_GUIDE.md` conventions for release artifacts).
- GPL/AGPL parts: verify against the specific port you ship; prefer the
  MPL-2.0-only Android port to keep obligations minimal.

## 9. Rollout checklist

- [ ] ABI: `arm64-v8a` verified on a real device (emulator first for x86_64).
- [ ] `liblo-native-code` loads; no `UnsatisfiedLinkError` (check §3 soname note).
- [ ] `lo_engine_init` returns success; sample files open and render.
- [ ] Event loop posts to main Looper; no ANRs on open/save.
- [ ] Undo/redo parity with the simulated bridge (same public API).
- [ ] Memory screen stays green for the bundled samples (see §7).
- [ ] `NOTICE` + `LICENSE` present in the APK (`assets/legal/`).
- [ ] Feature flag: `lokit.native` defaults **off** until the checklist passes.

## 10. Feature-flag gate

```kotlin
// BuildConfig / flavor flag
LOKIT_NATIVE = false   // flip per-flavor; default false = simulated
```

The flag is read **once** at engine creation; it cannot change mid-session.
