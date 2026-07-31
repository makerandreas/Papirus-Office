package com.makerandreas.papirusoffice.data

data class ParserReport(
    val format: String = "Unknown",
    val warnings: List<String> = emptyList(),
    val unsupported: List<String> = emptyList(),
    val repaired: List<String> = emptyList(),
    val elapsed: Long = 0L,
    val parserVersion: Int = 1
)
