package com.makerandreas.papirusoffice.data

import android.net.Uri
import java.io.File

sealed class DocumentReference {
    abstract val displayName: String
    abstract val mimeType: String?
    abstract val storagePath: String

    data class SafUri(
        val uri: Uri,
        override val displayName: String,
        override val mimeType: String? = null
    ) : DocumentReference() {
        override val storagePath: String = uri.toString()
    }

    data class LocalFile(
        val file: File,
        override val displayName: String = file.name,
        override val mimeType: String? = null
    ) : DocumentReference() {
        override val storagePath: String = file.absolutePath
    }

    data class Memory(
        val id: String,
        override val displayName: String,
        override val mimeType: String? = null
    ) : DocumentReference() {
        override val storagePath: String = "memory://$id"
    }

    data class Cloud(
        val cloudUri: String,
        override val displayName: String,
        override val mimeType: String? = null
    ) : DocumentReference() {
        override val storagePath: String = cloudUri
    }
}
