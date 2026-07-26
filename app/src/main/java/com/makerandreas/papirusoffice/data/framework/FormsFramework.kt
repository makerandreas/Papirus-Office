package com.makerandreas.papirusoffice.data.framework

// ---------------------------------------------------------
// Form Components & Containers
// ---------------------------------------------------------

interface XFormComponent : XComponent {
    var name: String
    var parent: Any? // XChild
}

interface XFormComponents : XNameAccess, XIndexAccess, XEnumerationAccess, XEventAttacherManager

interface XFormsSupplier {
    val forms: XNameContainer
}

interface XForm : XFormComponent

// ---------------------------------------------------------
// Control Models & Types
// ---------------------------------------------------------

interface XControlModel : XComponent // AWT

interface XFormControlModel : XControlModel, XFormComponent, XPropertyState {
    var classId: Short // FormComponentType
    var tabIndex: Short
}

object FormComponentType {
    const val CONTROL: Short = 0
    const val COMMANDBUTTON: Short = 1
    const val RADIOBUTTON: Short = 2
    const val IMAGEBUTTON: Short = 3
    const val CHECKBOX: Short = 4
    const val LISTBOX: Short = 5
    const val COMBOBOX: Short = 6
    const val GROUPBOX: Short = 7
    const val FIXEDTEXT: Short = 8
    const val GRIDCONTROL: Short = 9
    const val FILECONTROL: Short = 10
    const val HIDDENCONTROL: Short = 11
    const val IMAGECONTROL: Short = 12
    const val DATEFIELD: Short = 13
    const val TIMEFIELD: Short = 14
    const val NUMERICFIELD: Short = 15
    const val CURRENCYFIELD: Short = 16
    const val PATTERNFIELD: Short = 17
    const val TEXTFIELD: Short = 18
}

interface XPropertyState {
    fun getPropertyState(propertyName: String): Short // PropertyState enum
    fun getPropertyStates(propertyNames: Array<String>): Array<Short>
    fun setPropertyToDefault(propertyName: String)
    fun getPropertyDefault(propertyName: String): Any?
}

// ---------------------------------------------------------
// HTML & Data Forms
// ---------------------------------------------------------

interface XReset {
    fun reset()
    fun addResetListener(listener: Any /* XResetListener */)
    fun removeResetListener(listener: Any /* XResetListener */)
}

interface XSubmit {
    fun submit(control: XControl, mouseEvent: Any /* MouseEvent */)
    fun addSubmitListener(listener: Any /* XSubmitListener */)
    fun removeSubmitListener(listener: Any /* XSubmitListener */)
}

interface XLoadable {
    val isLoaded: Boolean
    fun load()
    fun unload()
    fun reload()
    fun addLoadListener(listener: Any /* XLoadListener */)
    fun removeLoadListener(listener: Any /* XLoadListener */)
}

// ---------------------------------------------------------
// Data Awareness
// ---------------------------------------------------------

interface XDatabaseParameterBroadcaster {
    fun addDatabaseParameterListener(listener: Any /* XDatabaseParameterListener */)
    fun removeDatabaseParameterListener(listener: Any /* XDatabaseParameterListener */)
}

interface XBoundComponent : XUpdateBroadcaster {
    fun commit(): Boolean
}

interface XUpdateBroadcaster {
    fun addUpdateListener(listener: Any /* XUpdateListener */)
    fun removeUpdateListener(listener: Any /* XUpdateListener */)
}

// ---------------------------------------------------------
// Value Bindings
// ---------------------------------------------------------

interface XValueBinding {
    fun getSupportedValueTypes(): Array<Any> // Type[]
    fun supportsType(type: Any): Boolean
    fun getValue(): Any
    fun setValue(value: Any)
}

interface XBindableValue {
    fun setValueBinding(binding: XValueBinding)
    fun getValueBinding(): XValueBinding?
}

interface XListEntrySink {
    fun setListEntrySource(source: XListEntrySource)
    fun getListEntrySource(): XListEntrySource?
}

interface XListEntrySource {
    val listEntryCount: Int
    fun getListEntry(position: Int): String
    fun getAllListEntries(): Array<String>
    fun addListEntryListener(listener: Any /* XListEntryListener */)
    fun removeListEntryListener(listener: Any /* XListEntryListener */)
}

// ---------------------------------------------------------
// Validation
// ---------------------------------------------------------

interface XValidator {
    fun isValid(value: Any): Boolean
    fun explainInvalid(value: Any): String
    fun addValidityConstraintListener(listener: Any /* XValidityConstraintListener */)
    fun removeValidityConstraintListener(listener: Any /* XValidityConstraintListener */)
}

interface XValidatable {
    fun setValidator(validator: XValidator)
    fun getValidator(): XValidator?
}

interface XValidatableFormComponent : XValidatable {
    val isValid: Boolean
    val currentValue: Any
    fun addFormComponentValidityListener(listener: Any /* XFormComponentValidityListener */)
    fun removeFormComponentValidityListener(listener: Any /* XFormComponentValidityListener */)
}

// ---------------------------------------------------------
// Scripting and Events
// ---------------------------------------------------------

interface XEventAttacherManager {
    fun registerScriptEvent(index: Int, scriptEvent: ScriptEventDescriptor)
    fun registerScriptEvents(index: Int, scriptEvents: Array<ScriptEventDescriptor>)
    fun revokeScriptEvent(index: Int, listenerType: String, eventMethod: String, removeListenerParam: String)
    fun revokeScriptEvents(index: Int)
    fun insertEntry(index: Int)
    fun removeEntry(index: Int)
    fun getScriptEvents(index: Int): Array<ScriptEventDescriptor>
}

data class ScriptEventDescriptor(
    var ListenerType: String = "",
    var EventMethod: String = "",
    var AddListenerParam: String = "",
    var ScriptType: String = "",
    var ScriptCode: String = ""
)

// ---------------------------------------------------------
// Views and Control Access
// ---------------------------------------------------------

interface XControl {
    fun setContext(context: Any /* XUnoControlContext */)
    fun getContext(): Any?
    fun createPeer(toolkit: Any /* XToolkit */, parent: Any /* XWindowPeer */)
    fun getPeer(): Any?
    fun setModel(model: XControlModel): Boolean
    fun getModel(): XControlModel?
    fun getView(): Any?
    fun setDesignMode(designMode: Boolean)
    fun isDesignMode(): Boolean
    fun isTransparent(): Boolean
}

interface XControlAccess {
    fun getControl(model: XControlModel): XControl
}

interface XTabControllerModel {
    var groupControl: Boolean
    var controlModels: Array<XControlModel>
    var group: Array<XControlModel>
    fun getGroupCount(): Int
    fun getGroup(groupIndex: Int): Array<XControlModel>
    fun getGroupByName(name: String): Array<XControlModel>
    fun setGroup(group: Array<XControlModel>, groupName: String)
}
