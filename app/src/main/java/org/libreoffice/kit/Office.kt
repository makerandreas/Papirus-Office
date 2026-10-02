// SPDX-License-Identifier: MPL-2.0
package org.libreoffice.kit

import java.nio.ByteBuffer

class Office(private var handle: ByteBuffer) {

    private external fun documentLoadNative(url: String): ByteBuffer?

    fun documentLoad(url: String): Document? {
        val docHandle = documentLoadNative(url) ?: return null
        return Document(docHandle)
    }

    external fun getError(): String?

    external fun destroy()

    external fun destroyAndExit()

    external fun bindMessageCallback()

    external fun setDocumentPassword(url: String, password: String?)

    external fun setOptionalFeatures(features: Long)

    interface MessageCallback {
        fun messageRetrieved(signalNumber: Int, payload: String)
    }

    companion object {
        const val LOK_FEATURE_DOCUMENT_PASSWORD: Long = 1L shl 0
        const val LOK_FEATURE_DOCUMENT_PASSWORD_TO_MODIFY: Long = 1L shl 1
        const val LOK_FEATURE_PART_IN_INVALIDATION_CALLBACK: Long = 1L shl 2
        const val LOK_FEATURE_NO_TILED_ANNOTATIONS: Long = 1L shl 3
        const val LOK_FEATURE_RANGE_HEADERS: Long = 1L shl 4

        const val LOK_CALLBACK_DOCUMENT_PASSWORD: Int = 16
        const val LOK_CALLBACK_DOCUMENT_PASSWORD_TO_MODIFY: Int = 17
    }
}
