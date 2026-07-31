package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.makerandreas.papirusoffice.data.framework.*

/**
 * Universal Forms Sheet & Engine (SDK Guide Chapter 39 "Forms API Overview" & Google Forms Lite Concept)
 * 
 * Provides complete form creation, interactive response filling, dynamic validation runtime,
 * response summary analytics, and document embedding across ALL Papirus Office modules (Inky, Cellina, Slidia, Pagella).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalFormsSheet(
    activeModuleName: String, // "Inky", "Cellina", "Slidia", "Pagella"
    onDismiss: () -> Unit,
    onInsertFormToDoc: (FormSchema) -> Unit = {}
) {
    val context = LocalContext.current
    val formsEngine = remember { PapirusFormsEngine.getInstance() }

    var allForms by remember { mutableStateOf(formsEngine.getAllForms()) }
    var selectedFormId by remember {
        mutableStateOf(allForms.firstOrNull()?.formId ?: formsEngine.createNewForm().formId)
    }
    var activeForm by remember(selectedFormId, allForms) {
        mutableStateOf(formsEngine.getForm(selectedFormId) ?: formsEngine.createNewForm())
    }

    var selectedTab by remember { mutableStateOf(0) } // 0 = Builder, 1 = Preview/Responder, 2 = Responses, 3 = Insert

    // Responder state (answers for preview filling)
    val responderAnswers = remember { mutableStateMapOf<String, Any>() }
    var submitResultMsg by remember { mutableStateOf<String?>(null) }
    var submitErrors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    fun refreshForms() {
        allForms = formsEngine.getAllForms()
        val updated = formsEngine.getForm(selectedFormId)
        if (updated != null) {
            activeForm = updated
        } else if (allForms.isNotEmpty()) {
            selectedFormId = allForms.first().formId
            activeForm = allForms.first()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Assignment,
                                    contentDescription = "Forms",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Papirus Forms Engine",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Google Forms Lite • Module: $activeModuleName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = {
                                val newF = formsEngine.createNewForm("New Survey", "Form description")
                                refreshForms()
                                selectedFormId = newF.formId
                                Toast.makeText(context, "Created new blank form", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "New Form", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Form")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Form Selector Carousel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allForms.forEach { f ->
                        val isSelected = f.formId == selectedFormId
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedFormId = f.formId
                                submitResultMsg = null
                                submitErrors = emptyMap()
                                responderAnswers.clear()
                            },
                            label = { Text(f.title) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Primary Tab Row
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Form Builder") },
                        icon = { Icon(Icons.Default.EditNote, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Interactive Preview") },
                        icon = { Icon(Icons.Default.Visibility, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Responses & Analytics") },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Insert into Doc") },
                        icon = { Icon(Icons.Default.PostAdd, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        text = { Text("SDK Ch. 40") },
                        icon = { Icon(Icons.Default.Code, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content Body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedTab) {
                        0 -> FormBuilderTab(
                            form = activeForm,
                            onUpdate = {
                                formsEngine.saveForm(it)
                                refreshForms()
                            },
                            onDelete = {
                                formsEngine.deleteForm(it.formId)
                                refreshForms()
                            }
                        )
                        1 -> FormResponderTab(
                            form = activeForm,
                            answers = responderAnswers,
                            submitResultMsg = submitResultMsg,
                            submitErrors = submitErrors,
                            onSubmit = { rawMap ->
                                val formattedAnswers = rawMap.mapValues { (k, v) ->
                                    FormResponseAnswer(questionId = k, value = v)
                                }
                                val result = formsEngine.submitResponse(activeForm.formId, formattedAnswers)
                                when (result) {
                                    is FormSubmitResult.Success -> {
                                        submitResultMsg = result.message
                                        submitErrors = emptyMap()
                                        responderAnswers.clear()
                                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                    }
                                    is FormSubmitResult.ValidationError -> {
                                        submitErrors = result.fieldErrors
                                        submitResultMsg = "Please correct errors before submitting."
                                    }
                                }
                            }
                        )
                        2 -> FormAnalyticsTab(
                            form = activeForm,
                            summary = formsEngine.getResponseSummary(activeForm.formId),
                            onExportSpreadsheet = {
                                val exportText = formsEngine.exportResponsesToSpreadsheet(activeForm.formId)
                                Toast.makeText(context, "Exported responses to spreadsheet table", Toast.LENGTH_LONG).show()
                            }
                        )
                        3 -> FormInsertTab(
                            form = activeForm,
                            activeModuleName = activeModuleName,
                            onInsertNativeControls = {
                                val msg = formsEngine.exportFormToDocument(activeForm.formId, null)
                                onInsertFormToDoc(activeForm)
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        )
                        4 -> ProgrammaticFormSdkTab(
                            activeModuleName = activeModuleName,
                            onInsertToDoc = { summaryText ->
                                val schema = formsEngine.createNewForm("Programmatic Form (SDK Ch. 40)", summaryText)
                                onInsertFormToDoc(schema)
                                Toast.makeText(context, "Inserted SDK Form Summary into $activeModuleName", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

// =========================================================
// Tab 0: Form Builder Component
// =========================================================

@Composable
private fun FormBuilderTab(
    form: FormSchema,
    onUpdate: (FormSchema) -> Unit,
    onDelete: (FormSchema) -> Unit
) {
    var title by remember(form) { mutableStateOf(form.title) }
    var description by remember(form) { mutableStateOf(form.description) }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        form.title = it
                        onUpdate(form)
                    },
                    label = { Text("Form Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        form.description = it
                        onUpdate(form)
                    },
                    label = { Text("Form Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Questions (${form.questions.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Button(
                onClick = {
                    form.questions.add(
                        FormQuestion(
                            title = "New Question ${form.questions.size + 1}",
                            type = FormQuestionType.SHORT_ANSWER
                        )
                    )
                    onUpdate(form)
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Question")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(form.questions) { index, q ->
                QuestionEditorCard(
                    index = index,
                    question = q,
                    onQuestionChanged = {
                        onUpdate(form)
                    },
                    onDeleteQuestion = {
                        form.questions.removeAt(index)
                        onUpdate(form)
                    }
                )
            }
        }
    }
}

@Composable
private fun QuestionEditorCard(
    index: Int,
    question: FormQuestion,
    onQuestionChanged: () -> Unit,
    onDeleteQuestion: () -> Unit
) {
    var title by remember(question) { mutableStateOf(question.title) }
    var type by remember(question) { mutableStateOf(question.type) }
    var isRequired by remember(question) { mutableStateOf(question.isRequired) }
    var validationType by remember(question) { mutableStateOf(question.validationType) }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Q${index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Required", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = isRequired,
                        onCheckedChange = {
                            isRequired = it
                            question.isRequired = it
                            onQuestionChanged()
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDeleteQuestion) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    question.title = it
                    onQuestionChanged()
                },
                label = { Text("Question Title") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Type Dropdown Selector
            Text("Question Type:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FormQuestionType.values().forEach { qt ->
                    FilterChip(
                        selected = qt == type,
                        onClick = {
                            type = qt
                            question.type = qt
                            onQuestionChanged()
                        },
                        label = { Text(qt.displayName) }
                    )
                }
            }

            // Options editor for choice types
            if (type == FormQuestionType.MULTIPLE_CHOICE || type == FormQuestionType.CHECKBOXES || type == FormQuestionType.DROPDOWN) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Options:", style = MaterialTheme.typography.labelMedium)
                question.options.forEachIndexed { optIdx, optStr ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = optStr,
                            onValueChange = { newOpt ->
                                val list = question.options.toMutableList()
                                list[optIdx] = newOpt
                                question.options = list
                                onQuestionChanged()
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                if (question.options.size > 1) {
                                    val list = question.options.toMutableList()
                                    list.removeAt(optIdx)
                                    question.options = list
                                    onQuestionChanged()
                                }
                            }
                        ) {
                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = null)
                        }
                    }
                }
                TextButton(
                    onClick = {
                        val list = question.options.toMutableList()
                        list.add("Option ${list.size + 1}")
                        question.options = list
                        onQuestionChanged()
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Add Option")
                }
            }

            // Validation selector
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Validation Rule: ", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.width(4.dp))
                FormValidationType.values().forEach { vt ->
                    AssistChip(
                        onClick = {
                            validationType = vt
                            question.validationType = vt
                            onQuestionChanged()
                        },
                        label = { Text(vt.label) },
                        colors = if (vt == validationType) {
                            ChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                leadingIconContentColor = Color.Unspecified,
                                trailingIconContentColor = Color.Unspecified,
                                disabledContainerColor = Color.Unspecified,
                                disabledLabelColor = Color.Unspecified,
                                disabledLeadingIconContentColor = Color.Unspecified,
                                disabledTrailingIconContentColor = Color.Unspecified
                            )
                        } else AssistChipDefaults.assistChipColors()
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }
    }
}

// =========================================================
// Tab 1: Form Responder / Preview
// =========================================================

@Composable
private fun FormResponderTab(
    form: FormSchema,
    answers: MutableMap<String, Any>,
    submitResultMsg: String?,
    submitErrors: Map<String, String>,
    onSubmit: (Map<String, Any>) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = form.title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = form.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        if (submitResultMsg != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (submitErrors.isEmpty()) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = submitResultMsg,
                        modifier = Modifier.padding(12.dp),
                        color = if (submitErrors.isEmpty()) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        itemsIndexed(form.questions) { index, q ->
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row {
                        Text(
                            text = "${index + 1}. ${q.title}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        if (q.isRequired) {
                            Text(" *", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    if (q.helpText.isNotEmpty()) {
                        Text(q.helpText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val err = submitErrors[q.id]

                    when (q.type) {
                        FormQuestionType.SHORT_ANSWER -> {
                            var text by remember(q.id) { mutableStateOf(answers[q.id]?.toString() ?: "") }
                            OutlinedTextField(
                                value = text,
                                onValueChange = {
                                    text = it
                                    answers[q.id] = it
                                },
                                isError = err != null,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        FormQuestionType.PARAGRAPH -> {
                            var text by remember(q.id) { mutableStateOf(answers[q.id]?.toString() ?: "") }
                            OutlinedTextField(
                                value = text,
                                onValueChange = {
                                    text = it
                                    answers[q.id] = it
                                },
                                isError = err != null,
                                minLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        FormQuestionType.MULTIPLE_CHOICE -> {
                            var selected by remember(q.id) { mutableStateOf(answers[q.id]?.toString() ?: "") }
                            q.options.forEach { opt ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selected = opt
                                            answers[q.id] = opt
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    RadioButton(
                                        selected = selected == opt,
                                        onClick = {
                                            selected = opt
                                            answers[q.id] = opt
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(opt)
                                }
                            }
                        }
                        FormQuestionType.CHECKBOXES -> {
                            val currentList = (answers[q.id] as? List<*>)?.mapNotNull { it?.toString() }?.toMutableList() ?: remember { mutableStateListOf() }
                            q.options.forEach { opt ->
                                val isChecked = currentList.contains(opt)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isChecked) currentList.remove(opt) else currentList.add(opt)
                                            answers[q.id] = currentList.toList()
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            if (checked) currentList.add(opt) else currentList.remove(opt)
                                            answers[q.id] = currentList.toList()
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(opt)
                                }
                            }
                        }
                        FormQuestionType.DROPDOWN -> {
                            var selected by remember(q.id) { mutableStateOf(answers[q.id]?.toString() ?: q.options.firstOrNull() ?: "") }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                q.options.forEach { opt ->
                                    FilterChip(
                                        selected = selected == opt,
                                        onClick = {
                                            selected = opt
                                            answers[q.id] = opt
                                        },
                                        label = { Text(opt) }
                                    )
                                }
                            }
                        }
                        FormQuestionType.RATING -> {
                            var score by remember(q.id) { mutableStateOf(answers[q.id]?.toString()?.toIntOrNull() ?: 5) }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                (1..5).forEach { star ->
                                    IconButton(
                                        onClick = {
                                            score = star
                                            answers[q.id] = star.toString()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (star <= score) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = "$star Stars",
                                            tint = if (star <= score) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text("($score/5)", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        FormQuestionType.DATE -> {
                            var dateStr by remember(q.id) { mutableStateOf(answers[q.id]?.toString() ?: "2026-07-30") }
                            OutlinedTextField(
                                value = dateStr,
                                onValueChange = {
                                    dateStr = it
                                    answers[q.id] = it
                                },
                                label = { Text("YYYY-MM-DD") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        FormQuestionType.TIME -> {
                            var timeStr by remember(q.id) { mutableStateOf(answers[q.id]?.toString() ?: "10:00") }
                            OutlinedTextField(
                                value = timeStr,
                                onValueChange = {
                                    timeStr = it
                                    answers[q.id] = it
                                },
                                label = { Text("HH:mm") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (err != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = { onSubmit(answers) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Submit Response")
            }
        }
    }
}

// =========================================================
// Tab 2: Response Analytics Component
// =========================================================

@Composable
private fun FormAnalyticsTab(
    form: FormSchema,
    summary: FormResponseSummary,
    onExportSpreadsheet: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Responses", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${summary.totalResponses}",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Button(onClick = onExportSpreadsheet) {
                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export to Spreadsheet")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (summary.totalResponses == 0) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("No responses submitted yet. Try submitting a response in the Preview tab!")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(form.questions) { idx, q ->
                    val qSum = summary.questionSummaries[q.id]
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "${idx + 1}. ${q.title}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (qSum != null) {
                                if (qSum.optionCounts.isNotEmpty()) {
                                    qSum.optionCounts.forEach { (opt, count) ->
                                        val pct = if (summary.totalResponses > 0) (count.toFloat() / summary.totalResponses) else 0f
                                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(opt, style = MaterialTheme.typography.bodyMedium)
                                                Text("$count (${(pct * 100).toInt()}%)", style = MaterialTheme.typography.bodySmall)
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            LinearProgressIndicator(
                                                progress = { pct },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                            )
                                        }
                                    }
                                } else if (qSum.sampleAnswers.isNotEmpty()) {
                                    Text("Recent Answers:", style = MaterialTheme.typography.labelSmall)
                                    qSum.sampleAnswers.take(5).forEach { sample ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp)
                                        ) {
                                            Text(sample, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================
// Tab 3: Document Insert Tab
// =========================================================

@Composable
private fun FormInsertTab(
    form: FormSchema,
    activeModuleName: String,
    onInsertNativeControls: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.InsertDriveFile,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Embed Form into $activeModuleName Document",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Inserts native LibreOffice UNO Form Controls (TextFields, RadioButtons, CheckBoxes, DateFields) matching '${form.title}' directly onto your document canvas.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onInsertNativeControls,
            modifier = Modifier.fillMaxWidth(0.8f).height(48.dp)
        ) {
            Icon(Icons.Default.PostAdd, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Insert Native Form Controls")
        }
    }
}

// =========================================================
// Tab 4: SDK Chapter 40 Programmatic Form Builder Tab
// =========================================================

@Composable
private fun ProgrammaticFormSdkTab(
    activeModuleName: String,
    onInsertToDoc: (String) -> Unit
) {
    val formsEngine = remember { PapirusFormsEngine.getInstance() }
    var dbName by remember { mutableStateOf("liang.odb") }
    var buildResult by remember { mutableStateOf<ProgrammaticFormBuildResult?>(null) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LibreOffice SDK Ch. 40: Building a Form Programmatically",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Demonstrates BuildForm.java API: programmatically creating controls (ControlShape, RadioButtons, CommandButtons, DatabaseListBox, GridControl), binding data sources (liang.odb), and attaching UNO event listeners.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = dbName,
                onValueChange = { dbName = it },
                label = { Text("Database Source (.odb)") },
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = {
                    val res = formsEngine.buildFormProgrammatically(databaseName = dbName)
                    buildResult = res
                    Toast.makeText(context, "Built SDK Form with ${res.totalControlsCreated} controls!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.height(56.dp)
            ) {
                Icon(Icons.Default.Build, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Build Form")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        buildResult?.let { res ->
            Text(
                text = "Build Summary: ${res.formName}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text("${res.totalControlsCreated} Controls Created") },
                    leadingIcon = { Icon(Icons.Default.Widgets, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = {},
                    label = { Text("DB: ${res.databaseName}") },
                    leadingIcon = { Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Form Controls & Shapes Created:", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(4.dp))
            res.createdControlsSummary.forEach { item ->
                Text(" • $item", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Bound Tables & SQL Queries:", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(4.dp))
            res.boundTablesAndQueries.forEach { item ->
                Text(" • $item", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Attached UNO Listeners:", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(4.dp))
            res.listenersAttached.forEach { item ->
                Text(" • $item", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Execution Log:", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = res.executionLog,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onInsertToDoc(res.executionLog) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PostAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Insert SDK Form Summary into $activeModuleName")
            }
        }
    }
}
