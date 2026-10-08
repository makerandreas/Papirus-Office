# Audit 024: the Compose 1.12 upgrade landed, lint moved to 9.4.1, and the 62-warning backlog

Date: 2026-10-09
Branch: `arena/574ad9fd-papirus-office`, merged to `main` as `6b2dfea`
Evidence tier: 3 (GitHub Actions run `37858250791`, head `c61e970`), unless stated

## What landed

PR #38 is merged. It carried the 58-warning cleanup, the dead-code sweep, the reminder
cap, and then the Compose upgrade the owner asked for on top.

The upgrade was not one line. Bumping the BOM alone failed before a line of Kotlin
compiled, because Compose 1.12.1's AAR metadata requires compileSdk 37 and AGP 9.1.1
caps at 36.1. The chain that shipped:

| what | before | after |
|---|---|---|
| Compose BOM | `2024.09.00` (Compose 1.7.0) | `2026.09.00` (Compose 1.12.1, Material3 1.4.0) |
| compileSdk | 36.1 | 37 |
| AGP | 9.1.1 | 9.4.1 |
| Gradle | 9.3.1 | 9.8.1 |
| targetSdk / minSdk | 36 / 24 | 36 / 24, unchanged |

All 3,238 Material3 and foundation call sites recompiled without change and all 435
tests pass on the new Compose. No product code changed to make it compile.

## The lint version moved, not the code

The AGP bump carried lint from 9.1.1 to 9.4.1, and 9.4.1 ships checks 9.1.1 did not
have. That is the whole story behind the error count going 0 to 78 and back to 0:

- 75 `LocalContextGetResourceValueCall`, all the same idiom of `context.getString(...)`
  on a context captured from composition. Fixed with `LocalResources.current`, the
  replacement androidx's own detector documents and its own quickfix applies.
- 2 `RememberInComposition`, `focusRequesters.getOrPut(...)` mutating during
  composition. Wrapped in `remember(index)`.
- 1 `NonObservableLocale`, `Locale.getDefault()` read in composition. Moved behind a
  plain non-composable `displayLocale(LocaleList)`.

None of the 78 was a regression. All were existing code meeting a new check.

## The 62-warning backlog

Final run: `lint 9.4.1: 0 errors, 62 warnings across 8 issue ids`.

The arithmetic against the previous 18:

- `-3`: `SwitchIntDef` fixed (the missing case was `TRIM_MEMORY_RUNNING_MODERATE`), and
  `AndroidGradlePluginVersion` x2 made moot by the toolchain bump.
- `+47`: five checks new in lint 9.4.1.
- `15` unchanged: `PluralsCandidate` 6, `VectorRaster` 6, `IconLocation` 3, all parked
  in audit 022 and still parked.

So the original 58 is finished. The new lint opened a second backlog. It splits into
three groups by what kind of decision each one needs.

### Group A: mechanical, no design decision, 35 sites

**`AutoboxingStateCreation`, 34 sites, severity Hint.** `mutableStateOf(0)` boxes an
`Int` on every write; `mutableIntStateOf(0)` does not. 28 want `mutableIntStateOf`,
6 want `mutableFloatStateOf`. This is a search-and-replace plus a type annotation
change at each site, and it is the single largest block. `InkyModule.kt` holds 9,
`HomeSubpages.kt` 4, `PagellaModule.kt` 3, `UniversalPrintSheet.kt` 3, and ten files
hold 1 or 2 each.

It is a Hint, not a Warning, so it does not affect the gate. It is worth doing because
the boxing is real and because it is 34 of the 62.

**`UseOfNonLambdaOffsetOverload`, 1 site.** `AboutScreen.kt:351` passes a State-backed
value to `Modifier.offset()`, which recomposes the layout node on every change. The
lambda overload `Modifier.offset { }` defers it to the draw phase. One line.

### Group B: real Compose 1.12 migrations, needs eyes on layout, 12 sites

**`ConfigurationScreenWidthHeight`, 6 sites.** `Configuration.screenWidthDp` and
`screenHeightDp` report the whole screen, not the container the composable is in. On a
foldable or in split screen the two disagree, and the composable lays out against the
wrong number. The replacement is `LocalWindowInfo.current.containerSize`, which is a
`DpSize` in pixels, not dp, so each site needs a density conversion rather than a rename.

`MainActivity.kt`:220, `InkyModule.kt`:147 and :148,
`LayoutDrivenDocumentRenderer.kt`:214, `PapirusTextToolbar.kt`:127 and :128.

Three of these six are in the document renderer and the text toolbar, which is where a
wrong container size is visible to the user. This is the group most likely to change
what the app draws, and it should not share a PR with the mechanical work.

