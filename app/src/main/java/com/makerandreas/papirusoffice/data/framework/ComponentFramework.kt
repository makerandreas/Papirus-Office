package com.makerandreas.papirusoffice.data.framework

/**
 * PropertyValue struct matching com.sun.star.beans.PropertyValue
 */
data class PropertyValue(val name: String, var value: Any?)

/**
 * MediaDescriptor is simply a list of PropertyValues in LibreOffice.
 */
typealias MediaDescriptor = List<PropertyValue>

interface XEventListener {
    fun disposing(source: EventObject)
}

data class EventObject(val source: Any)

/**
 * Base interface for all disposable components.
 * Matches com.sun.star.lang.XComponent
 */
interface XComponent {
    fun dispose()
    fun addEventListener(listener: XEventListener)
    fun removeEventListener(listener: XEventListener)
}

/**
 * Document model interface.
 * Matches com.sun.star.frame.XModel
 */
interface XModel : XComponent {
    val url: String
    val args: MediaDescriptor
    
    fun attachResource(url: String, args: MediaDescriptor): Boolean
    fun getCurrentController(): XController?
    fun setCurrentController(controller: XController)
    fun connectController(controller: XController)
    fun disconnectController(controller: XController)
    fun lockControllers()
    fun unlockControllers()
    fun hasControllersLocked(): Boolean
}

/**
 * Controller interface managing the view.
 * Matches com.sun.star.frame.XController
 */
interface XController : XComponent {
    fun getFrame(): XFrame?
    fun attachFrame(frame: XFrame)
    fun getModel(): XModel?
    fun attachModel(model: XModel): Boolean
    fun suspend(suspend: Boolean): Boolean
    fun getViewData(): Any?
    fun restoreViewData(data: Any)
}

/**
 * Frame interface connecting controller to window.
 * Matches com.sun.star.frame.XFrame
 */
interface XFrame : XComponent {
    var name: String
    
    fun setComponent(componentWindow: Any?, controller: XController): Boolean
    fun getComponentWindow(): Any?
    fun getController(): XController?
    
    fun setCreator(creator: XFramesSupplier)
    fun getCreator(): XFramesSupplier?
    fun findFrame(targetFrameName: String, searchFlags: Int): XFrame?
    fun isTop(): Boolean
    
    fun activate()
    fun deactivate()
    fun isActive(): Boolean
}

/**
 * Frames supplier, typically supported by desktop or parent frames.
 * Matches com.sun.star.frame.XFramesSupplier
 */
interface XFramesSupplier : XFrame {
    fun getFrames(): XFrames
    fun getActiveFrame(): XFrame?
    fun setActiveFrame(frame: XFrame)
}

/**
 * Container for frames.
 * Matches com.sun.star.frame.XFrames
 */
interface XFrames {
    fun append(frame: XFrame)
    fun remove(frame: XFrame)
    fun queryFrames(searchFlags: Int): List<XFrame>
}

/**
 * Interface to load components into frames.
 * Matches com.sun.star.frame.XComponentLoader
 */
interface XComponentLoader {
    fun loadComponentFromURL(
        url: String,
        targetFrameName: String,
        searchFlags: Int,
        args: MediaDescriptor
    ): XComponent?
}

/**
 * Search flags used by findFrame and loadComponentFromURL
 */
object FrameSearchFlag {
    const val AUTO = 0
    const val PARENT = 1
    const val SELF = 2
    const val CHILDREN = 4
    const val CREATE = 8
    const val SIBLINGS = 16
    const val TASKS = 32
    const val ALL = 23 // CHILDREN | SIBLINGS | PARENT | SELF
}
