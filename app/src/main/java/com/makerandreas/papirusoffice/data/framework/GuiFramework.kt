package com.makerandreas.papirusoffice.data.framework

// ---------------------------------------------------------
// Basic Windowing Interfaces (com.sun.star.awt)
// ---------------------------------------------------------

interface XWindow {
    fun setVisible(visible: Boolean)
    fun isVisible(): Boolean
    fun setEnable(enable: Boolean)
    fun isEnable(): Boolean
    fun setFocus()
    fun addWindowListener(listener: Any /* XWindowListener */)
    fun removeWindowListener(listener: Any /* XWindowListener */)
    fun addFocusListener(listener: XFocusListener)
    fun removeFocusListener(listener: XFocusListener)
    fun addKeyListener(listener: XKeyListener)
    fun removeKeyListener(listener: XKeyListener)
    fun addMouseListener(listener: XMouseListener)
    fun removeMouseListener(listener: XMouseListener)
    fun addMouseMotionListener(listener: XMouseMotionListener)
    fun removeMouseMotionListener(listener: XMouseMotionListener)
    fun addPaintListener(listener: XPaintListener)
    fun removePaintListener(listener: XPaintListener)
    fun getPosSize(): Rectangle
    fun setPosSize(x: Int, y: Int, width: Int, height: Int, flags: Short)
}

interface XWindow2 : XWindow {
    fun setOutputSize(size: Size)
    fun getOutputSize(): Size
    fun isForeground(): Boolean
    fun isVisible2(): Boolean
    fun isEnabled(): Boolean
    fun isActive(): Boolean
}

interface XWindowPeer : XComponent {
    val toolkit: XToolkit
    fun setPointer(pointer: XPointer)
    fun setBackground(color: Int)
    fun invalidate(flags: Short)
    fun invalidateRect(rect: Rectangle, flags: Short)
    fun setProperty(propertyName: String, value: Any)
    fun getProperty(propertyName: String): Any?
}

interface XVclWindowPeer : XWindowPeer

interface XTopWindow : XWindow {
    fun addTopWindowListener(listener: Any /* XTopWindowListener */)
    fun removeTopWindowListener(listener: Any /* XTopWindowListener */)
    fun toFront()
    fun toBack()
    fun setMenuBar(menuBar: XMenuBar)
}

interface XView {
    fun draw(x: Int, y: Int)
    var graphics: XGraphics?
    val size: Size
}

interface XLayoutConstraints {
    fun getMinimumSize(): Size
    fun getPreferredSize(): Size
    fun calcAdjustedSize(newSize: Size): Size
}

// ---------------------------------------------------------
// Control Containers & Dialogs
// ---------------------------------------------------------

interface XControlContainer {
    fun getControl(name: String): XControl?
    fun getControls(): Array<XControl>
    fun addControl(name: String, control: XControl)
    fun removeControl(control: XControl)
    fun setStatusIndicator(statusIndicator: Any /* XStatusIndicator */)
}

interface XDialog {
    fun setTitle(title: String)
    fun getTitle(): String
    fun execute(): Short
    fun endExecute()
}

// ---------------------------------------------------------
// Toolkit, Devices and Graphics
// ---------------------------------------------------------

interface XToolkit {
    fun getDesktopWindow(): XWindowPeer
    fun getWorkArea(): Rectangle
    fun createWindow(descriptor: WindowDescriptor): XWindowPeer
    fun createWindows(descriptors: Array<WindowDescriptor>): Array<XWindowPeer>
    fun createScreenCompatibleDevice(width: Int, height: Int): XDevice
}

data class WindowDescriptor(
    var Type: Short = 0, // WindowClass
    var WindowServiceName: String = "",
    var Parent: XWindowPeer? = null,
    var ParentIndex: Short = 0,
    var Bounds: Rectangle = Rectangle(),
    var WindowAttributes: Int = 0
)

object WindowClass {
    const val TOP: Short = 0
    const val MODALTOP: Short = 1
    const val CONTAINER: Short = 2
    const val SIMPLE: Short = 3
}

object WindowAttribute {
    const val SHOW: Int = 1
    const val MOVEABLE: Int = 2
    const val SIZEABLE: Int = 4
    const val CLOSEABLE: Int = 8
    const val BORDER: Int = 16
}

