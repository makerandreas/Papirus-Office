package com.example

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * A test library dropped from `app/build.gradle.kts` while the test sources
 * still import it fails `:app:compileDebugUnitTestKotlin`, and the failure
 * lands as a wall of `Unresolved reference` lines naming every use site rather
 * than the one dependency line that caused them. This guard names the line.
 */
class TestDependencyGuardTest {

    private class GuardedLibrary(
        /** Import prefix as it appears in a test source file. */
        val importPrefix: String,
        /** Version-catalog accessor the build script must declare. */
        val catalogAccessor: String
    )

    private val guardedLibraries = listOf(
        GuardedLibrary("io.mockk.", "libs.mockk"),
        GuardedLibrary("org.robolectric.", "libs.robolectric"),
        GuardedLibrary("com.github.takahirom.roborazzi.", "libs.roborazzi"),
        GuardedLibrary("kotlinx.coroutines.test.", "libs.kotlinx.coroutines.test"),
        GuardedLibrary("androidx.compose.ui.test.", "libs.androidx.compose.ui.test.junit4")
    )

    /** Gradle runs unit tests from the `app` module directory. */
    private fun resolve(relativeToModule: String, relativeToRoot: String): File {
        return listOf(File(relativeToModule), File(relativeToRoot))
            .firstOrNull { it.exists() }
            ?: error("neither $relativeToModule nor $relativeToRoot exists from ${File(".").absolutePath}")
    }

    private fun testSources(): List<File> {
        val root = resolve("src/test/java", "app/src/test/java")
        return root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    @Test
    fun everyGuardedTestLibraryTheSuiteImportsIsDeclaredInBuildScript() {
        val buildScript = resolve("build.gradle.kts", "app/build.gradle.kts").readText()
        val declared = buildScript.lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("testImplementation(") }
            .toList()

        for (library in guardedLibraries) {
            val importers = testSources()
                .filter { file ->
                    file.useLines { lines ->
                        lines.any { it.trim().startsWith("import ${library.importPrefix}") }
                    }
                }
                .map { it.name }
                .sorted()
            if (importers.isEmpty()) continue

            val missing = declared.none { library.catalogAccessor in it }
            assertTrue(
                "${importers.size} test file(s) import ${library.importPrefix}* " +
                    "(${importers.take(5).joinToString(", ")}${if (importers.size > 5) ", ..." else ""}) " +
                    "but no testImplementation line in app/build.gradle.kts declares " +
                    "${library.catalogAccessor}. Add it back; do not add an import to the test file.",
                !missing
            )
        }
    }

    @Test
    fun everyCatalogAccessorUsedByTheBuildScriptExistsInTheVersionCatalog() {
        val buildScript = resolve("build.gradle.kts", "app/build.gradle.kts").readText()
        val catalog = resolve("../gradle/libs.versions.toml", "gradle/libs.versions.toml").readText()
        val catalogAliases = Regex("""^([A-Za-z0-9._-]+)\s*=\s*\{\s*group""", RegexOption.MULTILINE)
            .findAll(catalog)
            .map { it.groupValues[1] }
            .toSet()

        val used = Regex("""libs\.([A-Za-z0-9._]+)""")
            .findAll(buildScript)
            .map { it.groupValues[1] }
            .distinct()
            .toList()

        for (accessor in used) {
            // `libs.plugins.*` and `libs.versions.*` are not library aliases.
            if (accessor.startsWith("plugins.") || accessor.startsWith("versions.")) continue
            val alias = accessor.replace('.', '-')
            assertTrue(
                "app/build.gradle.kts uses libs.$accessor but gradle/libs.versions.toml " +
                    "declares no library alias '$alias'",
                alias in catalogAliases
            )
        }
    }
}
