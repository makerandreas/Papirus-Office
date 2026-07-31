package com.makerandreas.papirusoffice.data.framework

import android.util.Log

/**
 * LibreOffice SDK Chapter 42 - Sending E-mail Framework Simulation.
 * Implements SimpleSystemMail, SimpleCommandMail, MailServiceProvider,
 * JavaMail-like structures, and transferable mime-type handlers.
 */

// --- DATA TRANSFER & MIME TYPES ---

data class DataFlavor(
    val mimeType: String,
    val humanPresentableName: String,
    val dataType: Any
)

interface XTransferable {
    @Throws(Exception::class)
    fun getTransferData(df: DataFlavor): Any
    fun getTransferDataFlavors(): Array<DataFlavor>
    fun isDataFlavorSupported(df: DataFlavor): Boolean
}

class TextTransferable(private val text: String) : XTransferable {
    private val unicodeMimeType = "text/plain;charset=utf-16"

    override fun getTransferData(df: DataFlavor): Any {
        if (!df.mimeType.equals(unicodeMimeType, ignoreCase = true)) {
            throw Exception("Unsupported data flavor")
        }
        return text
    }

    override fun getTransferDataFlavors(): Array<DataFlavor> {
        return arrayOf(
            DataFlavor(unicodeMimeType, "Unicode Text", String::class.java)
        )
    }

    override fun isDataFlavorSupported(df: DataFlavor): Boolean {
        return df.mimeType.equals(unicodeMimeType, ignoreCase = true)
    }
}

class FileTransferable(private val filePath: String) : XTransferable {
    private var mimeType: String = "application/octet-stream"
    private var fileData: ByteArray = ByteArray(0)

    init {
        mimeType = getMimeType(filePath)
        try {
            // Simulated reading bytes
            fileData = "[Simulated binary data of $filePath]".toByteArray()
        } catch (e: Exception) {
            Log.e("FileTransferable", "Could not read bytes from $filePath")
        }
    }

    private fun getMimeType(path: String): String {
        return when {
            path.endsWith(".odt", ignoreCase = true) -> "application/vnd.oasis.opendocument.text"
            path.endsWith(".ods", ignoreCase = true) -> "application/vnd.oasis.opendocument.spreadsheet"
            path.endsWith(".odp", ignoreCase = true) -> "application/vnd.oasis.opendocument.presentation"
            path.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
            path.endsWith(".png", ignoreCase = true) -> "image/png"
            path.endsWith(".jpg", ignoreCase = true) || path.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
            else -> "application/octet-stream"
        }
    }

    override fun getTransferData(df: DataFlavor): Any {
        if (!df.mimeType.equals(mimeType, ignoreCase = true)) {
            throw Exception("Unsupported data flavor")
        }
        return fileData
    }

    override fun getTransferDataFlavors(): Array<DataFlavor> {
        return arrayOf(
            DataFlavor(mimeType, mimeType, ByteArray::class.java)
        )
    }

    override fun isDataFlavorSupported(df: DataFlavor): Boolean {
        return df.mimeType.equals(mimeType, ignoreCase = true)
    }
}


// --- SIMPLE SYSTEM MAIL (OS Default Email Client via Mailto/Intents) ---

object SimpleMailClientFlags {
    const val DEFAULTS: Int = 0
    const val NO_USER_INTERFACE: Int = 1
    const val NO_RECORD_SEND: Int = 2
}

interface XSimpleMailMessage {
    fun getRecipient(): String
    fun setRecipient(recipient: String)
    fun getSubject(): String
    fun setSubject(subject: String)
    fun getAttachement(): Array<String>
    fun setAttachement(attachments: Array<String>)
}

interface XSimpleMailMessage2 : XSimpleMailMessage {
    fun getBody(): String
    fun setBody(body: String)
}

class SimpleMailMessage : XSimpleMailMessage2 {
    private var recipient: String = ""
    private var subject: String = ""
    private var body: String = ""
    private var attachments: Array<String> = emptyArray()

