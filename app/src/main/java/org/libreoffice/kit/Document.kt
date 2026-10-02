// SPDX-License-Identifier: MPL-2.0
package org.libreoffice.kit

import java.nio.ByteBuffer

class Document(val handle: ByteBuffer) {

    external fun destroy()

    external fun bindMessageCallback()

    external fun getPart(): Int

    external fun setPart(part: Int)

    external fun getParts(): Int

    external fun getPartName(part: Int): String?

    external fun setPartMode(partMode: Int)

    external fun getDocumentHeight(): Long

    external fun getDocumentWidth(): Long

    private external fun getDocumentTypeNative(): Int

    fun getDocumentType(): Int = getDocumentTypeNative()

    external fun paintTileNative(
        buffer: ByteBuffer,
        canvasWidth: Int,
        canvasHeight: Int,
        tilePosX: Int,
        tilePosY: Int,
        tileWidth: Int,
        tileHeight: Int
    )

    external fun saveAs(url: String, format: String, options: String): Int

    external fun initializeForRendering()

    external fun setClientZoom(
        tilePixelWidth: Int,
        tilePixelHeight: Int,
        tileTwipWidth: Int,
        tileTwipHeight: Int
    )

    external fun postKeyEvent(type: Int, charCode: Int, keyCode: Int)

    external fun postMouseEvent(type: Int, x: Int, y: Int, count: Int, buttons: Int, modifier: Int)

    external fun postUnoCommand(command: String, arguments: String?, notifyWhenFinished: Boolean)

    external fun setTextSelection(type: Int, x: Int, y: Int)

    external fun getTextSelection(mimeType: String): String?

    external fun paste(mimeType: String, data: String): Boolean

    external fun resetSelection()

    external fun setGraphicSelection(type: Int, x: Int, y: Int)

    external fun getCommandValues(command: String): String?

    external fun getPartPageRectangles(): String?

    interface MessageCallback {
        fun messageRetrieved(signalNumber: Int, payload: String)
    }

    companion object {
        const val PART_MODE_SLIDES: Int = 0
        const val PART_MODE_NOTES: Int = 1
        const val PART_MODE_COMBINED: Int = 2

        const val DOCTYPE_TEXT: Int = 0
        const val DOCTYPE_SPREADSHEET: Int = 1
        const val DOCTYPE_PRESENTATION: Int = 2
        const val DOCTYPE_DRAWING: Int = 3
        const val DOCTYPE_OTHER: Int = 4
    }
}
