package com.makerandreas.papirusoffice.data.framework

// ---------------------------------------------------------
// Number Formats
// ---------------------------------------------------------

interface XNumberFormatsSupplier {
    val numberFormats: XNumberFormats
}

interface XNumberFormats {
    fun getByKey(key: Int): Any // Returns XPropertySet of NumberFormatProperties
    fun queryKeys(type: Short, locale: Locale, create: Boolean): IntArray
    fun queryKey(format: String, locale: Locale, scan: Boolean): Int
    fun addNew(format: String, locale: Locale): Int
    fun addNewConverted(format: String, locale: Locale, newLocale: Locale): Int
    fun removeByKey(key: Int)
    fun generateFormat(baseKey: Int, locale: Locale, thousands: Boolean, red: Boolean, decimals: Short, leading: Short): String
}

interface XNumberFormatTypes {
    fun getStandardIndex(locale: Locale): Int
    fun getStandardFormat(type: Short, locale: Locale): Int
    fun getFormatIndex(index: Short, locale: Locale): Int
    fun isTypeCompatible(oldType: Short, newType: Short): Boolean
    fun getFormatForLocale(key: Int, locale: Locale): Int
}

interface XNumberFormatter {
    fun attachNumberFormatsSupplier(supplier: XNumberFormatsSupplier)
    fun getNumberFormatsSupplier(): XNumberFormatsSupplier
    fun detectNumberFormat(key: Int, string: String): Int
    fun convertStringToNumber(key: Int, string: String): Double
    fun convertNumberToString(key: Int, value: Double): String
    fun formatString(key: Int, string: String): String
    fun getInputString(key: Int, value: Double): String
}

interface XNumberFormatPreviewer {
    fun convertNumberToPreviewString(format: String, value: Double, locale: Locale, allowEnglish: Boolean): String
}

object NumberFormat {
    const val ALL: Short = 0
    const val DEFINED: Short = 1
    const val DATE: Short = 2
    const val TIME: Short = 4
    const val CURRENCY: Short = 8
    const val NUMBER: Short = 16
    const val SCIENTIFIC: Short = 32
    const val FRACTION: Short = 64
    const val PERCENT: Short = 128
    const val TEXT: Short = 256
    const val DATETIME: Short = 6
    const val LOGICAL: Short = 1024
    const val UNDEFINED: Short = 2048
}

data class Locale(
    var Language: String = "",
    var Country: String = "",
    var Variant: String = ""
)