    override fun getRecipient(): String = recipient
    override fun setRecipient(recipient: String) { this.recipient = recipient }

    override fun getSubject(): String = subject
    override fun setSubject(subject: String) { this.subject = subject }

    override fun getAttachement(): Array<String> = attachments
    override fun setAttachement(attachments: Array<String>) { this.attachments = attachments }

    override fun getBody(): String = body
    override fun setBody(body: String) { this.body = body }
}

interface XSimpleMailClient {
    fun createSimpleMailMessage(): XSimpleMailMessage
    fun sendSimpleMailMessage(message: XSimpleMailMessage, flags: Int)
}

interface XSimpleMailClientSupplier {
    fun querySimpleMailClient(): XSimpleMailClient
}


// --- MAIL SERVICE PROVIDER (SMTP Socket/Python script mock-up) ---

enum class MailServiceType {
    SMTP, POP3, IMAP
}

interface XConnectionListener {
    fun connected(e: EventObject)
    fun disconnected(e: EventObject)
    fun disposing(e: EventObject)
}

interface XAuthenticator {
    fun getUserName(): String
    fun getPassword(): String
}

interface XMailService {
    fun addConnectionListener(listener: XConnectionListener)
    fun removeConnectionListener(listener: XConnectionListener)
    fun connect(context: XComponentContext, authenticator: XAuthenticator)
    fun disconnect()
    fun isConnected(): Boolean
}

data class MailAttachment(
    val transferable: XTransferable,
    val readableName: String
)

interface XMailMessage {
    var recipient: String
    var sender: String
    var subject: String
    var body: XTransferable
    val attachments: MutableList<MailAttachment>
    fun addAttachment(attachment: MailAttachment)
}

class MailMessage(
    override var recipient: String,
    override var sender: String,
    override var subject: String,
    override var body: XTransferable
) : XMailMessage {
    override val attachments: MutableList<MailAttachment> = mutableListOf()

    override fun addAttachment(attachment: MailAttachment) {
        attachments.add(attachment)
    }

    companion object {
        fun create(
            context: XComponentContext?,
            recipient: String,
            sender: String,
            subject: String,
            body: XTransferable
        ): XMailMessage {
            return MailMessage(recipient, sender, subject, body)
        }
    }
}

interface XSmtpService : XMailService {
    fun sendMailMessage(message: XMailMessage)
}

interface XMailServiceProvider {
    fun create(type: MailServiceType): XMailService
}


// --- SMTP SERVICE IMPLEMENTATION (Simulated Mail Server) ---

class SMTPMailService : XSmtpService {
    private val listeners = mutableListOf<XConnectionListener>()
    private var connected = false
    private var serverName = ""
    private var port = 25
    private var user = ""

    override fun addConnectionListener(listener: XConnectionListener) {
        listeners.add(listener)
    }

    override fun removeConnectionListener(listener: XConnectionListener) {
        listeners.remove(listener)
    }

    override fun connect(context: XComponentContext, authenticator: XAuthenticator) {
        serverName = (context.getValueByName("ServerName") as? String) ?: "localhost"
        port = (context.getValueByName("Port") as? Int) ?: 25
        user = authenticator.getUserName()
        connected = true

        val event = EventObject(this)
        listeners.forEach { it.connected(event) }
    }

    override fun disconnect() {
        connected = false
        val event = EventObject(this)
        listeners.forEach { it.disconnected(event) }
    }

    override fun isConnected(): Boolean = connected

    override fun sendMailMessage(message: XMailMessage) {
        if (!connected) {
            throw Exception("SMTP client is not connected.")
        }
        Log.d("SMTPMailService", "Mail sent successfully via SMTP host $serverName:$port from $user to ${message.recipient}")
    }
}

class MailServiceProviderImpl : XMailServiceProvider {
    override fun create(type: MailServiceType): XMailService {
        return when (type) {
            MailServiceType.SMTP -> SMTPMailService()
            else -> throw Exception("Only SMTP is supported in simulated MailServiceProvider.")
        }
    }
}
