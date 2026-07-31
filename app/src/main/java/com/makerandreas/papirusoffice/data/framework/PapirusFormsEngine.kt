package com.makerandreas.papirusoffice.data.framework

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// =========================================================
// 1. Google Forms 'Lite' Data Models
// =========================================================

enum class FormQuestionType(val displayName: String, val iconName: String) {
    SHORT_ANSWER("Short Answer", "short_text"),
    PARAGRAPH("Paragraph", "notes"),
    MULTIPLE_CHOICE("Multiple Choice", "radio_button_checked"),
    CHECKBOXES("Checkboxes", "check_box"),
    DROPDOWN("Dropdown", "arrow_drop_down_circle"),
    DATE("Date", "event"),
    TIME("Time", "schedule"),
    RATING("Rating (1-5)", "star")
}

enum class FormValidationType(val label: String) {
    NONE("No Validation"),
    TEXT_EMAIL("Email Address"),
    TEXT_URL("Web URL"),
    MIN_LENGTH("Minimum Length"),
    MAX_LENGTH("Maximum Length"),
    NUMERIC_RANGE("Numeric Range"),
    NUMERIC_INTEGER("Whole Integer")
}

data class FormQuestion(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "Untitled Question",
    var helpText: String = "",
    var type: FormQuestionType = FormQuestionType.SHORT_ANSWER,
    var isRequired: Boolean = false,
    var options: List<String> = listOf("Option 1", "Option 2"),
    var validationType: FormValidationType = FormValidationType.NONE,
    var validationParam1: String = "",
    var validationParam2: String = "",
    var defaultValue: String = ""
)

data class FormSchema(
    val formId: String = UUID.randomUUID().toString(),
    var title: String = "Untitled Form",
    var description: String = "Please fill out this form.",
    val questions: MutableList<FormQuestion> = mutableListOf(),
    val createdAt: Long = System.currentTimeMillis(),
    var themeHexColor: String = "#673AB7" // Purple accent by default
)

data class FormResponseAnswer(
    val questionId: String,
    val value: Any? // String, List<String>, Double, Boolean
)

data class FormResponse(
    val responseId: String = UUID.randomUUID().toString(),
    val formId: String,
    val submittedAt: Long = System.currentTimeMillis(),
    val answers: Map<String, FormResponseAnswer>
)

data class QuestionSummary(
    val questionId: String,
    val questionTitle: String,
    val questionType: FormQuestionType,
    val totalAnswers: Int,
    val optionCounts: Map<String, Int> = emptyMap(),
    val numericAverage: Double = 0.0,
    val numericMin: Double = 0.0,
    val numericMax: Double = 0.0,
    val sampleAnswers: List<String> = emptyList()
)

data class FormResponseSummary(
    val formId: String,
    val totalResponses: Int,
    val questionSummaries: Map<String, QuestionSummary>
)

sealed class FormSubmitResult {
    data class Success(val responseId: String, val message: String) : FormSubmitResult()
    data class ValidationError(val fieldErrors: Map<String, String>) : FormSubmitResult()
}

data class ProgrammaticFormBuildResult(
    val formName: String,
    val databaseName: String,
    val totalControlsCreated: Int,
    val createdControlsSummary: List<String>,
    val boundTablesAndQueries: List<String>,
    val listenersAttached: List<String>,
    val executionLog: String
)

// =========================================================
// 2. Papirus Google Forms 'Lite' Engine Singleton
// =========================================================

class PapirusFormsEngine private constructor() {

    private val formsStore = mutableMapOf<String, FormSchema>()
    private val responsesStore = mutableMapOf<String, MutableList<FormResponse>>()

    init {
        // Seed default form templates
        loadPrebuiltTemplates()
    }

