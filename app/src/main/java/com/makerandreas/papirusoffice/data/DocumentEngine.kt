package com.makerandreas.papirusoffice.data

// Mengimpor modul inti langsung dari jarfile LibreOffice yang kita pasang
import com.sun.star.comp.helper.Bootstrap
import com.sun.star.uno.XComponentContext

class DocumentEngine {

    fun initializeContext(): XComponentContext? {
        return try {
            // Membuat konteks komponen awal untuk menjembatani API Java ke sistem
            val localContext = Bootstrap.createInitialComponentContext(null)
            localContext
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
