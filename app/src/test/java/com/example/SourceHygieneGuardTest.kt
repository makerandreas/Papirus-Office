package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.max

/**
 * Plan 3A guard (plan-03 items 3.1-3.4, 3.8): the hygiene rules every later
 * PR must keep. Scans only the main source set, with comments masked out
 * first; test sources may quote literals freely. The grey-colour allowances
 * are recorded at guard birth (PR 12, 2026-09-24) per file; plan 3B owns
 * burning them down to zero.
 */
class SourceHygieneGuardTest {

    // region configuration: the tolerated carve-outs, as data

    /** path under the main java root -> maximum raw grey usages allowed today. */
    private val greyAllowances: Map<String, Int> = mapOf(
        "com/example/modules/slidia/SlidiaModule.kt" to 8,
        "com/example/ui/components/SwTextFormattingInspectorDialog.kt" to 7,
        "com/example/modules/cellina/CellinaModule.kt" to 7,
        "com/example/modules/pagella/PagellaModule.kt" to 4,
        // HomeSubpages: the colour-picker palette entries are document colours (plan-03 3.11)
        "com/example/modules/inky/HomeSubpages.kt" to 4,
        "com/example/ui/components/UniversalNavigatorSheet.kt" to 3,
        "com/example/modules/inky/InkyModule.kt" to 3,
        "com/example/ui/components/UniversalOdfSheet.kt" to 2,
        "com/example/ui/components/UniversalEmailSheet.kt" to 2,
        "com/example/ui/components/UniversalClipboardSheet.kt" to 2,
        "com/example/ui/components/OfficeUiComponents.kt" to 2,
        "com/example/ui/components/UniversalChartSheet.kt" to 1,
        "com/example/ui/components/CloudSyncBar.kt" to 1,
        "com/example/modules/inky/LayoutDrivenDocumentRenderer.kt" to 1
    )

    /** Literals that plan-03 item 3.3 replaced with resources; they must not come back. */
    private val bannedLiterals: List<String> = listOf(
        "Draft Dokumen Baru",
        "Inky_Dokumen",
        "Pengaturan aplikasi sukses direset",
        "Hubungkan Akun Google",
        "Ukuran font diubah ke",
        "Menempelkan sebagai",
        "Ubah Ukuran Font"
    )

    // endregion

    private data class Offender(val file: String, val line: Int, val rule: String, val excerpt: String) {
        fun describe(): String = "$file:$line [$rule] ${excerpt.take(90)}"
    }

    private fun mainSourceRoot(): File {
        val candidates = listOf(File("src/main/java"), File("app/src/main/java"))
        return candidates.firstOrNull { it.isDirectory }
            ?: error("app source root not found from ${File(".").absolutePath}")
    }

    /**
     * Blanks comment characters out (spaces, newlines kept) so offsets and
     * line numbers survive and no rule can fire on commented-out code.
     * Strings, raw strings and char literals are left intact.
     */
    private fun maskComments(src: String): String {
        val out = StringBuilder(src.length)
        var i = 0
        var mode = Mode.CODE
        while (i < src.length) {
            val c = src[i]
            val two = if (i + 1 < src.length) src.substring(i, i + 2) else ""
            when (mode) {
                Mode.CODE -> when {
                    two == "//" -> {
                        mode = Mode.LINE_COMMENT; out.append("  "); i += 2
                    }
                    two == "/*" -> {
                        mode = Mode.BLOCK_COMMENT; out.append("  "); i += 2
                    }
                    src.startsWith("\"\"\"", i) -> {
                        mode = Mode.RAW_STRING; out.append("\"\"\""); i += 3
                    }
                    c == '"' -> {
                        mode = Mode.STRING; out.append(c); i++
                    }
                    c == '\'' -> {
                        mode = Mode.CHAR; out.append(c); i++
                    }
                    else -> {
                        out.append(c); i++
                    }
                }
                Mode.LINE_COMMENT -> when {
                    c == '\n' -> {
                        mode = Mode.CODE; out.append(c); i++
                    }
                    else -> {
                        out.append(' '); i++
                    }
                }
                Mode.BLOCK_COMMENT -> when {
                    two == "*/" -> {
                        mode = Mode.CODE; out.append("  "); i += 2
                    }
                    else -> {
                        out.append(if (c == '\n') '\n' else ' '); i++
                    }
                }
                Mode.RAW_STRING -> when {
                    src.startsWith("\"\"\"", i) -> {
                        mode = Mode.CODE; out.append("\"\"\""); i += 3
                    }
                    else -> {
                        out.append(c); i++
                    }
                }
                Mode.STRING -> when {
                    c == '\\' -> {
                        out.append(c); i++
                        if (i < src.length) {
                            out.append(src[i]); i++
                        }
                    }
                    c == '"' -> {
                        mode = Mode.CODE; out.append(c); i++
                    }
                    c == '\n' -> {
                        // an unterminated plain string cannot span lines; recover
                        mode = Mode.CODE; out.append(c); i++
                    }
                    else -> {
                        out.append(c); i++
                    }
                }
                Mode.CHAR -> when {
                    c == '\\' -> {
                        out.append(c); i++
                        if (i < src.length) {
                            out.append(src[i]); i++
                        }
                    }
                    c == '\'' -> {
                        mode = Mode.CODE; out.append(c); i++
                    }
                    else -> {
                        out.append(c); i++
                    }
                }
            }
        }
        return out.toString()
    }

