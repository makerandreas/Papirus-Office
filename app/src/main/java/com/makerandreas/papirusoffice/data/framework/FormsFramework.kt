package com.makerandreas.papirusoffice.data.framework

// =========================================================
// 1. Core Form Components & Containers (SDK Guide Ch. 39)
// =========================================================

interface XFormComponent : XComponent {
    var name: String
    var parent: Any? // XChild
}

interface XFormComponents : XNameAccess, XIndexAccess, XEnumerationAccess, XEventAttacherManager

interface XFormsSupplier {
    val forms: XNameContainer
}

interface XFormsSupplier2 : XFormsSupplier {
    fun hasForms(): Boolean
}

interface XForm : XFormComponent

interface XFormController : XComponent {
    var currentControl: XControl?
    val formOperations: Any?
}

// =========================================================
// 2. Control Models & Types
// =========================================================

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

    fun getTypeString(typeId: Short): String = when (typeId) {
        COMMANDBUTTON -> "CommandButton"
        RADIOBUTTON -> "RadioButton"
        IMAGEBUTTON -> "ImageButton"
        CHECKBOX -> "CheckBox"
        LISTBOX -> "ListBox"
        COMBOBOX -> "ComboBox"
        GROUPBOX -> "GroupBox"
        FIXEDTEXT -> "FixedText"
        GRIDCONTROL -> "GridControl"
        FILECONTROL -> "FileControl"
        HIDDENCONTROL -> "HiddenControl"
        IMAGECONTROL -> "ImageControl"
        DATEFIELD -> "DateField"
        TIMEFIELD -> "TimeField"
        NUMERICFIELD -> "NumericField"
        CURRENCYFIELD -> "CurrencyField"
        PATTERNFIELD -> "PatternField"
        TEXTFIELD -> "TextField"
        else -> "UnknownControl"
    }
}

interface XPropertyState {
    fun getPropertyState(propertyName: String): Short // PropertyState enum
    fun getPropertyStates(propertyNames: Array<String>): Array<Short>
    fun setPropertyToDefault(propertyName: String)
    fun getPropertyDefault(propertyName: String): Any?
}

// =========================================================
// 3. Form Event Objects
// =========================================================

open class FormEventObject(val source: Any)

class FormTextEvent(source: Any) : FormEventObject(source)
class FormChangeEvent(source: Any) : FormEventObject(source)

// =========================================================
// 4. Submission & HTML Form Interfaces
// =========================================================

interface XReset {
    fun reset()
    fun addResetListener(listener: XResetListener)
    fun removeResetListener(listener: XResetListener)
}

interface XResetListener {
    fun approveReset(event: FormEventObject): Boolean
    fun resetted(event: FormEventObject)
}

interface XSubmit {
    fun submit(control: XControl, mouseEvent: Any? = null)
    fun addSubmitListener(listener: XSubmitListener)
    fun removeSubmitListener(listener: XSubmitListener)
}

interface XSubmitListener {
    fun approveSubmit(event: FormEventObject): Boolean
}

interface XLoadable {
    val isLoaded: Boolean
    fun load()
    fun unload()
    fun reload()
    fun addLoadListener(listener: XLoadListener)
    fun removeLoadListener(listener: XLoadListener)
}

interface XLoadListener {
    fun loaded(event: FormEventObject)
    fun reloading(event: FormEventObject)
    fun reloaded(event: FormEventObject)
    fun unloading(event: FormEventObject)
    fun unloaded(event: FormEventObject)
}

// =========================================================
// 5. Data Awareness & Database Parameters
// =========================================================

interface XDatabaseParameterBroadcaster {
    fun addDatabaseParameterListener(listener: XDatabaseParameterListener)
    fun removeDatabaseParameterListener(listener: XDatabaseParameterListener)
}

interface XDatabaseParameterListener {
    fun approveParameter(event: FormEventObject): Boolean
}

interface XBoundComponent : XUpdateBroadcaster {
    fun commit(): Boolean
}

