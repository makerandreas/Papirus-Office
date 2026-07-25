package com.makerandreas.papirusoffice.data.framework

/**
 * SSO API for authentication
 * Matches com.sun.star.auth.*
 */
interface XSSOManagerFactory {
    fun getSSOManager(): XSSOManager?
}

interface XSSOManager {
    fun createInitiatorContext(sourceName: String, targetName: String, targetHost: String): XSSOInitiatorContext
    fun createAcceptorContext(): XSSOAcceptorContext
}

interface XSSOInitiatorContext {
    fun init(serverToken: ByteArray?): ByteArray
    fun getMutual(): Boolean
}

interface XSSOAcceptorContext {
    fun accept(clientToken: ByteArray): ByteArray
}

interface XSSOPasswordCache {
    fun addEntry(userName: String, password: String)
    fun getEntry(userName: String): String?
    fun removeEntry(userName: String)
}
