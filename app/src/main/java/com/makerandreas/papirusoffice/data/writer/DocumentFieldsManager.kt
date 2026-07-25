package com.makerandreas.papirusoffice.data.writer

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Field types supported by LibreOffice Writer / Papirus Engine.
 */
sealed class SwFieldType(val name: String) {
    class PageNumberFieldType(val pageOffset: Int = 0) : SwFieldType("PageNumber")
    class PageCountFieldType : SwFieldType("PageCount")
    class DocTitleFieldType : SwFieldType("DocTitle")
    class DateTimeFieldType(val formatPattern: String = "yyyy-MM-dd HH:mm") : SwFieldType("DateTime")
    class CustomVariableFieldType(val variableName: String, var value: String) : SwFieldType("CustomVariable")
}

/**
 * Manages SwFieldType registrations and caches expanded field evaluations (e.g. Page Numbers).
 */
class DocumentFieldsManager {

    private val fieldTypesRegistry: MutableMap<String, SwFieldType> = mutableMapOf()
    private val formatFieldsList: MutableList<SwFormatField> = mutableListOf()
    private val fieldEvaluationCache: MutableMap<String, String> = mutableMapOf()

    init {
        // Register standard field types
        registerFieldType(SwFieldType.PageNumberFieldType())
        registerFieldType(SwFieldType.PageCountFieldType())
        registerFieldType(SwFieldType.DocTitleFieldType())
        registerFieldType(SwFieldType.DateTimeFieldType())
    }

    fun registerFieldType(fieldType: SwFieldType) {
        fieldTypesRegistry[fieldType.name] = fieldType
    }

    fun registerFormatField(field: SwFormatField) {
        formatFieldsList.add(field)
        clearCache()
    }

    fun unregisterFormatField(field: SwFormatField) {
        formatFieldsList.remove(field)
        clearCache()
    }

    fun evaluateField(
        field: SwFormatField,
        contextDocTitle: String = "Untitled Document",
        currentPage: Int = 1,
        totalPages: Int = 1
    ): String {
        val cacheKey = "${field.fieldType.name}_${currentPage}_${totalPages}_${contextDocTitle}"
        fieldEvaluationCache[cacheKey]?.let { return it }

        val expandedValue = when (val type = field.fieldType) {
            is SwFieldType.PageNumberFieldType -> (currentPage + type.pageOffset).toString()
            is SwFieldType.PageCountFieldType -> totalPages.toString()
            is SwFieldType.DocTitleFieldType -> contextDocTitle
            is SwFieldType.DateTimeFieldType -> {
                try {
                    val sdf = SimpleDateFormat(type.formatPattern, Locale.getDefault())
                    sdf.format(Date())
                } catch (e: Exception) {
                    Date().toString()
                }
            }
            is SwFieldType.CustomVariableFieldType -> type.value
        }

        fieldEvaluationCache[cacheKey] = expandedValue
        return expandedValue
    }

    fun evaluateAllFields(
        contextDocTitle: String = "Untitled Document",
        currentPage: Int = 1,
        totalPages: Int = 1
    ): Map<SwFormatField, String> {
        return formatFieldsList.associateWith { field ->
            evaluateField(field, contextDocTitle, currentPage, totalPages)
        }
    }

    fun clearCache() {
        fieldEvaluationCache.clear()
    }

    fun getAllFormatFields(): List<SwFormatField> = formatFieldsList.toList()
}
