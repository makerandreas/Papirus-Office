package com.makerandreas.papirusoffice.data.framework

import java.io.InputStream

/**
 * Base interface for importing and exporting filters.
 * Matches com.sun.star.document.XFilter
 */
interface XFilter {
    fun filter(descriptor: MediaDescriptor): Boolean
    fun cancel()
}

/**
 * Importer interface.
 * Matches com.sun.star.document.XImporter
 */
interface XImporter : XFilter {
    fun setTargetDocument(doc: XComponent)
}

/**
 * Exporter interface.
 * Matches com.sun.star.document.XExporter
 */
interface XExporter : XFilter {
    fun setSourceDocument(doc: XComponent)
}

/**
 * Type detection interface.
 * Matches com.sun.star.document.XExtendedFilterDetection
 */
interface XExtendedFilterDetection {
    fun detect(descriptor: MutableList<PropertyValue>): String
}

/**
 * Service representing TypeDetection and FilterFactory
 */
interface XTypeDetection {
    fun queryTypeByURL(url: String): String
}

interface XFilterFactory {
    fun createFilter(filterName: String): XFilter?
}

/**
 * Metadata Interfaces for RDF support (ODF 1.2)
 * Matches com.sun.star.rdf.*
 */
interface XDocumentMetadataAccess {
    fun getMetadataGraphsWithType(type: String): List<String>
    fun addMetadataFile(fileName: String, types: List<String>): String
    fun importMetadataFile(format: Short, inStream: InputStream, fileName: String, baseURI: String, types: List<String>)
}

interface XMetadatable {
    fun ensureMetadataReference()
}