interface XUpdateBroadcaster {
    fun addUpdateListener(listener: XUpdateListener)
    fun removeUpdateListener(listener: XUpdateListener)
}

interface XUpdateListener {
    fun approveUpdate(event: FormEventObject): Boolean
    fun updated(event: FormEventObject)
}

interface XRowSetListener {
    fun cursorMoved(event: FormEventObject)
    fun rowChanged(event: FormEventObject)
    fun rowSetChanged(event: FormEventObject)
}

// =========================================================
// 6. Value Bindings
// =========================================================

interface XValueBinding {
    fun getSupportedValueTypes(): Array<Any>
    fun supportsType(type: Any): Boolean
    fun getValue(): Any?
    fun setValue(value: Any?)
}

interface XBindableValue {
    fun setValueBinding(binding: XValueBinding)
    fun getValueBinding(): XValueBinding?
}

interface XListEntrySink {
    fun setListEntrySource(source: XListEntrySource?)
    fun getListEntrySource(): XListEntrySource?
}

interface XListEntrySource {
    val listEntryCount: Int
    fun getListEntry(position: Int): String
    fun getAllListEntries(): Array<String>
    fun addListEntryListener(listener: Any)
    fun removeListEntryListener(listener: Any)
}

// =========================================================
// 7. Validation Engine
// =========================================================

interface XValidator {
    fun isValid(value: Any?): Boolean
    fun explainInvalid(value: Any?): String
    fun addValidityConstraintListener(listener: XValidityConstraintListener)
    fun removeValidityConstraintListener(listener: XValidityConstraintListener)
}

interface XValidityConstraintListener {
    fun validityConstraintChanged(event: FormEventObject)
}

interface XValidatable {
    fun setValidator(validator: XValidator?)
    fun getValidator(): XValidator?
}

interface XFormComponentValidityListener {
    fun componentValidityChanged(event: FormEventObject)
}

interface XValidatableFormComponent : XValidatable {
    val isValid: Boolean
    val currentValue: Any?
    fun addFormComponentValidityListener(listener: XFormComponentValidityListener)
    fun removeFormComponentValidityListener(listener: XFormComponentValidityListener)
}

// =========================================================
// 8. Scripting & Events
// =========================================================

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

// =========================================================
// 9. Views, Control Access & SDK Ch. 40 Form Interfaces
// =========================================================

object CommandType {
    const val TABLE: Int = 0
    const val QUERY: Int = 1
    const val COMMAND: Int = 2
}

object ListSourceType {
    const val VALUELIST: Int = 0
    const val TABLE: Int = 1
    const val QUERY: Int = 2
    const val SQL: Int = 3
    const val SQLPASSTHROUGH: Int = 4
    const val TABLEFIELDS: Int = 5
}

interface XGridColumnFactory {
    fun createColumn(columnType: String): XPropertySet
}

interface XGridControl {
    fun addGridControlListener(listener: Any)
    fun removeGridControlListener(listener: Any)
    fun getCurrentColumnPosition(): Short
}

interface XGridColumnListener {
    fun columnChanged(event: EventObject)
}

interface XSelectionChangeListener {
    fun selectionChanged(event: EventObject)
}

class MockControlPropertySet : XPropertySet {
    val properties = mutableMapOf<String, Any?>()

    override fun getPropertySetInfo(): XPropertySetInfo {
        return object : XPropertySetInfo {
            override fun getProperties(): Array<Property> = properties.keys.map { Property(it, 0, Any::class.java, 0.toShort()) }.toTypedArray()
            override fun getPropertyByName(name: String): Property = Property(name, 0, Any::class.java, 0.toShort())
            override fun hasPropertyByName(name: String): Boolean = properties.containsKey(name)
        }
    }

    override fun setPropertyValue(propertyName: String, value: Any) {
        properties[propertyName] = value
    }

    override fun getPropertyValue(propertyName: String): Any {
        return properties[propertyName] ?: ""
    }

