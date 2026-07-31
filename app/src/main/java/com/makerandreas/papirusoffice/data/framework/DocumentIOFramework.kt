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