    private enum class Mode { CODE, LINE_COMMENT, BLOCK_COMMENT, RAW_STRING, STRING, CHAR }

    private fun kotlinFiles(root: File): List<File> =
        root.walkTopDown().filter { it.isFile && it.name.endsWith(".kt") }.sorted().toList()

    private fun lineOf(src: String, index: Int): Int =
        src.substring(0, index.coerceAtMost(src.length)).count { it == '\n' } + 1

    /** Indexes of Toast.makeText calls whose message argument starts with a string literal. */
    private fun toastLiteralSites(src: String): List<Int> {
        val sites = mutableListOf<Int>()
        val marker = "Toast.makeText("
        var i = 0
        while (true) {
            val start = src.indexOf(marker, i)
            if (start < 0) break
            val end = matchingParen(src, start + marker.length - 1)
            val args = src.substring(start + marker.length, max(start + marker.length, end))
            val second = splitTopLevel(args).getOrNull(1)?.trimStart()
            if (second != null && second.startsWith("\"")) sites += start
            i = end + 1
        }
        return sites
    }

    /** Index of the ')' matching the '(' at openParen (exclusive end). */
    private fun matchingParen(src: String, openParen: Int): Int {
        var depth = 1
        var j = openParen + 1
        while (j < src.length && depth > 0) {
            when {
                src[j] == '"' -> {
                    j++
                    while (j < src.length && src[j] != '"') {
                        if (src[j] == '\\') j++
                        j++
                    }
                }
                src[j] == '(' -> depth++
                src[j] == ')' -> depth--
            }
            j++
        }
        return j - 1
    }

    /** Splits call arguments at depth-1 commas, skipping string literals and nested brackets. */
    private fun splitTopLevel(args: String): List<String> {
        val parts = mutableListOf<String>()
        val buf = StringBuilder()
        var depth = 1
        var i = 0
        while (i < args.length) {
            val c = args[i]
            if (c == '"') {
                buf.append(c)
                i++
                while (i < args.length && args[i] != '"') {
                    buf.append(args[i])
                    if (args[i] == '\\') {
                        i++
                        if (i < args.length) buf.append(args[i])
                    }
                    i++
                }
                if (i < args.length) {
                    buf.append('"')
                    i++
                }
                continue
            }
            when (c) {
                '(', '{', '[' -> depth++
                ')', '}', ']' -> depth--
                ',' -> if (depth == 1) {
                    parts += buf.toString()
                    buf.clear()
                    i++
                    continue
                }
            }
            buf.append(c)
            i++
        }
        parts += buf.toString()
        return parts
    }

    private val contentDescriptionLiteral = Regex("contentDescription\\s*=\\s*\"")
    private val rawGrey = Regex("""Color\.(Gray|DarkGray|LightGray)\b""")