    override fun addPropertyChangeListener(propertyName: String, listener: Any) {}
    override fun removePropertyChangeListener(propertyName: String, listener: Any) {}
    override fun addVetoableChangeListener(propertyName: String, listener: Any) {}
    override fun removeVetoableChangeListener(propertyName: String, listener: Any) {}
}

class DynamicNameContainer : XNameContainer, XForm {
    override var name: String = "Form"
    override var parent: Any? = null
    override fun dispose() {}
    override fun addEventListener(listener: XEventListener) {}
    override fun removeEventListener(listener: XEventListener) {}

    private val elements = mutableMapOf<String, Any>()

    override fun insertByName(name: String, element: Any) {
        elements[name] = element
    }

    override fun removeByName(name: String) {
        elements.remove(name)
    }

    override fun getByName(name: String): Any = elements[name] ?: ""
    override fun getElementNames(): List<String> = elements.keys.toList()
    override fun hasByName(name: String): Boolean = elements.containsKey(name)
}

interface XControl {
    fun setContext(context: Any?)
    fun getContext(): Any?
    fun createPeer(toolkit: Any?, parent: Any?)
    fun getPeer(): Any?
    fun setModel(model: XControlModel): Boolean
    fun getModel(): XControlModel?
    fun getView(): Any?
    fun setDesignMode(designMode: Boolean)
    fun isDesignMode(): Boolean
    fun isTransparent(): Boolean
}

interface XControlAccess {
    fun getControl(model: XControlModel): XControl?
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

// =========================================================
// 10. SDK Guide Chapter 39 & 40 Core Utility Object: `Forms`
// =========================================================

object Forms {

    private val documentFormsStore = mutableMapOf<Any?, MutableMap<String, XNameContainer>>()

    fun insertForm(formName: String, doc: Any?): XNameContainer {
        val formMap = documentFormsStore.getOrPut(doc) { mutableMapOf() }
        if (!formMap.containsKey(formName)) {
            val newFormContainer = DynamicNameContainer()
            formMap[formName] = newFormContainer
        }
        return formMap[formName]!!
    }

    fun getForm(doc: Any?, formName: String): XForm? {
        val formMap = documentFormsStore[doc]
        val container = formMap?.get(formName)
        if (container is XForm) return container
        return null
    }

    fun getFormName(cModel: Any?): String {
        return if (cModel is XNamed) cModel.name else "Form"
    }

    /**
     * Programmatic form control creation (SDK Ch. 40: BuildForm.java)
     */
    fun addControl(
        doc: Any?,
        name: String,
        label: String?,
        compKind: String,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        parentForm: XNameContainer? = null
    ): XPropertySet {
        val modelProps = MockControlPropertySet()
        modelProps.setPropertyValue("Name", name)
        if (label != null) {
            modelProps.setPropertyValue("Label", label)
        }
        modelProps.setPropertyValue("ComponentKind", compKind)
        modelProps.setPropertyValue("X", x * 100)
        modelProps.setPropertyValue("Y", y * 100)
        modelProps.setPropertyValue("Width", width * 100)
        modelProps.setPropertyValue("Height", height * 100)
        modelProps.setPropertyValue("AnchorType", TextContentAnchorType.AT_PARAGRAPH)

        val targetForm = parentForm ?: insertForm("Form", doc)
        targetForm.insertByName(name, modelProps)
        return modelProps
    }

    fun addButton(
        doc: Any?,
        name: String,
        label: String,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ): XPropertySet {
        val buttonProps = addControl(doc, name, label, "CommandButton", x, y, width, height)
        buttonProps.setPropertyValue("HelpText", name)
        buttonProps.setPropertyValue("Tabstop", false)
        buttonProps.setPropertyValue("FocusOnClick", false)
        return buttonProps
    }