object VclWindowPeerAttribute {
    const val CLIPCHILDREN: Int = 256
}

interface XDevice {
    fun getInfo(): Any // DeviceInfo
    fun createGraphics(): XGraphics
    fun createDeviceCompatibleBitmap(width: Int, height: Int): Any // XBitmap
}

interface XGraphics {
    fun drawLine(x1: Int, y1: Int, x2: Int, y2: Int)
    fun drawRect(x: Int, y: Int, width: Int, height: Int)
    fun setClipRegion(region: XRegion)
    fun setRasterOp(op: Int) // RasterOperation
}

interface XRegion {
    fun clear()
    fun unionRectangle(rect: Rectangle)
    fun intersectRectangle(rect: Rectangle)
    fun excludeRectangle(rect: Rectangle)
    fun xorRectangle(rect: Rectangle)
}

interface XPointer {
    fun setType(type: Int) // SystemPointer
    fun getType(): Int
}

object SystemPointer {
    const val ARROW: Int = 0
    const val CROSS: Int = 1
    const val HAND: Int = 2
    const val REFHAND: Int = 3
}

// ---------------------------------------------------------
// Events and Listeners
// ---------------------------------------------------------

open class ActionEvent(source: Any, val ActionCommand: String) : EventObject(source)

open class MouseEvent(
    source: Any,
    val Buttons: Short,
    val X: Int,
    val Y: Int,
    val ClickCount: Int
) : EventObject(source)

open class KeyEvent(
    source: Any,
    val KeyChar: Char,
    val KeyCode: Int,
    val KeyFunc: Int
) : EventObject(source)

open class FocusEvent(
    source: Any,
    val FocusFlags: Short,
    val NextFocus: Any?
) : EventObject(source)

object FocusChangeReason {
    const val TAB: Short = 1
}

open class PaintEvent(source: Any, val UpdateRect: Rectangle, val Count: Int) : EventObject(source)

open class AdjustmentEvent(source: Any, val Type: AdjustmentType, val Value: Int) : EventObject(source)

enum class AdjustmentType {
    ADJUST_ABS,
    ADJUST_LINE,
    ADJUST_PAGE
}

open class ItemEvent(source: Any, val ItemId: Int, val Selected: Int) : EventObject(source)

open class SpinEvent(source: Any, val dummy: Int = 0) : EventObject(source)

interface XActionListener : XEventListener {
    fun actionPerformed(event: ActionEvent)
}

interface XMouseListener : XEventListener {
    fun mousePressed(event: MouseEvent)
    fun mouseReleased(event: MouseEvent)
    fun mouseEntered(event: MouseEvent)
    fun mouseExited(event: MouseEvent)
}

interface XMouseMotionListener : XEventListener {
    fun mouseDragged(event: MouseEvent)
    fun mouseMoved(event: MouseEvent)
}

interface XKeyListener : XEventListener {
    fun keyPressed(event: KeyEvent)
    fun keyReleased(event: KeyEvent)
}

interface XFocusListener : XEventListener {
    fun focusGained(event: FocusEvent)
    fun focusLost(event: FocusEvent)
}

interface XPaintListener : XEventListener {
    fun windowPaint(event: PaintEvent)
}

interface XAdjustmentListener : XEventListener {
    fun adjustmentValueChanged(event: AdjustmentEvent)
}

interface XItemListener : XEventListener {
    fun itemStateChanged(event: ItemEvent)
}

interface XTextListener : XEventListener {
    fun textChanged(event: EventObject)
}

interface XSpinListener : XEventListener {
    fun up(event: SpinEvent)
    fun down(event: SpinEvent)
    fun first(event: SpinEvent)
    fun last(event: SpinEvent)
}

// ---------------------------------------------------------
// Specific Controls & Sub-Interfaces
// ---------------------------------------------------------

interface XFixedText : XControl {
    fun setText(text: String)
    fun getText(): String
}

interface XButton : XControl {
    fun setLabel(label: String)
    fun getLabel(): String
    fun addActionListener(listener: XActionListener)
    fun removeActionListener(listener: XActionListener)
}

