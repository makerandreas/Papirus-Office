package com.makerandreas.papirusoffice.data.framework

// ---------------------------------------------------------
// Document Save and Print Interfaces
// ---------------------------------------------------------

interface XStorable {
    fun hasLocation(): Boolean
    fun getLocation(): String
    fun isReadonly(): Boolean
    fun store()
    fun storeAsURL(url: String, args: MediaDescriptor)
    fun storeToURL(url: String, args: MediaDescriptor)
}

interface XPrintable {
    fun getPrinter(): MediaDescriptor
    fun setPrinter(printer: MediaDescriptor)
    fun print(options: MediaDescriptor)
}

// ---------------------------------------------------------
// Page Properties for Printing
// ---------------------------------------------------------

data class PaperFormat(
    val A4: Short = 0,
    val A3: Short = 1,
    val LETTER: Short = 2,
    val LEGAL: Short = 3
    // ...
)

enum class PaperOrientation {
    PORTRAIT,
    LANDSCAPE
}