    fun addLabelledControl(
        doc: Any?,
        label: String,
        compKind: String,
        x: Int = 2,
        y: Int = 11,
        height: Int = 6
    ): XPropertySet {
        val labelProps = addControl(doc, "${label}_Label", label, "FixedText", x, y, 25, 6)
        val ctrlProps = addControl(doc, label, null, compKind, x + 26, y, 40, height)
        ctrlProps.setPropertyValue("DataField", label)
        ctrlProps.setPropertyValue("LabelControl", labelProps)
        return ctrlProps
    }

    fun addDatabaseList(
        doc: Any?,
        name: String,
        sqlCmd: String,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ): XPropertySet {
        val listProps = addControl(doc, name, null, "DatabaseListBox", x, y, width, height)
        listProps.setPropertyValue("Dropdown", true)
        listProps.setPropertyValue("MultiSelection", false)
        listProps.setPropertyValue("BoundColumn", 0.toShort())
        listProps.setPropertyValue("ListSourceType", ListSourceType.SQL)
        listProps.setPropertyValue("ListSource", arrayOf(sqlCmd))
        return listProps
    }

    fun bindFormToTable(xForm: Any?, sourceName: String, tableName: String) {
        if (xForm is XPropertySet) {
            xForm.setPropertyValue("DataSourceName", sourceName)
            xForm.setPropertyValue("Command", tableName)
            xForm.setPropertyValue("CommandType", CommandType.TABLE)
        }
    }

    fun bindFormToSQL(xForm: Any?, sourceName: String, cmd: String) {
        if (xForm is XPropertySet) {
            xForm.setPropertyValue("DataSourceName", sourceName)
            xForm.setPropertyValue("Command", cmd)
            xForm.setPropertyValue("CommandType", CommandType.COMMAND)
        }
    }

    fun createGridColumn(gridModel: Any?, dataField: String, colKind: String, width: Int) {
        if (gridModel is MockControlPropertySet) {
            @Suppress("UNCHECKED_CAST")
            val cols = (gridModel.getPropertyValue("GridColumns") as? MutableList<MockControlPropertySet>)
                ?: mutableListOf<MockControlPropertySet>().also { gridModel.setPropertyValue("GridColumns", it) }

            val colProps = MockControlPropertySet().apply {
                setPropertyValue("DataField", dataField)
                setPropertyValue("Label", dataField)
                setPropertyValue("Name", dataField)
                setPropertyValue("ColumnKind", colKind)
                if (width > 0) {
                    setPropertyValue("Width", width * 10)
                }
            }
            cols.add(colProps)
        }
    }

    /**
     * Recursively retrieves all form control models contained in the document.
     */
    fun getModels(docOrContainer: Any?): List<XControlModel> {
        val result = ArrayList<XControlModel>()
        if (docOrContainer == null) return result

        when (docOrContainer) {
            is XFormsSupplier -> {
                val formsContainer = docOrContainer.forms
                for (name in formsContainer.getElementNames()) {
                    val child = formsContainer.getByName(name)
                    result.addAll(getModels(child))
                }
            }
            is XIndexAccess -> {
                val count = docOrContainer.count
                for (i in 0 until count) {
                    val child = docOrContainer.getByIndex(i)
                    if (child is XControlModel) {
                        result.add(child)
                    } else if (child is XIndexAccess) {
                        result.addAll(getModels(child))
                    }
                }
            }
            is XControlModel -> {
                result.add(docOrContainer)
            }
        }
        return result
    }

    /**
     * Retrieves the XControl view for a given XControlModel.
     */
    fun getControl(doc: Any?, model: XControlModel): XControl? {
        if (doc is XModel) {
            val controller = doc.getCurrentController()
            if (controller is XControlAccess) {
                return controller.getControl(model)
            }
        }
        return null
    }

    fun getName(cModel: Any?): String {
        return if (cModel is XNamed) cModel.name else ""
    }

    fun getLabel(cModel: Any?): String {
        return FLTools.getLabel(cModel)
    }

    fun getID(cModel: Any?): Short {
        return if (cModel is XFormControlModel) cModel.classId else FormComponentType.CONTROL
    }

