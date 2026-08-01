package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.R
import kotlin.math.roundToInt

enum class FctMode {
    COMPACT,
    GENERAL,
    CHARACTER,
    PARAGRAPH,
    SYNONYMS,
    SELECTION_MODE,
    AI_OPTIONS
}

class PapirusTextToolbar : TextToolbar {

    private var statusState by mutableStateOf(TextToolbarStatus.Hidden)
    private var rectState by mutableStateOf(Rect.Zero)

    // Callbacks for text actions
    var onCopy: (() -> Unit)? = null
    var onCut: (() -> Unit)? = null
    var onPaste: (() -> Unit)? = null
    var onSelectAll: (() -> Unit)? = null

    override val status: TextToolbarStatus
        get() = statusState

    override fun hide() {
        statusState = TextToolbarStatus.Hidden
    }

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        rectState = rect
        onCopy = onCopyRequested
        onCut = onCutRequested
        onPaste = onPasteRequested
        onSelectAll = onSelectAllRequested
        statusState = TextToolbarStatus.Shown
    }

    fun show(rect: Rect) {
        rectState = rect
        statusState = TextToolbarStatus.Shown
    }

    @Composable
    fun Content(
        isEditMode: Boolean = true,
        isListParagraph: Boolean = false,
        isNumberedList: Boolean = false,
        isDictionaryDownloaded: Boolean = true,
        selectedText: String = "",
        hasClipboardContent: Boolean = true,
        isBottomBarShowing: Boolean = false,
        onCharacterStyleClick: () -> Unit = {},
        onCharacterOptionsClick: () -> Unit = {},
        onParagraphStyleClick: () -> Unit = {},
        onParagraphOptionsClick: () -> Unit = {},
        onSectionOptionsClick: () -> Unit = {},
        onBulletsNumberingOptionsClick: () -> Unit = {},
        onSkipNumberingClick: () -> Unit = {},
        onRemoveNumberingClick: () -> Unit = {},
        onRestartFromBeginningClick: () -> Unit = {},
        onTabsSettingsClick: () -> Unit = {},
        onBorderSettingsClick: () -> Unit = {},
        onShadingSettingsClick: () -> Unit = {},
        onSynonymSelected: (String) -> Unit = {},
        onSelectNonContiguousClick: () -> Unit = {},
        onBlockToSelectClick: () -> Unit = {},
        onGenerateTextClick: () -> Unit = {},
        onProofreadClick: () -> Unit = {},
        onTranslateClick: () -> Unit = {},
        onRewriteClick: (style: String) -> Unit = {},
        onSetReminderClick: () -> Unit = {}
    ) {
        if (statusState == TextToolbarStatus.Shown) {
            var mode by remember { mutableStateOf(FctMode.COMPACT) }

            val density = LocalDensity.current
            val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
            val screenHeightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }

            val compactHeightPx = with(density) { 48.dp.toPx() }
            val targetExpandedHeightPx = with(density) { 280.dp.toPx() }
            val gapPx = with(density) { 10.dp.toPx() }
            val minTopMarginPx = with(density) { 110.dp.toPx() }
            val minBottomMarginPx = with(density) { 60.dp.toPx() }
            val paddingPx = with(density) { 16.dp.toPx() }.toInt()

            val spaceAbove = rectState.top - gapPx - minTopMarginPx
            val spaceBelow = screenHeightPx - minBottomMarginPx - (rectState.bottom + gapPx)

            val isAbove = spaceAbove >= targetExpandedHeightPx || (spaceAbove >= compactHeightPx && spaceAbove >= spaceBelow)

            val maxExpandedHeightDp = if (isAbove) {
                with(density) { spaceAbove.coerceIn(140.dp.toPx(), targetExpandedHeightPx).toDp() }
            } else {
                with(density) { spaceBelow.coerceIn(140.dp.toPx(), targetExpandedHeightPx).toDp() }
            }

            val maxExpandedHeightPx = with(density) { maxExpandedHeightDp.toPx() }
            val currentHeightPx = if (mode == FctMode.COMPACT) compactHeightPx else maxExpandedHeightPx

            val rawY = if (isAbove) {
                (rectState.top - currentHeightPx - gapPx).roundToInt()
            } else {
                (rectState.bottom + gapPx).roundToInt()
            }

            val popupX = rectState.left.roundToInt().coerceIn(
                paddingPx,
                (screenWidthPx - with(density) { 260.dp.toPx() }).toInt().coerceAtLeast(paddingPx)
            )
            val popupY = rawY.coerceIn(
                paddingPx,
                (screenHeightPx - currentHeightPx - paddingPx).toInt().coerceAtLeast(paddingPx)
            )

            Popup(
                offset = IntOffset(x = popupX, y = popupY),
                onDismissRequest = { 
                    mode = FctMode.COMPACT
                    hide() 
                },
                properties = PopupProperties(
                    focusable = false,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = !isBottomBarShowing
                )
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 6.dp,
                    shadowElevation = 10.dp
                ) {
                    AnimatedContent(
                        targetState = mode,
                        label = "FCTModeTransition"
                    ) { targetMode ->
                        val hasSelection = selectedText.isNotEmpty()
                        val synonymsList = remember(selectedText) { getSynonymsForText(selectedText) }
                        val hasSynonyms = hasSelection && synonymsList.isNotEmpty()

                        when (targetMode) {
                            FctMode.COMPACT -> {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isEditMode) {
                                        // Editor Mode Compact
                                        if (hasSelection && onCut != null) {
                                            FctIconButton(Icons.Default.ContentCut, stringResource(R.string.fct_cut)) {
                                                val action = onCut
                                                hide()
                                                action?.invoke()
                                            }
                                        }
                                        if (hasSelection && onCopy != null) {
                                            FctIconButton(Icons.Default.ContentCopy, stringResource(R.string.fct_copy)) {
                                                val action = onCopy
                                                hide()
                                                action?.invoke()
                                            }
                                        }
                                        if (hasClipboardContent && onPaste != null) {
                                            FctIconButton(Icons.Default.ContentPaste, stringResource(R.string.fct_paste)) {
                                                val action = onPaste
                                                hide()
                                                action?.invoke()
                                            }
                                        }
                                        if (hasSelection) {
                                            VerticalDivider(
                                                modifier = Modifier.height(24.dp).padding(horizontal = 2.dp),
                                                color = MaterialTheme.colorScheme.outlineVariant
                                            )
                                            FctIconButton(Icons.Default.Delete, "Delete") {
                                                val action = onCut
                                                hide()
                                                action?.invoke()
                                            }
                                        }
                                        if (onSelectAll != null) {
                                            FctIconButton(Icons.Default.SelectAll, stringResource(R.string.fct_select_all)) {
                                                val action = onSelectAll
                                                hide()
                                                action?.invoke()
                                            }
                                        }
                                        FctIconButton(Icons.Default.AutoAwesome, "AI options") {
                                            mode = FctMode.AI_OPTIONS
                                        }
                                        FctIconButton(Icons.Default.MoreVert, stringResource(R.string.fct_more)) {
                                            mode = FctMode.GENERAL
                                        }
                                    } else {
                                        // Viewer Mode Compact
                                        if (hasSelection && onCopy != null) {
                                            FctIconButton(Icons.Default.ContentCopy, stringResource(R.string.fct_copy)) {
                                                val action = onCopy
                                                hide()
                                                action?.invoke()
                                            }
                                        }
                                        if (onSelectAll != null) {
                                            FctIconButton(Icons.Default.SelectAll, stringResource(R.string.fct_select_all)) {
                                                val action = onSelectAll
                                                hide()
                                                action?.invoke()
                                            }
                                        }
                                    }
                                }
                            }

                            FctMode.GENERAL -> {
                                Column(
                                    modifier = Modifier
                                        .width(260.dp)
                                        .heightIn(max = maxExpandedHeightDp)
                                        .verticalScroll(rememberScrollState())
                                        .padding(vertical = 4.dp)
                                ) {
                                    FctHeader("General Options") { mode = FctMode.COMPACT }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    // Selection Mode... [1]
                                    if (hasSelection) {
                                        FctMenuItem(
                                            icon = Icons.Default.SelectAll,
                                            label = "Selection Mode...",
                                            enabled = true,
                                            trailingIcon = Icons.Default.KeyboardArrowRight,
                                            onClick = { mode = FctMode.SELECTION_MODE }
                                        )
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                    }

                                    // Character -> Character Mode
                                    FctMenuItem(
                                        icon = Icons.Default.TextFields,
                                        label = "Character",
                                        trailingIcon = Icons.Default.KeyboardArrowRight,
                                        onClick = { mode = FctMode.CHARACTER }
                                    )

                                    // Paragraph -> Paragraph Mode
                                    FctMenuItem(
                                        icon = Icons.Default.FormatAlignLeft,
                                        label = "Paragraph",
                                        trailingIcon = Icons.Default.KeyboardArrowRight,
                                        onClick = { mode = FctMode.PARAGRAPH }
                                    )

                                    // Section Options...
                                    FctMenuItem(
                                        icon = Icons.Default.Layers,
                                        label = "Section Options...",
                                        onClick = {
                                            hide()
                                            onSectionOptionsClick()
                                        }
                                    )

                                    // Set Reminder
                                    FctMenuItem(
                                        icon = Icons.Default.AddAlert,
                                        label = "Set Reminder",
                                        onClick = {
                                            hide()
                                            onSetReminderClick()
                                        }
                                    )

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

                                    // Bullets and Numbering Options...
                                    FctMenuItem(
                                        icon = Icons.Default.FormatListBulleted,
                                        label = "Bullets and Numbering Options...",
                                        onClick = {
                                            hide()
                                            onBulletsNumberingOptionsClick()
                                        }
                                    )
                                    // Skip Numbering [2]
                                    FctMenuItem(
                                        icon = Icons.Default.FormatListBulleted,
                                        label = "Skip Numbering",
                                        enabled = isListParagraph,
                                        onClick = {
                                            hide()
                                            onSkipNumberingClick()
                                        }
                                    )
                                    // Remove Numbering [2]
                                    FctMenuItem(
                                        icon = Icons.Default.FormatListBulleted,
                                        label = "Remove Numbering",
                                        enabled = isListParagraph,
                                        onClick = {
                                            hide()
                                            onRemoveNumberingClick()
                                        }
                                    )
                                    // Restart from Beginning [3]
                                    FctMenuItem(
                                        icon = Icons.Default.FormatListNumbered,
                                        label = "Restart from Beginning",
                                        enabled = isNumberedList,
                                        onClick = {
                                            hide()
                                            onRestartFromBeginningClick()
                                        }
                                    )

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

                                    // Tab Settings...
                                    FctMenuItem(
                                        icon = Icons.Default.Tab,
                                        label = "Tab Settings...",
                                        onClick = {
                                            hide()
                                            onTabsSettingsClick()
                                        }
                                    )
                                    // Border Settings...
                                    FctMenuItem(
                                        icon = Icons.Default.BorderAll,
                                        label = "Border Settings...",
                                        onClick = {
                                            hide()
                                            onBorderSettingsClick()
                                        }
                                    )
                                    // Shading Settings...
                                    FctMenuItem(
                                        icon = Icons.Default.FormatColorFill,
                                        label = "Shading Settings...",
                                        onClick = {
                                            hide()
                                            onShadingSettingsClick()
                                        }
                                    )

                                    if (hasSynonyms) {
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

                                        // Synonyms [4]
                                        FctMenuItem(
                                            icon = Icons.Default.Spellcheck,
                                            label = "Synonyms",
                                            enabled = isDictionaryDownloaded,
                                            trailingIcon = Icons.Default.KeyboardArrowRight,
                                            onClick = { mode = FctMode.SYNONYMS }
                                        )
                                    }
                                }
                            }

                            FctMode.CHARACTER -> {
                                Column(
                                    modifier = Modifier
                                        .width(260.dp)
                                        .heightIn(max = maxExpandedHeightDp)
                                        .verticalScroll(rememberScrollState())
                                        .padding(vertical = 4.dp)
                                ) {
                                    FctHeader("Character") { mode = FctMode.GENERAL }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    FctMenuItem(
                                        icon = Icons.Default.Style,
                                        label = "Character Style...",
                                        onClick = {
                                            hide()
                                            onCharacterStyleClick()
                                        }
                                    )
                                    FctMenuItem(
                                        icon = Icons.Default.Tune,
                                        label = "Character Options...",
                                        onClick = {
                                            hide()
                                            onCharacterOptionsClick()
                                        }
                                    )
                                }
                            }

                            FctMode.PARAGRAPH -> {
                                Column(
                                    modifier = Modifier
                                        .width(260.dp)
                                        .heightIn(max = maxExpandedHeightDp)
                                        .verticalScroll(rememberScrollState())
                                        .padding(vertical = 4.dp)
                                ) {
                                    FctHeader("Paragraph") { mode = FctMode.GENERAL }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    FctMenuItem(
                                        icon = Icons.Default.Style,
                                        label = "Paragraph Style...",
                                        onClick = {
                                            hide()
                                            onParagraphStyleClick()
                                        }
                                    )
                                    FctMenuItem(
                                        icon = Icons.Default.Tune,
                                        label = "Paragraph Options...",
                                        onClick = {
                                            hide()
                                            onParagraphOptionsClick()
                                        }
                                    )
                                }
                            }

                            FctMode.SYNONYMS -> {
                                Column(
                                    modifier = Modifier
                                        .width(260.dp)
                                        .heightIn(max = maxExpandedHeightDp)
                                        .verticalScroll(rememberScrollState())
                                        .padding(vertical = 4.dp)
                                ) {
                                    FctHeader("Synonyms") { mode = FctMode.GENERAL }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    if (synonymsList.isEmpty()) {
                                        FctMenuItem(
                                            icon = Icons.Default.Spellcheck,
                                            label = "No synonyms available",
                                            enabled = false,
                                            onClick = {}
                                        )
                                    } else {
                                        synonymsList.forEach { synonym ->
                                            FctMenuItem(
                                                icon = Icons.Default.Translate,
                                                label = synonym,
                                                onClick = {
                                                    hide()
                                                    onSynonymSelected(synonym)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            FctMode.SELECTION_MODE -> {
                                Column(
                                    modifier = Modifier
                                        .width(260.dp)
                                        .heightIn(max = maxExpandedHeightDp)
                                        .verticalScroll(rememberScrollState())
                                        .padding(vertical = 4.dp)
                                ) {
                                    FctHeader("Selection Mode") { mode = FctMode.GENERAL }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    FctMenuItem(
                                        icon = Icons.Default.TouchApp,
                                        label = "Select non-contiguous text",
                                        onClick = {
                                            hide()
                                            onSelectNonContiguousClick()
                                        }
                                    )
                                    FctMenuItem(
                                        icon = Icons.Default.CropFree,
                                        label = "Block to select",
                                        onClick = {
                                            hide()
                                            onBlockToSelectClick()
                                        }
                                    )
                                }
                            }

                            FctMode.AI_OPTIONS -> {
                                Column(
                                    modifier = Modifier
                                        .width(260.dp)
                                        .heightIn(max = maxExpandedHeightDp)
                                        .verticalScroll(rememberScrollState())
                                        .padding(vertical = 4.dp)
                                ) {
                                    FctHeader("AI Options") { mode = FctMode.COMPACT }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    FctMenuItem(
                                        icon = Icons.Default.AutoAwesome,
                                        label = "Generate Text",
                                        onClick = {
                                            hide()
                                            onGenerateTextClick()
                                        }
                                    )

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

                                    FctMenuItem(
                                        icon = Icons.Default.Spellcheck,
                                        label = "Proofread",
                                        onClick = {
                                            hide()
                                            onProofreadClick()
                                        }
                                    )
                                    FctMenuItem(
                                        icon = Icons.Default.Translate,
                                        label = "Translate",
                                        onClick = {
                                            hide()
                                            onTranslateClick()
                                        }
                                    )

                                    // Rewrite Submenu
                                    var expandRewrite by remember { mutableStateOf(false) }
                                    FctMenuItem(
                                        icon = Icons.Default.EditNote,
                                        label = "Rewrite",
                                        trailingIcon = if (expandRewrite) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        onClick = { expandRewrite = !expandRewrite }
                                    )
                                    if (expandRewrite) {
                                        val rewriteStyles = listOf("Lucu", "Profesional", "Akademis", "Naratif")
                                        rewriteStyles.forEach { style ->
                                            FctSubMenuItem(style) {
                                                hide()
                                                onRewriteClick(style)
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
}

@Composable
private fun FctHeader(
    title: String,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun getSynonymsForText(text: String): List<String> {
    val clean = text.trim().lowercase()
    if (clean.isEmpty()) return emptyList()
    return when {
        clean.contains("dokumen") || clean.contains("file") || clean.contains("naskah") -> listOf("Berkas", "Naskah", "Arsip", "Dokumentasi", "Catatan")
        clean.contains("teks") || clean.contains("kata") || clean.contains("tulisan") -> listOf("Kalimat", "Wacana", "Paragraf", "Redaksi", "Penggalan")
        clean.contains("buat") || clean.contains("kerja") -> listOf("Susun", "Ciptakan", "Gagas", "Hasilkan", "Gubah")
        clean.contains("hello") || clean.contains("halo") -> listOf("Hai", "Salam", "Greetings", "Sapaan")
        else -> listOf("Persamaan 1", "Persamaan 2", "Sinonim Kata")
    }
}

@Composable
private fun FctIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun FctMenuItem(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    trailingIcon: ImageVector? = null,
    onClick: () -> Unit
) {
    val contentColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            modifier = Modifier.weight(1f)
        )
        if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun FctSubMenuItem(
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 36.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "• $label",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