interface XCheckBox : XControl {
    fun setState(state: Short)
    fun getState(): Short
    fun addItemListener(listener: XItemListener)
    fun removeItemListener(listener: XItemListener)
}

interface XRadioButton : XControl {
    fun setState(state: Short)
    fun getState(): Short
    fun addItemListener(listener: XItemListener)
    fun removeItemListener(listener: XItemListener)
}

interface XScrollBar : XControl {
    fun setValue(value: Int)
    fun getValue(): Int
    fun addAdjustmentListener(listener: XAdjustmentListener)
    fun removeAdjustmentListener(listener: XAdjustmentListener)
}

interface XListBox : XControl {
    fun selectItemPos(pos: Short, select: Boolean)
    fun getSelectedItems(): ShortArray
}

interface XComboBox : XControl {
    fun setText(text: String)
    fun getText(): String
}

interface XProgressBar : XControl {
    fun setValue(value: Int)
    fun getValue(): Int
}

interface XSpinField : XControl {
    fun addSpinListener(listener: XSpinListener)
    fun removeSpinListener(listener: XSpinListener)
}

// ---------------------------------------------------------
// Pickers and File Dialogs
// ---------------------------------------------------------

interface XFilePicker {
    fun setDefaultName(name: String)
    fun setDisplayDirectory(directory: String)
    fun getFiles(): Array<String>
}

interface XFolderPicker2 {
    fun setDisplayDirectory(directory: String)
    fun getDirectory(): String
    fun setTitle(title: String)
}

interface XFilterManager {
    fun appendFilter(title: String, filter: String)
}

interface XFilePickerControlAccess {
    fun setValue(controlId: Short, dummy: Short, value: Any)
}

object ExtendedFilePickerElementIds {
    const val CHECKBOX_AUTOEXTENSION: Short = 1
}

object TemplateDescription {
    const val FILESAVE_AUTOEXTENSION: Short = 1
}

interface XExecutableDialog {
    fun execute(): Short
}

object ExecutableDialogResults {
    const val OK: Short = 1
    const val CANCEL: Short = 0
}

// ---------------------------------------------------------
// Message Box
// ---------------------------------------------------------

interface XMessageBox {
    fun execute(): Short
}

interface XMessageBoxFactory {
    fun createMessageBox(
        parent: XWindowPeer,
        type: MessageBoxType,
        buttons: Int,
        title: String,
        message: String
    ): XMessageBox
}

enum class MessageBoxType {
    MESSAGEBOX,
    INFOBOX,
    WARNINGBOX,
    ERRORBOX,
    QUERYBOX
}

object MessageBoxButtons {
    const val BUTTONS_OK: Int = 1
    const val BUTTONS_OK_CANCEL: Int = 2
    const val BUTTONS_YES_NO: Int = 3
}

// ---------------------------------------------------------
// Graphics and Image Providers
// ---------------------------------------------------------

interface XGraphic

interface XGraphicProvider {
    fun queryGraphic(properties: Array<PropertyValue>): XGraphic
    fun storeGraphic(graphic: XGraphic, properties: Array<PropertyValue>)
}

// ---------------------------------------------------------
// Menuing
// ---------------------------------------------------------

interface XMenu {
    fun insertItem(id: Short, text: String, itemStyle: Short, pos: Short)
    fun insertSeparator(pos: Short)
    fun enableItem(id: Short, enable: Boolean)
    fun checkItem(id: Short, check: Boolean)
    fun addMenuListener(listener: XMenuListener)
}

interface XPopupMenu : XMenu

interface XMenuBar : XMenu {
    fun setPopupMenu(id: Short, popupMenu: XPopupMenu)
}

interface XMenuListener : XEventListener {
    fun select(event: MenuEvent)
    fun highlight(event: MenuEvent)
    fun activate(event: MenuEvent)
    fun deactivate(event: MenuEvent)
}

open class MenuEvent(source: Any, val MenuId: Short) : EventObject(source)

object MenuItemStyle {
    const val AUTOCHECK: Short = 1
    const val RADIOCHECK: Short = 2
    const val CHECKABLE: Short = 4
}

// ---------------------------------------------------------
// Common Structs and Types
// ---------------------------------------------------------

data class Rectangle(var X: Int = 0, var Y: Int = 0, var Width: Int = 0, var Height: Int = 0)