    fun getTypeStr(cModel: Any?): String {
        return FormComponentType.getTypeString(getID(cModel))
    }

    fun isButton(cModel: Any?): Boolean {
        val id = getID(cModel)
        return id == FormComponentType.COMMANDBUTTON || id == FormComponentType.IMAGEBUTTON
    }

    fun isTextField(cModel: Any?): Boolean {
        val id = getID(cModel)
        return id == FormComponentType.TEXTFIELD || id == FormComponentType.PATTERNFIELD
    }

    fun isCheckBox(cModel: Any?): Boolean {
        return getID(cModel) == FormComponentType.CHECKBOX
    }

    fun isListBox(cModel: Any?): Boolean {
        val id = getID(cModel)
        return id == FormComponentType.LISTBOX || id == FormComponentType.COMBOBOX
    }

    fun isRadioButton(cModel: Any?): Boolean {
        return getID(cModel) == FormComponentType.RADIOBUTTON
    }

    fun isDateField(cModel: Any?): Boolean {
        return getID(cModel) == FormComponentType.DATEFIELD
    }

    fun isTimeField(cModel: Any?): Boolean {
        return getID(cModel) == FormComponentType.TIMEFIELD
    }

    fun isNumericField(cModel: Any?): Boolean {
        val id = getID(cModel)
        return id == FormComponentType.NUMERICFIELD || id == FormComponentType.CURRENCYFIELD
    }

    fun getEventControlModel(event: Any?): XControlModel? {
        if (event is FormEventObject) {
            val src = event.source
            if (src is XControl) {
                return src.getModel()
            } else if (src is XControlModel) {
                return src
            }
        }
        return null
    }
}

// =========================================================
// 11. Form Layer Helper & FLTools
// =========================================================

object FLTools {
    fun getLabel(aFormComponent: Any?): String {
        if (aFormComponent == null) return ""
        if (aFormComponent is XNamed) {
            val name = aFormComponent.name
            if (name.isNotEmpty()) return name
        }
        return "Control"
    }

    fun classifyFormComponentType(cModel: Any?): String {
        return Forms.getTypeStr(cModel)
    }

    fun disposeComponent(component: Any?) {
        if (component is XComponent) {
            component.dispose()
        }
    }
}

class FormLayer(private val documentModel: Any?) {

