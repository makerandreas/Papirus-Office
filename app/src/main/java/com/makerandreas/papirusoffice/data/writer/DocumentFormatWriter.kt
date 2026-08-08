package com.makerandreas.papirusoffice.data.writer

import com.makerandreas.papirusoffice.data.OfficeDocument

interface DocumentFormatWriter {
    fun write(document: OfficeDocument): ByteArray
}