    /** Index ranges of every string literal (plain or raw) in comment-masked source. */
    private fun stringLiteralSpans(src: String): List<IntRange> {
        val spans = mutableListOf<IntRange>()
        var i = 0
        while (i < src.length) {
            val c = src[i]
            when {
                c == '"' && src.startsWith("\"\"\"", i) -> {
                    val end = src.indexOf("\"\"\"", i + 3)
                    if (end < 0) break
                    spans += i..(end + 2)
                    i = end + 3
                }
                c == '"' -> {
                    var j = i + 1
                    while (j < src.length && src[j] != '"') {
                        if (src[j] == '\\') j++
                        j++
                    }
                    spans += i..j.coerceAtMost(src.length - 1)
                    i = j + 1
                }
                else -> i++
            }
        }
        return spans
    }

    private fun scanTree(root: File): List<Offender> {
        val offenders = mutableListOf<Offender>()
        for (file in kotlinFiles(root)) {
            val rel = file.relativeTo(root).path
            val original = file.readText()
            val src = maskComments(original)
            for (start in toastLiteralSites(src)) {
                offenders += Offender(rel, lineOf(src, start), "toast-literal", src.substring(start, minOf(src.length, start + 80)))
            }
            for (m in contentDescriptionLiteral.findAll(src)) {
                offenders += Offender(rel, lineOf(src, m.range.first), "contentDescription-literal", src.substring(m.range.first, minOf(src.length, m.range.first + 80)))
            }
            val greyCount = rawGrey.findAll(src).count()
            val allowance = greyAllowances[rel] ?: 0
            if (greyCount > allowance) {
                offenders += Offender(rel, 1, "raw-grey", "$greyCount usages exceed the recorded allowance of $allowance")
            }
            for (span in stringLiteralSpans(src)) {
                if (span.last < src.length && src.substring(span.first + 1, span.last).contains('\u2014')) {
                    offenders += Offender(rel, lineOf(src, span.first), "em-dash", src.substring(span.first + 1, span.last))
                }
            }
            for (banned in bannedLiterals) {
                val idx = src.indexOf(banned)
                if (idx >= 0) offenders += Offender(rel, lineOf(src, idx), "banned-literal", banned)
            }
        }
        return offenders
    }

    @Test
    fun `main sources pass the plan 3A hygiene guard`() {
        val offenders = scanTree(mainSourceRoot())
        assertTrue(
            offenders.joinToString(prefix = "\n", separator = "\n") { it.describe() },
            offenders.isEmpty()
        )
    }

    @Test
    fun `the guard rules fire on synthetic offenders and stay silent on clean code`() {
        val temp = File(System.getProperty("java.io.tmpdir"), "hygiene-guard-${System.nanoTime()}")
        temp.deleteRecursively()
        val main = File(temp, "java").apply { mkdirs() }
        File(main, "Dirty.kt").writeText(
            """
            fun demo(context: android.content.Context, count: Int) {
                Toast.makeText(context, "Interpolated ${'$'}count", 0).show()
                Icon(Icons.Default.Add, contentDescription = "Add")
                val c = Color.DarkGray
                val note = "first — second"
                val revive = "Draft Dokumen Baru"
            }
            """.trimIndent()
        )
        File(main, "Clean.kt").writeText(
            """
            // Toast.makeText(context, "commented out", 0) must stay invisible
            /* Icon(Icons.Default.Add, contentDescription = "also commented") */
            fun demo(context: android.content.Context, count: Int) {
                Toast.makeText(context, R.string.done, 0).show()
                Toast.makeText(context, context.getString(R.string.items, count), 0).show()
                Toast.makeText(context, context.getString(R.string.width_pct, "%.2f".format(count)), 0).show()
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add))
                Icon(Icons.Default.Close, contentDescription = null)
                val paper = Color.White
                val quote = '"'
            }
            """.trimIndent()
        )
        try {
            val offenders = scanTree(temp)
            val dirty = offenders.filter { it.file.endsWith("Dirty.kt") }
            val clean = offenders.filter { it.file.endsWith("Clean.kt") }
            assertEquals(
                listOf("toast-literal", "contentDescription-literal", "raw-grey", "em-dash", "banned-literal"),
                dirty.map { it.rule }
            )
            assertTrue("clean file must stay silent: ${clean.map { it.describe() }}", clean.isEmpty())
        } finally {
            temp.deleteRecursively()
        }
    }
}