    fun createControlAndShape(
        componentService: String,
        xPos: Int,
        yPos: Int,
        width: Int,
        height: Int,
        label: String = ""
    ): XControlModel? {
        val model = object : XFormControlModel {
            override var classId: Short = when (componentService) {
                "TextField" -> FormComponentType.TEXTFIELD
                "CommandButton" -> FormComponentType.COMMANDBUTTON
                "CheckBox" -> FormComponentType.CHECKBOX
                "RadioButton" -> FormComponentType.RADIOBUTTON
                "ListBox" -> FormComponentType.LISTBOX
                "DateField" -> FormComponentType.DATEFIELD
                "TimeField" -> FormComponentType.TIMEFIELD
                "NumericField" -> FormComponentType.NUMERICFIELD
                else -> FormComponentType.CONTROL
            }
            override var tabIndex: Short = 0
            override var name: String = label.ifEmpty { componentService }
            override var parent: Any? = null

            private val listeners = mutableListOf<XEventListener>()

            override fun getPropertyState(propertyName: String): Short = 0
            override fun getPropertyStates(propertyNames: Array<String>): Array<Short> = Array(propertyNames.size) { 0 }
            override fun setPropertyToDefault(propertyName: String) {}
            override fun getPropertyDefault(propertyName: String): Any? = null

            override fun dispose() {
                listeners.forEach { it.disposing(EventObject(this)) }
                listeners.clear()
            }

            override fun addEventListener(listener: XEventListener) {
                listeners.add(listener)
            }

            override fun removeEventListener(listener: XEventListener) {
                listeners.remove(listener)
            }
        }
        return model
    }
}

// =========================================================
// 12. Component Tree Traversal Engine
// =========================================================

fun interface IFormComponentAction {
    fun act(component: XControlModel)
}

class ComponentTreeTraversal {
    fun traverse(container: Any?, action: IFormComponentAction) {
        val models = Forms.getModels(container)
        for (m in models) {
            action.act(m)
        }
    }
}

// =========================================================
// 13. Concrete Validator Suite (Reference Java SDK Validators)
// =========================================================

class TextValidator(
    var minLength: Int = 0,
    var maxLength: Int = Int.MAX_VALUE,
    var regexPattern: String = ""
) : XValidator {

    private val listeners = mutableListOf<XValidityConstraintListener>()

    override fun isValid(value: Any?): Boolean {
        val str = value?.toString() ?: ""
        if (str.length < minLength) return false
        if (str.length > maxLength) return false
        if (regexPattern.isNotEmpty() && !str.matches(Regex(regexPattern))) return false
        return true
    }

    override fun explainInvalid(value: Any?): String {
        val str = value?.toString() ?: ""
        if (str.length < minLength) return "Minimum length is $minLength characters."
        if (str.length > maxLength) return "Maximum length is $maxLength characters."
        if (regexPattern.isNotEmpty() && !str.matches(Regex(regexPattern))) return "Invalid text format."
        return ""
    }

    override fun addValidityConstraintListener(listener: XValidityConstraintListener) {
        listeners.add(listener)
    }

    override fun removeValidityConstraintListener(listener: XValidityConstraintListener) {
        listeners.remove(listener)
    }
}

class NumericValidator(
    var minValue: Double = Double.MIN_VALUE,
    var maxValue: Double = Double.MAX_VALUE,
    var integerOnly: Boolean = false
) : XValidator {

    private val listeners = mutableListOf<XValidityConstraintListener>()

    override fun isValid(value: Any?): Boolean {
        if (value == null) return false
        val num = value.toString().toDoubleOrNull() ?: return false
        if (num < minValue || num > maxValue) return false
        if (integerOnly && num % 1.0 != 0.0) return false
        return true
    }

    override fun explainInvalid(value: Any?): String {
        if (value == null || value.toString().isEmpty()) return "Numeric value is required."
        val num = value.toString().toDoubleOrNull()
            ?: return "Value must be a valid number."
        if (num < minValue) return "Value must be at least $minValue."
        if (num > maxValue) return "Value must be at most $maxValue."
        if (integerOnly && num % 1.0 != 0.0) return "Value must be a whole integer."
        return ""
    }

    override fun addValidityConstraintListener(listener: XValidityConstraintListener) {
        listeners.add(listener)
    }

    override fun removeValidityConstraintListener(listener: XValidityConstraintListener) {
        listeners.remove(listener)
    }
}

class DateValidator(
    var minDateIso: String = "",
    var maxDateIso: String = ""
) : XValidator {
    private val listeners = mutableListOf<XValidityConstraintListener>()

    override fun isValid(value: Any?): Boolean {
        val str = value?.toString() ?: ""
        if (str.isEmpty()) return false
        if (minDateIso.isNotEmpty() && str < minDateIso) return false
        if (maxDateIso.isNotEmpty() && str > maxDateIso) return false
        return true
    }

    override fun explainInvalid(value: Any?): String {
        val str = value?.toString() ?: ""
        if (str.isEmpty()) return "Date is required."
        if (minDateIso.isNotEmpty() && str < minDateIso) return "Date cannot be earlier than $minDateIso."
        if (maxDateIso.isNotEmpty() && str > maxDateIso) return "Date cannot be later than $maxDateIso."
        return ""
    }

    override fun addValidityConstraintListener(listener: XValidityConstraintListener) { listeners.add(listener) }
    override fun removeValidityConstraintListener(listener: XValidityConstraintListener) { listeners.remove(listener) }
}

class TimeValidator : XValidator {
    private val listeners = mutableListOf<XValidityConstraintListener>()