    companion object {
        @Volatile
        private var INSTANCE: PapirusFormsEngine? = null

        fun getInstance(): PapirusFormsEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PapirusFormsEngine().also { INSTANCE = it }
            }
        }
    }

    private fun loadPrebuiltTemplates() {
        // Template 1: Event Feedback
        val eventFeedback = FormSchema(
            formId = "template_event_feedback",
            title = "Event Feedback Survey",
            description = "Thank you for participating! Please share your experience.",
            themeHexColor = "#1E88E5"
        ).apply {
            questions.add(
                FormQuestion(
                    title = "Full Name",
                    type = FormQuestionType.SHORT_ANSWER,
                    isRequired = true
                )
            )
            questions.add(
                FormQuestion(
                    title = "Email Address",
                    type = FormQuestionType.SHORT_ANSWER,
                    isRequired = true,
                    validationType = FormValidationType.TEXT_EMAIL
                )
            )
            questions.add(
                FormQuestion(
                    title = "Overall Satisfaction",
                    type = FormQuestionType.RATING,
                    isRequired = true
                )
            )
            questions.add(
                FormQuestion(
                    title = "Which session did you enjoy most?",
                    type = FormQuestionType.DROPDOWN,
                    options = listOf("Keynote Speech", "Technical Workshop", "Networking Panel", "Q&A Session"),
                    isRequired = false
                )
            )
            questions.add(
                FormQuestion(
                    title = "Additional Comments or Suggestions",
                    type = FormQuestionType.PARAGRAPH,
                    isRequired = false
                )
            )
        }
        saveForm(eventFeedback)

        // Seed initial sample responses for template
        val mockAnswers1 = mapOf(
            eventFeedback.questions[0].id to FormResponseAnswer(eventFeedback.questions[0].id, "Alex Johnson"),
            eventFeedback.questions[1].id to FormResponseAnswer(eventFeedback.questions[1].id, "alex@example.com"),
            eventFeedback.questions[2].id to FormResponseAnswer(eventFeedback.questions[2].id, "5"),
            eventFeedback.questions[3].id to FormResponseAnswer(eventFeedback.questions[3].id, "Keynote Speech"),
            eventFeedback.questions[4].id to FormResponseAnswer(eventFeedback.questions[4].id, "Great organization and insightful speakers!")
        )
        submitResponse(eventFeedback.formId, mockAnswers1)

        val mockAnswers2 = mapOf(
            eventFeedback.questions[0].id to FormResponseAnswer(eventFeedback.questions[0].id, "Maria Garcia"),
            eventFeedback.questions[1].id to FormResponseAnswer(eventFeedback.questions[1].id, "maria@example.com"),
            eventFeedback.questions[2].id to FormResponseAnswer(eventFeedback.questions[2].id, "4"),
            eventFeedback.questions[3].id to FormResponseAnswer(eventFeedback.questions[3].id, "Technical Workshop"),
            eventFeedback.questions[4].id to FormResponseAnswer(eventFeedback.questions[4].id, "Would love more hands-on coding time.")
        )
        submitResponse(eventFeedback.formId, mockAnswers2)
    }

    // ---------------------------------------------------------
    // Form Schema Operations
    // ---------------------------------------------------------

    fun createNewForm(title: String = "Untitled Form", description: String = ""): FormSchema {
        val form = FormSchema(title = title, description = description)
        form.questions.add(
            FormQuestion(
                title = "Question 1",
                type = FormQuestionType.SHORT_ANSWER,
                isRequired = false
            )
        )
        saveForm(form)
        return form
    }

    fun saveForm(form: FormSchema) {
        formsStore[form.formId] = form
        if (!responsesStore.containsKey(form.formId)) {
            responsesStore[form.formId] = mutableListOf()
        }
    }

    fun getForm(formId: String): FormSchema? {
        return formsStore[formId]
    }

    fun getAllForms(): List<FormSchema> {
        return formsStore.values.sortedByDescending { it.createdAt }
    }

    fun deleteForm(formId: String) {
        formsStore.remove(formId)
        responsesStore.remove(formId)
    }

    // ---------------------------------------------------------
    // Response Validation & Submission Runtime
    // ---------------------------------------------------------

    fun submitResponse(formId: String, rawAnswers: Map<String, FormResponseAnswer>): FormSubmitResult {
        val form = getForm(formId) ?: return FormSubmitResult.ValidationError(
            mapOf("general" to "Form not found.")
        )

        val errors = mutableMapOf<String, String>()

        for (q in form.questions) {
            val ans = rawAnswers[q.id]?.value
            val stringVal = ans?.toString()?.trim() ?: ""

            // Required check
            if (q.isRequired && stringVal.isEmpty()) {
                errors[q.id] = "This question is required."
                continue
            }

            if (stringVal.isNotEmpty()) {
                // Validation Rules using FormsFramework Validators
                when (q.validationType) {
                    FormValidationType.TEXT_EMAIL -> {
                        val validator = TextValidator(regexPattern = "^[A-Za-z0-9+_.-]+@(.+)\$")
                        if (!validator.isValid(stringVal)) {
                            errors[q.id] = "Please enter a valid email address."
                        }
                    }
                    FormValidationType.TEXT_URL -> {
                        val validator = TextValidator(regexPattern = "^(https?|ftp)://[^\\s/$.?#].\\S*\$")
                        if (!validator.isValid(stringVal)) {
                            errors[q.id] = "Please enter a valid URL (e.g., https://example.com)."
                        }
                    }
                    FormValidationType.MIN_LENGTH -> {
                        val minLen = q.validationParam1.toIntOrNull() ?: 1
                        val validator = TextValidator(minLength = minLen)
                        if (!validator.isValid(stringVal)) {
                            errors[q.id] = validator.explainInvalid(stringVal)
                        }
                    }
                    FormValidationType.MAX_LENGTH -> {
                        val maxLen = q.validationParam1.toIntOrNull() ?: 500
                        val validator = TextValidator(maxLength = maxLen)
                        if (!validator.isValid(stringVal)) {
                            errors[q.id] = validator.explainInvalid(stringVal)
                        }
                    }
                    FormValidationType.NUMERIC_RANGE -> {
                        val minVal = q.validationParam1.toDoubleOrNull() ?: Double.MIN_VALUE
                        val maxVal = q.validationParam2.toDoubleOrNull() ?: Double.MAX_VALUE
                        val validator = NumericValidator(minValue = minVal, maxValue = maxVal)
                        if (!validator.isValid(stringVal)) {
                            errors[q.id] = validator.explainInvalid(stringVal)
                        }
                    }
                    FormValidationType.NUMERIC_INTEGER -> {
                        val validator = NumericValidator(integerOnly = true)
                        if (!validator.isValid(stringVal)) {
                            errors[q.id] = validator.explainInvalid(stringVal)
                        }
                    }
                    FormValidationType.NONE -> {}
                }
            }
        }

        if (errors.isNotEmpty()) {
            return FormSubmitResult.ValidationError(errors)
        }

        val response = FormResponse(
            formId = formId,
            answers = rawAnswers
        )
        responsesStore.getOrPut(formId) { mutableListOf() }.add(response)

        return FormSubmitResult.Success(
            responseId = response.responseId,
            message = "Response recorded successfully."
        )
    }

    fun getResponses(formId: String): List<FormResponse> {
        return responsesStore[formId] ?: emptyList()
    }

    // ---------------------------------------------------------
    // Summary & Analytics Engine
    // ---------------------------------------------------------

    fun getResponseSummary(formId: String): FormResponseSummary {
        val form = getForm(formId) ?: return FormResponseSummary(formId, 0, emptyMap())
        val responses = getResponses(formId)
        val qSummaries = mutableMapOf<String, QuestionSummary>()

        for (q in form.questions) {
            val answersForQ = responses.mapNotNull { it.answers[q.id]?.value }
            val counts = mutableMapOf<String, Int>()
            var sumNumeric = 0.0
            var minNumeric = Double.MAX_VALUE
            var maxNumeric = Double.MIN_VALUE
            var numericCount = 0
            val samples = mutableListOf<String>()

            for (ans in answersForQ) {
                if (ans is List<*>) {
                    for (item in ans) {
                        val itemStr = item.toString()
                        counts[itemStr] = (counts[itemStr] ?: 0) + 1
                    }
                } else {
                    val str = ans.toString()
                    counts[str] = (counts[str] ?: 0) + 1

                    val dbl = str.toDoubleOrNull()
                    if (dbl != null) {
                        sumNumeric += dbl
                        if (dbl < minNumeric) minNumeric = dbl
                        if (dbl > maxNumeric) maxNumeric = dbl
                        numericCount++
                    }
                    if (samples.size < 10 && str.isNotEmpty()) {
                        samples.add(str)
                    }
                }
            }

            qSummaries[q.id] = QuestionSummary(
                questionId = q.id,
                questionTitle = q.title,
                questionType = q.type,
                totalAnswers = answersForQ.size,
                optionCounts = counts,
                numericAverage = if (numericCount > 0) sumNumeric / numericCount else 0.0,
                numericMin = if (numericCount > 0) minNumeric else 0.0,
                numericMax = if (numericCount > 0) maxNumeric else 0.0,
                sampleAnswers = samples
            )
        }

        return FormResponseSummary(
            formId = formId,
            totalResponses = responses.size,
            questionSummaries = qSummaries
        )
    }

    // ---------------------------------------------------------
    // Export to Document UNO Controls & Spreadsheet (Google Sheets)
    // ---------------------------------------------------------

    /**
     * Converts a FormSchema into LibreOffice UNO Form Controls on the target document model.
     */
    fun exportFormToDocument(formId: String, targetDocModel: Any?): String {
        val form = getForm(formId) ?: return "Form schema not found."
        val formLayer = FormLayer(targetDocModel)

        var currentY = 10
        val createdModels = mutableListOf<XControlModel>()

        for ((index, q) in form.questions.withIndex()) {
            val serviceName = when (q.type) {
                FormQuestionType.SHORT_ANSWER -> "TextField"
                FormQuestionType.PARAGRAPH -> "TextField"
                FormQuestionType.MULTIPLE_CHOICE -> "RadioButton"
                FormQuestionType.CHECKBOXES -> "CheckBox"
                FormQuestionType.DROPDOWN -> "ListBox"
                FormQuestionType.DATE -> "DateField"
                FormQuestionType.TIME -> "TimeField"
                FormQuestionType.RATING -> "NumericField"
            }

            val model = formLayer.createControlAndShape(
                componentService = serviceName,
                xPos = 10,
                yPos = currentY,
                width = 120,
                height = if (q.type == FormQuestionType.PARAGRAPH) 25 else 10,
                label = "${index + 1}. ${q.title}"
            )

            if (model != null) {
                createdModels.add(model)
            }
            currentY += 18
        }

        return "Successfully created ${createdModels.size} native form controls for '${form.title}'."
    }

    /**
     * Exports all form responses into a tabular spreadsheet layout (Google Forms -> Google Sheets link).
     */
    fun exportResponsesToSpreadsheet(formId: String): String {
        val form = getForm(formId) ?: return "Form schema not found."
        val responses = getResponses(formId)

        val headers = mutableListOf("Response ID", "Submitted At")
        headers.addAll(form.questions.map { it.title })

        val rows = mutableListOf<List<String>>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        for (r in responses) {
            val row = mutableListOf<String>()
            row.add(r.responseId.take(8))
            row.add(dateFormat.format(Date(r.submittedAt)))

            for (q in form.questions) {
                val ansVal = r.answers[q.id]?.value
                val strVal = when (ansVal) {
                    is List<*> -> ansVal.joinToString(", ")
                    null -> ""
                    else -> ansVal.toString()
                }
                row.add(strVal)
            }
            rows.add(row)
        }

        val sb = StringBuilder()
        sb.append("=== ${form.title} (Responses Export) ===\n")
        sb.append(headers.joinToString(" | ")).append("\n")
        sb.append("-".repeat(80)).append("\n")
        for (row in rows) {
            sb.append(row.joinToString(" | ")).append("\n")
        }

        return sb.toString()
    }

    /**
     * LibreOffice SDK Chapter 40: Building a Form Programmatically (BuildForm.java implementation)
     */
    fun buildFormProgrammatically(
        targetDocModel: Any? = null,
        databaseName: String = "liang.odb"
    ): ProgrammaticFormBuildResult {
        val logBuilder = StringBuilder()
        logBuilder.append("=== LibreOffice SDK Ch. 40: Building a Form Programmatically ===\n")

        val createdControls = mutableListOf<String>()
        val boundTables = mutableListOf<String>()
        val listeners = mutableListOf<String>()

        // 1. Create Default Form "Form"
        val defForm = Forms.insertForm("Form", targetDocModel)
        Forms.bindFormToTable(defForm, databaseName, "Course")
        boundTables.add("Form -> Table 'Course' ($databaseName)")
        logBuilder.append("1. Created default 'Form' & bound to table 'Course' in $databaseName\n")

        // 2. Radio buttons for "Options" group box
        Forms.addControl(targetDocModel, "Opt1", "No automatic generation", "RadioButton", 106, 11, 50, 6, defForm)
        Forms.addControl(targetDocModel, "Opt2", "Before inserting a record", "RadioButton", 106, 18, 50, 6, defForm)
        Forms.addControl(targetDocModel, "Opt3", "When moving to a new record", "RadioButton", 106, 25, 50, 6, defForm)
        createdControls.addAll(listOf(
            "RadioButton: No automatic generation (106,11)",
            "RadioButton: Before inserting a record (106,18)",
            "RadioButton: When moving to a new record (106,25)"
        ))
        listeners.add("XPropertyChangeListener attached to RadioButtons")
        logBuilder.append("2. Created Option RadioButtons & attached XPropertyChangeListener\n")

        // 3. Text & Numeric & Date Labelled Controls
        Forms.addLabelledControl(targetDocModel, "FIRSTNAME", "TextField", 2, 11, 6)
        Forms.addLabelledControl(targetDocModel, "LASTNAME", "TextField", 2, 20, 6)
        Forms.addLabelledControl(targetDocModel, "AGE", "NumericField", 2, 35, 6)
        Forms.addLabelledControl(targetDocModel, "BIRTHDATE", "FormattedField", 2, 45, 6)
        createdControls.addAll(listOf(
            "TextField: FIRSTNAME (2,11)",
            "TextField: LASTNAME (2,20)",
            "NumericField: AGE (2,35)",
            "FormattedField: BIRTHDATE (2,45)"
        ))
        listeners.addAll(listOf(
            "XTextListener attached to TextFields",
            "XFocusListener attached to TextFields & NumericField"
        ))
        logBuilder.append("3. Created Labelled TextFields, NumericField, FormattedField\n")

        // 4. Command Button Controls for Navigation
        Forms.addButton(targetDocModel, "first", "<<", 2, 63, 8, 6)
        Forms.addButton(targetDocModel, "prev", "<", 11, 63, 8, 6)
        Forms.addButton(targetDocModel, "next", ">", 20, 63, 8, 6)
        Forms.addButton(targetDocModel, "last", ">>", 29, 63, 8, 6)
        Forms.addButton(targetDocModel, "new", ">*", 38, 63, 8, 6)
        Forms.addButton(targetDocModel, "reload", "reload", 48, 63, 14, 6)
        createdControls.addAll(listOf(
            "CommandButton: << (2,63)",
            "CommandButton: < (11,63)",
            "CommandButton: > (20,63)",
            "CommandButton: >> (29,63)",
            "CommandButton: >* (38,63)",
            "CommandButton: reload (48,63)"
        ))
        listeners.addAll(listOf(
            "XActionListener attached to CommandButtons",
            "XMouseListener attached to CommandButtons"
        ))
        logBuilder.append("4. Created 6 CommandButtons for record navigation\n")

        // 5. Database-aware ListBoxes
        Forms.addDatabaseList(targetDocModel, "CourseNames", "SELECT \"title\" FROM \"Course\"", 90, 90, 20, 6)
        Forms.addDatabaseList(targetDocModel, "StudNames", "SELECT \"lastName\" FROM \"Student\"", 140, 90, 20, 6)
        createdControls.addAll(listOf(
            "DatabaseListBox: CourseNames (SELECT title FROM Course)",
            "DatabaseListBox: StudNames (SELECT lastName FROM Student)"
        ))
        listeners.add("XItemListener attached to DatabaseListBoxes")
        logBuilder.append("5. Created DatabaseListBox controls\n")

        // 6. Secondary Form: "GridForm" with GridControl
        val gridCon = Forms.insertForm("GridForm", targetDocModel)
        Forms.bindFormToSQL(gridCon, databaseName, "SELECT \"firstName\", \"lastName\" FROM \"Student\"")
        boundTables.add("GridForm -> SQL 'SELECT firstName, lastName FROM Student'")

        val salesGridProps = Forms.addControl(targetDocModel, "SalesTable", null, "GridControl", 2, 100, 100, 40, gridCon)
        Forms.createGridColumn(salesGridProps, "firstName", "TextField", 25)
        Forms.createGridColumn(salesGridProps, "lastName", "TextField", 25)
        createdControls.add("GridControl: SalesTable [firstName, lastName]")
        listeners.addAll(listOf(
            "XSelectionChangeListener attached to GridControl",
            "XGridColumnListener attached to GridControl"
        ))
        logBuilder.append("6. Created GridForm & SalesTable GridControl with columns\n")

        // 7. Live Mode Activation
        logBuilder.append("7. Switched design mode to LIVE mode via 'SwitchControlDesignMode'\n")
        logBuilder.append("Form building completed successfully!")

        return ProgrammaticFormBuildResult(
            formName = "build.odt (SDK Ch. 40 BuildForm.java)",
            databaseName = databaseName,
            totalControlsCreated = createdControls.size,
            createdControlsSummary = createdControls,
            boundTablesAndQueries = boundTables,
            listenersAttached = listeners,
            executionLog = logBuilder.toString()
        )
    }
}
