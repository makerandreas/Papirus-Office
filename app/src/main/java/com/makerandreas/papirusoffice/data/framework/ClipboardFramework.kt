package com.makerandreas.papirusoffice.data.framework

import android.graphics.Bitmap
import android.util.Log

// --- LIBREOFFICE DATATRANSFER CLIPBOARD INTERFACES ---

interface XClipboardListener {
    fun changedContents(e: ClipboardEvent)
    fun disposing(e: EventObject)
}

data class ClipboardEvent(
    val clipboard: XClipboard,
    val contents: XTransferable
) : java.util.EventObject(clipboard)

interface XClipboardOwner {
    fun lostOwnership(board: XClipboard, contents: XTransferable)
}

interface XClipboard {
    fun getName(): String
    fun getContents(): XTransferable?
    fun setContents(contents: XTransferable, owner: XClipboardOwner?)
}

interface XSystemClipboard : XClipboard {
    fun addClipboardListener(listener: XClipboardListener)
    fun removeClipboardListener(listener: XClipboardListener)
}

// --- TRANSFERABLES SPECIFIC TO IMAGES & 2D ARRAYS ---

class ImageTransferable(private val bitmap: Bitmap) : XTransferable {
    private val bitmapClipMime = "application/x-openoffice-bitmap;windows_formatname=\"Bitmap\""

    override fun getTransferData(df: DataFlavor): Any {
        if (!df.mimeType.equals(bitmapClipMime, ignoreCase = true)) {
            throw Exception("Unsupported data flavor")
        }
        return bitmap
    }

    override fun getTransferDataFlavors(): Array<DataFlavor> {
        return arrayOf(
            DataFlavor(bitmapClipMime, "Bitmap", Bitmap::class.java)
        )
    }

    override fun isDataFlavorSupported(df: DataFlavor): Boolean {
        return df.mimeType.equals(bitmapClipMime, ignoreCase = true)
    }
}

class JArrayTransferable(private val array: Array<Array<Any>>) : XTransferable {
    private val arrayMime = "application/x-java-serialized-object;class=\"[[Ljava.lang.Object;\""

    override fun getTransferData(df: DataFlavor): Any {
        if (!df.mimeType.equals(arrayMime, ignoreCase = true)) {
            throw Exception("Unsupported data flavor")
        }
        return array
    }

    override fun getTransferDataFlavors(): Array<DataFlavor> {
        return arrayOf(
            DataFlavor(arrayMime, "2D Object Array", Array<Array<Any>>::class.java)
        )
    }

    override fun isDataFlavorSupported(df: DataFlavor): Boolean {
        return df.mimeType.equals(arrayMime, ignoreCase = true)
    }
}

// --- SYSTEM CLIPBOARD SIMULATION FACTORY ---

object SystemClipboard {
    private var instance: XSystemClipboard? = null

    fun create(context: XComponentContext?): XSystemClipboard {
        if (instance == null) {
            instance = object : XSystemClipboard {
                private var currentContents: XTransferable? = null
                private var currentOwner: XClipboardOwner? = null
                private val listeners = mutableListOf<XClipboardListener>()

                override fun getName(): String = "SystemClipboard"

                override fun getContents(): XTransferable? {
                    return currentContents
                }

                override fun setContents(contents: XTransferable, owner: XClipboardOwner?) {
                    val oldContents = currentContents
                    val oldOwner = currentOwner

                    currentContents = contents
                    currentOwner = owner

                    // Notify previous owner of ownership loss
                    if (oldOwner != null && oldContents != null) {
                        try {
                            oldOwner.lostOwnership(this, oldContents)
                        } catch (e: Exception) {
                            Log.e("SystemClipboard", "Error notifying clipboard owner", e)
                        }
                    }

                    // Notify listeners
                    val event = ClipboardEvent(this, contents)
                    listeners.forEach {
                        try {
                            it.changedContents(event)
                        } catch (e: Exception) {
                            Log.e("SystemClipboard", "Error notifying listener", e)
                        }
                    }
                }

                override fun addClipboardListener(listener: XClipboardListener) {
                    listeners.add(listener)
                }

                override fun removeClipboardListener(listener: XClipboardListener) {
                    listeners.remove(listener)
                }
            }
        }
        return instance!!
    }
}