    override fun isValid(value: Any?): Boolean {
        val str = value?.toString() ?: ""
        return str.matches(Regex("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$"))
    }

    override fun explainInvalid(value: Any?): String {
        return "Time must be in HH:mm format."
    }

    override fun addValidityConstraintListener(listener: XValidityConstraintListener) { listeners.add(listener) }
    override fun removeValidityConstraintListener(listener: XValidityConstraintListener) { listeners.remove(listener) }
}

class BooleanValidator(var requiredState: Boolean = true) : XValidator {
    private val listeners = mutableListOf<XValidityConstraintListener>()

    override fun isValid(value: Any?): Boolean {
        val b = (value as? Boolean) ?: value.toString().toBoolean()
        return b == requiredState
    }

    override fun explainInvalid(value: Any?): String {
        return "This check box must be checked."
    }

    override fun addValidityConstraintListener(listener: XValidityConstraintListener) { listeners.add(listener) }
    override fun removeValidityConstraintListener(listener: XValidityConstraintListener) { listeners.remove(listener) }
}

class ListSelectionValidator(var allowedChoices: List<String>) : XValidator {
    private val listeners = mutableListOf<XValidityConstraintListener>()

    override fun isValid(value: Any?): Boolean {
        val str = value?.toString() ?: ""
        return allowedChoices.contains(str)
    }

    override fun explainInvalid(value: Any?): String {
        return "Please select a valid option from the list."
    }

    override fun addValidityConstraintListener(listener: XValidityConstraintListener) { listeners.add(listener) }
    override fun removeValidityConstraintListener(listener: XValidityConstraintListener) { listeners.remove(listener) }
}

// =========================================================
// 14. Single Control Validation Runner
// =========================================================

class SingleControlValidation(
    val controlModel: XControlModel,
    var validator: XValidator
) {
    var statusExplanation: String = ""
        private set

    fun validate(value: Any?): Boolean {
        val valid = validator.isValid(value)
        statusExplanation = if (valid) "" else validator.explainInvalid(value)
        return valid
    }
}

// =========================================================
// 15. Key Generator & Control Lock Engine
// =========================================================

class KeyGenerator(private var currentMaxKey: Long = 0) {
    fun generateNextKey(): Long {
        currentMaxKey += 1
        return currentMaxKey
    }
}

class ControlLock {
    fun updateLockState(isNewRecord: Boolean, modelsToLock: List<XControlModel>) {
        // Lock state management
    }
}

class GridFieldValidator : XUpdateListener {
    override fun approveUpdate(event: FormEventObject): Boolean {
        return true
    }

    override fun updated(event: FormEventObject) {}
}

// =========================================================
// 16. Table & Spreadsheet Value Binding
// =========================================================

class TableCellTextBinding(var cellValue: String = "") : XValueBinding {
    override fun getSupportedValueTypes(): Array<Any> = arrayOf(String::class.java)
    override fun supportsType(type: Any): Boolean = type == String::class.java
    override fun getValue(): Any = cellValue
    override fun setValue(value: Any?) {
        cellValue = value?.toString() ?: ""
    }
}

class SpreadsheetValueBinding(var cellRangeAddress: String) : XValueBinding, XListEntrySource {
    private var dataEntries = mutableListOf<String>()

    override fun getSupportedValueTypes(): Array<Any> = arrayOf(String::class.java, List::class.java)
    override fun supportsType(type: Any): Boolean = true
    override fun getValue(): Any = dataEntries
    override fun setValue(value: Any?) {
        if (value is List<*>) {
            dataEntries = value.mapNotNull { it?.toString() }.toMutableList()
        }
    }

    override val listEntryCount: Int get() = dataEntries.size
    override fun getListEntry(position: Int): String = dataEntries.getOrElse(position) { "" }
    override fun getAllListEntries(): Array<String> = dataEntries.toTypedArray()
    override fun addListEntryListener(listener: Any) {}
    override fun removeListEntryListener(listener: Any) {}
}
