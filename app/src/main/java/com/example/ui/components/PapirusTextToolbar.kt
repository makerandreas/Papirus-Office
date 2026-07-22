package com.example.ui.components

import androidx.activity.compose.BackHandler
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

    @Composable
    fun Content(
        isListParagraph: Boolean = false,
        isNumberedList: Boolean = false,
        isDictionaryDownloaded: Boolean = true,
        selectedText: String = "",
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
        onGenerateTextClick: () -> Unit = {},
        onProofreadClick: () -> Unit = {},
        onTranslateClick: () -> Unit = {},
        onRewriteClick: (style: String) -> Unit = {}
    ) {
        if (statusState == TextToolbarStatus.Shown) {
            var mode by remember { mutableStateOf(FctMode.COMPACT) }

            BackHandler(enabled = mode != FctMode.COMPACT) {
                mode = FctMode.COMPACT
            }

            val density = LocalDensity.current
            val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
            val screenHeightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }

            val toolbarHeightPx = with(density) { if (mode != FctMode.COMPACT) 280.dp.toPx() else 56.dp.toPx() }
            val minTopMarginPx = with(density) { 70.dp.toPx() }
            val paddingPx = with(density) { 16.dp.toPx() }.toInt()

            val rawY = if (rectState.top - toolbarHeightPx >= minTopMarginPx) {
                (rectState.top - toolbarHeightPx).roundToInt()
            } else {
                (rectState.bottom + with(density) { 8.dp.toPx() }).roundToInt()
            }

            val popupX = rectState.left.roundToInt().coerceIn(
                paddingPx,
                (screenWidthPx - with(density) { 260.dp.toPx() }).toInt().coerceAtLeast(paddingPx)
            )
            val popupY = rawY.coerceIn(
                paddingPx,
                (screenHeightPx - with(density) { 220.dp.toPx() }).toInt().coerceAtLeast(paddingPx)
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
                    dismissOnClickOutside = true
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
                        when (targetMode) {
                            FctMode.COMPACT -> {
                                // MODE 1: COMPACT BAR
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (onCut != null) {
                                        FctIconButton(Icons.Default.ContentCut, stringResource(R.string.fct_cut)) {
                                            val action = onCut
                                            hide()
                                            action?.invoke()
                                        }
                                    }
                                    if (onCopy != null) {
                                        FctIconButton(Icons.Default.ContentCopy, stringResource(R.string.fct_copy)) {
                                            val action = onCopy
                                            hide()
                                            action?.invoke()
                                        }
                                    }
                                    if (onPaste != null) {
                                        FctIconButton(Icons.Default.ContentPaste, stringResource(R.string.fct_paste)) {
                                            val action = onPaste
                                            hide()
                                            action?.invoke()
                                        }
                                    }
                                    VerticalDivider(
                                        modifier = Modifier.height(24.dp).padding(horizontal = 2.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                    if (onSelectAll != null) {
                                        FctIconButton(Icons.Default.SelectAll, stringResource(R.string.fct_select_all)) {
                                            val action = onSelectAll
                                            hide()
                                            action?.invoke()
                                        }
                                    }
                                    FctIconButton(Icons.Default.AutoAwesome, "AI Options") {
                                        mode = FctMode.AI_OPTIONS
                                    }
                                    FctIconButton(Icons.Default.MoreVert, stringResource(R.string.fct_more)) {
                                        mode = FctMode.GENERAL
                                    }
                                }
                            }
                            FctMode.GENERAL -> {
                                // MODE 2: EXPANDED GENERAL MENU
                                Column(
                                    modifier = Modifier
                                        .width(260.dp)
                                        .heightIn(max = 320.dp)
                                        .verticalScroll(rememberScrollState())
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { mode = FctMode.COMPACT },
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
                                            text = "General Options",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    // Character Submenu
                                    var expandCharacter by remember { mutableStateOf(false) }
                                    FctMenuItem(
                                        icon = Icons.Default.TextFields,
                                        label = "Character",
                                        trailingIcon = if (expandCharacter) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        onClick = { expandCharacter = !expandCharacter }
                                    )
                                    if (expandCharacter) {
                                        FctSubMenuItem("Character Style...") {
                                            hide()
                                            onCharacterStyleClick()
                                        }
                                        FctSubMenuItem("Character Options...") {
                                            hide()
                                            onCharacterOptionsClick()
                                        }
                                    }

                                    // Paragraph Submenu
                                    var expandParagraph by remember { mutableStateOf(false) }
                                    FctMenuItem(
                                        icon = Icons.Default.FormatAlignLeft,
                                        label = "Paragraph",
                                        trailingIcon = if (expandParagraph) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        onClick = { expandParagraph = !expandParagraph }
                                    )
                                    if (expandParagraph) {
                                        FctSubMenuItem("Paragraph Style...") {
                                            hide()
                                            onParagraphStyleClick()
                                        }
                                        FctSubMenuItem("Paragraph Options...") {
                                            hide()
                                            onParagraphOptionsClick()
                                        }
                                    }

                                    FctMenuItem(
                                        icon = Icons.Default.Layers,
                                        label = "Section Options...",
                                        onClick = {
                                            hide()
                                            onSectionOptionsClick()
                                        }
                                    )

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

                                    FctMenuItem(
                                        icon = Icons.Default.FormatListBulleted,
                                        label = "Bullets and Numbering Options",
                                        onClick = {
                                            hide()
                                            onBulletsNumberingOptionsClick()
                                        }
                                    )
                                    FctMenuItem(
                                        icon = Icons.Default.FormatListBulleted,
                                        label = "Skip Numbering",
                                        enabled = isListParagraph,
                                        onClick = {
                                            hide()
                                            onSkipNumberingClick()
                                        }
                                    )
                                    FctMenuItem(
                                        icon = Icons.Default.FormatListBulleted,
                                        label = "Remove Numbering",
                                        enabled = isListParagraph,
                                        onClick = {
                                            hide()
                                            onRemoveNumberingClick()
                                        }
                                    )
                                    FctMenuItem(
                                        icon = Icons.Default.FormatListNumbered,
                                        label = "Restart from beginning",
                                        enabled = isListParagraph && isNumberedList,
                                        onClick = {
                                            hide()
                                            onRestartFromBeginningClick()
                                        }
                                    )

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

                                    FctMenuItem(
                                        icon = Icons.Default.Tab,
                                        label = "Tabs settings",
                                        onClick = {
                                            hide()
                                            onTabsSettingsClick()
                                        }
                                    )
                                    FctMenuItem(
                                        icon = Icons.Default.BorderAll,
                                        label = "Border settings",
                                        onClick = {
                                            hide()
                                            onBorderSettingsClick()
                                        }
                                    )
                                    FctMenuItem(
                                        icon = Icons.Default.FormatColorFill,
                                        label = "Shading settings",
                                        onClick = {
                                            hide()
                                            onShadingSettingsClick()
                                        }
                                    )

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

                                    // Synonyms Submenu
                                    var expandSynonyms by remember { mutableStateOf(false) }
                                    FctMenuItem(
                                        icon = Icons.Default.Spellcheck,
                                        label = "Synonyms",
                                        enabled = isDictionaryDownloaded,
                                        trailingIcon = if (expandSynonyms) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        onClick = { if (isDictionaryDownloaded) expandSynonyms = !expandSynonyms }
                                    )
                                    if (expandSynonyms && isDictionaryDownloaded) {
                                        val synonymsList = remember(selectedText) {
                                            getSynonymsForText(selectedText)
                                        }
                                        synonymsList.forEach { synonym ->
                                            FctSubMenuItem(synonym) {
                                                hide()
                                                onSynonymSelected(synonym)
                                            }
                                        }
                                    }
                                }
                            }
                            FctMode.AI_OPTIONS -> {
                                // MODE 3: EXPANDED AI OPTIONS MENU
                                Column(
                                    modifier = Modifier
                                        .width(260.dp)
                                        .heightIn(max = 320.dp)
                                        .verticalScroll(rememberScrollState())
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { mode = FctMode.COMPACT },
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
                                            text = "AI Options",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
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

private fun getSynonymsForText(text: String): List<String> {
    val clean = text.trim().lowercase()
    return when {
        clean.contains("dokumen") || clean.contains("file") || clean.contains("naskah") -> listOf("Berkas", "Naskah", "Arsip", "Dokumentasi", "Catatan")
        clean.contains("teks") || clean.contains("kata") || clean.contains("tulisan") -> listOf("Kalimat", "Wacana", "Paragraf", "Redaksi", "Penggalan")
        clean.contains("buat") || clean.contains("kerja") -> listOf("Susun", "Ciptakan", "Gagas", "Hasilkan", "Gubah")
        else -> listOf("Padanan 1", "Padanan 2", "Padanan 3", "Persamaan Kata")
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