**`ModifierParameter`, 4 sites.** `modifier: Modifier` is not the first optional
parameter, which is the Compose API guideline and matters because callers pass
`modifier` positionally by convention. `HomeSubpages.kt`:121,
`LayoutDrivenDocumentRenderer.kt`:119, `HomeDashboard.kt`:1096,
`InkyViewSettingsSubpage.kt`:31. Reordering a parameter changes every positional call
site, so each needs its callers checked.

**`FrequentlyChangingValue`, 2 sites.** `InkyModule.kt`:1499 and :1810 read a value
annotated `@FrequentlyChangingValue` directly in composition, which invalidates on every
change. The fix is to push the read down to the narrowest scope or into a lambda. Both
need the surrounding composition read before deciding which.

### Group C: content decisions, owner's call, 15 sites

**`PluralsCandidate`, 6 sites.** These are Indonesian-language strings where a count is
interpolated next to a noun, so `en_US` gets "1 minutes". Verified content:

| line | string |
|---|---|
| 130 | `save_every_minutes` = `%1$d minutes` |
| 184 | `viewer_status_words_chars` = `%1$d words, %2$d chars` |
| 444 | `toast_drop_cap_set_to_lines_lines` = `Drop Cap set to %1$d lines` |
| 600 | `toast_built_sdk_form_with_res_totalcontrolscreated` = `Built SDK Form with %1$d controls!` |
| 684 | `toast_auto_recovery_set_to_every_validminutes_minutes` = `Auto Recovery set to every %1$d minutes` |
| 793 | `toast_custom_show_customshowname_created_with_parsed_size` = `Custom Show '%2$s' created with %1$d slides!` |

Fixing means adding `<plurals>` entries and switching each `getString` call to
`resources.getQuantityString`, which changes 6 call sites as well as `strings.xml`.
Line 184 is the awkward one: it interpolates two counts, and only one can be plural.

**`VectorRaster`, 6 sites.** All six brand marks are 1024dp x 1024dp, verified from the
XML. Lint wants 200dp or smaller for icons that draw often. `ic_papirus_foreground.xml`
is the adaptive launcher foreground, where a large vector is normal and the warning is
arguably wrong. The other five are logos shown in an about screen or a module header.
Resizing them is a one-attribute change per file but it is a visual change, so it needs
a look on device, not just a green build.

**`IconLocation`, 3 sites.** Re-verified this pass:

| file | bytes | references |
|---|---|---|
| `img_pattern_blue_1785086580418.jpg` | 1,269,429 | 0 |
| `img_pattern_marble_1785086554711.jpg` | 542,033 | 0 |
| `img_pattern_roses_1785086568415.jpg` | 1,229,088 | 0 |

3.04 MB of APK with zero references anywhere in `app/src`. The warning is about them
sitting in a densityless `res/drawable/`, but the real finding is that they are unused.
Deleting them is the fix and it also removes the warning. This needs the owner's go
ahead because it is a deletion, and audit 022 parked it for exactly that reason.

## Recommended sequencing

Three PRs, in this order, so a visual regression is never mixed with a mechanical one:

1. **Group A, 35 sites.** `AutoboxingStateCreation` x34 and the `Modifier.offset` lambda.
   No visual change, no API change, largest warning reduction. Expected result: 62 to 27.
2. **Group B, 12 sites.** The `containerSize` migration, the `Modifier` parameter
   reorder, and the two `FrequentlyChangingValue` reads. This is the one that can change
   what the app draws, so it should be reviewed against the recorded goldens, which are
   the thing this audit's Compose upgrade unblocked. Expected result: 27 to 15.
3. **Group C, 15 sites.** Content decisions. Plurals needs the two-count case at line
   184 decided; the vectors need a look on device; the three JPGs need a delete
   decision. Expected result: 15 to 0.

Group A and Group C are independent and can be prepared in either order. Group B is the
one that should not be rushed, and it is the one where the goldens matter.

## What I could not verify here

No JDK, no Gradle, no Android SDK in the sandbox, so nothing in this audit was built or
run locally. Every count, file path, line number, byte size and reference count above
was read either from the CI lint report for run `37858250791` or from the working tree
with `grep` and `stat`, which is tier 1 for the file facts and tier 3 for the lint facts.

The golden PNGs could not be retrieved. Actions redirects artifact downloads to
`productionresultssa1.blob.core.windows.net`, which is outside this sandbox's allowlist,
so the 13 screenshots recorded by run `37858250791` are sitting in artifact
`11585053567` and cannot be committed from here. Recording them needs a workflow that
runs on a runner, which is what `.github/workflows/goldens.yml` added alongside this
audit does.
