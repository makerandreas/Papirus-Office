package com.example

/**
 * Historical char/line-heuristic paginator, retired from the shipped Editor in
 * favor of LayoutEngine pagination (audit-003 P0-1). Kept here as a stub so
 * tests can still exercise the old 46-lines-per-page behavior if ever needed.
 */
fun partitionTextToPages(
    text: String,
    charsPerLine: Int = 75,
    defaultLinesPerPage: Int = 46,
    targetPageCount: Int? = null
): List<String> {
    val rawText = text
    if (rawText.isEmpty()) return listOf("")
    
    // Split by explicit page breaks if present
    val explicitBreakRegex = Regex("""(?:\r?\n)*(?:---|===)?\s*(?:Page\s+\d+\s*\()?Page\s*Break\)?\s*(?:---|===)?(?:\r?\n)*|\u000C""", RegexOption.IGNORE_CASE)
    val explicitChunks = rawText.split(explicitBreakRegex)
    
    val pages = mutableListOf<String>()
    explicitChunks.forEach { chunk ->
        val rawParagraphs = chunk.split("\n")
        var currentPageLines = mutableListOf<String>()
        var currentLinesCount = 0
        
        val effectiveLinesPerPage = if (targetPageCount != null && targetPageCount > 0) {
            val totalApproxLines = rawParagraphs.sumOf { maxOf(1, (it.length + charsPerLine - 1) / charsPerLine) }
            maxOf(20, (totalApproxLines + targetPageCount - 1) / targetPageCount)
        } else {
            defaultLinesPerPage
        }
        
        rawParagraphs.forEach { paragraph ->
            val approxLinesInParagraph = maxOf(1, (paragraph.length + charsPerLine - 1) / charsPerLine)
            if (currentLinesCount + approxLinesInParagraph > effectiveLinesPerPage && currentPageLines.isNotEmpty()) {
                pages.add(currentPageLines.joinToString("\n").trim())
                currentPageLines = mutableListOf()
                currentLinesCount = 0
            }
            currentPageLines.add(paragraph)
            currentLinesCount += approxLinesInParagraph
        }
        if (currentPageLines.isNotEmpty()) {
            pages.add(currentPageLines.joinToString("\n").trim())
        }
    }
    
    return if (pages.isEmpty()) listOf(rawText) else pages
}
