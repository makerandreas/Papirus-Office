package com.example.ui.options

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ai.GeminiAiService
import com.example.ui.theme.ThemeSettings

import androidx.compose.ui.res.stringResource
import com.example.R

data class OptionItem(
    val id: String,
    val title: String,
    val description: String? = null,
    val icon: ImageVector = Icons.Rounded.Settings
)

data class OptionGroup(
    val groupKey: String,
    val title: String,
    val items: List<OptionItem>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PapirusOfficeOptionsScreen(
    sourceModule: String = "general", // "general", "Inky", "Cellina", "Slidia"
    onCloseOptions: () -> Unit,
    onDynamicColorChange: ((Boolean) -> Unit)? = null,
    onRestartRequested: (() -> Unit)? = null
) {
    val context = LocalContext.current

    // Active subpage state: null means Main Options list
    var activeSubpage by remember { mutableStateOf<OptionItem?>(null) }
    var activeSubSubpage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }

    // Intercept back button
    BackHandler {
        if (activeSubSubpage != null) {
            activeSubSubpage = null
        } else if (activeSubpage != null) {
            activeSubpage = null
        } else {
            onCloseOptions()
        }
    }

    // Settings Groups Definition
    val generalGroup = OptionGroup(
        groupKey = "general_group",
        title = stringResource(R.string.group_papirus_settings),
        items = listOf(
            OptionItem("user_data", "User data", "Personalize user information & author credits", Icons.Rounded.Person),
            OptionItem("general", "General", "Basic application preferences and startup settings", Icons.Rounded.Settings),
            OptionItem("view", "View", "Screen layout, ruler, grid, and display preferences", Icons.Rounded.Visibility),
            OptionItem("print", "Print", "Default printer setup, paper format, and margins", Icons.Rounded.Print),
            OptionItem("paths", "Paths", "Storage locations for templates, backups, and drafts", Icons.Rounded.Folder),
            OptionItem("fonts", "Fonts", "Font replacement and substitution settings", Icons.Rounded.FontDownload),
            OptionItem("security", "Security", "Encryption, password protection, and macro safety", Icons.Rounded.Security),
            OptionItem("appearance", "Appearance", "Theme colors, dark mode, and Material You dynamic color", Icons.Rounded.Palette),
            OptionItem("accessibility", "Accessibility", "High contrast mode, screen reader, and zoom scaling", Icons.Rounded.Accessibility),
            OptionItem("advanced", "Advanced", "Experimental features, Java runtime, and memory optimization", Icons.Rounded.Tune),
            OptionItem("online_update", "Online Update", "Automatic updates and patch check frequency", Icons.Rounded.Update),
            OptionItem("opencl", "OpenCL", "Hardware acceleration and GPU computation", Icons.Rounded.Memory)
        )
    )

    val loadingSavingGroup = OptionGroup(
        groupKey = "loading_saving_group",
        title = stringResource(R.string.group_load_and_save),
        items = listOf(
            OptionItem("load_save_general", "General", "Auto-save intervals, backup creation, and default ODF versions", Icons.Rounded.Save),
            OptionItem("microsoft_office", "Microsoft Office", "DOCX, XLSX, PPTX conversion and compatibility rules", Icons.Rounded.Description)
        )
    )

    val languagesGroup = OptionGroup(
        groupKey = "languages_group",
        title = "Languages and Locales",
        items = listOf(
            OptionItem("lang_general", "General", "UI language, locale, currency, and date formats", Icons.Rounded.Language),
            OptionItem("writing_aids", "Writing Aids", "Spell checking dictionaries, thesaurus, and hyphenation", Icons.Rounded.Spellcheck),
            OptionItem("lang_dict", "Installing language dictionaries", "Download or import offline language dictionaries", Icons.Rounded.Download),
            OptionItem("search_japanese", "Searching in Japanese", "Kanji, Kana, and Japanese search matching rules", Icons.Rounded.Translate),
            OptionItem("asian_layout", "Asian layout", "Typography, line breaking, and spacing for Asian scripts", Icons.Rounded.FormatSize),
            OptionItem("ctl_layout", "Complex Text Layout", "Bidirectional text, Arabic, and Indic typography rules", Icons.Rounded.FormatAlignRight),
            OptionItem("lang_tool_server", "Language tool server", "Grammar checker API server connection", Icons.Rounded.Dns),
            OptionItem("english_sentence", "English sentence checking", "Punctuation, spacing, and style checker rules", Icons.Rounded.CheckCircle)
        )
    )

    val inkyGroup = OptionGroup(
        groupKey = "inky_group",
        title = "Inky Settings",
        items = listOf(
            OptionItem("inky_general", "General", "Inky Writer editor preferences and default view mode", Icons.Rounded.EditNote),
            OptionItem("inky_view", "View", "Rulers, boundaries, non-printing characters, and zoom", Icons.Rounded.Preview),
            OptionItem("inky_formatting", "Formatting Aids", "Tab stops, paragraph spacing indicators, and hidden text", Icons.Rounded.FormatAlignLeft),
            OptionItem("inky_grid", "Grid", "Snap to grid, page alignment grid spacing", Icons.Rounded.GridOn),
            OptionItem("inky_basic_fonts", "Basic Fonts", "Default fonts for Western, Asian, and CTL documents", Icons.Rounded.Title),
            OptionItem("inky_print", "Print", "Print hidden text, page background, and annotations", Icons.Rounded.Print),
            OptionItem("inky_table", "Table", "Default table borders, padding, and auto-fit rules", Icons.Rounded.TableChart),
            OptionItem("inky_changes", "Changes", "Track changes highlights, author colors, and comments", Icons.Rounded.History),
            OptionItem("inky_comparison", "Comparison", "Document comparison and revision merge options", Icons.Rounded.Compare),
            OptionItem("inky_compatibility", "Compatibility", "Spacing rules for legacy Word files", Icons.Rounded.Assistant),
            OptionItem("inky_autocaption", "AutoCaption", "Automatic captioning for tables, images, and frames", Icons.Rounded.Subtitles),
            OptionItem("inky_mail_merge", "Mail Merge Email", "SMTP configuration for bulk email document merge", Icons.Rounded.Email)
        )
    )

    val cellinaGroup = OptionGroup(
        groupKey = "cellina_group",
        title = "Cellina Settings",
        items = listOf(
            OptionItem("cellina_general", "General", "Measurement units and tab distance", Icons.Rounded.TableRows),
            OptionItem("cellina_defaults", "Defaults", "Default number of sheets in new spreadsheets", Icons.Rounded.LibraryAdd),
            OptionItem("cellina_view", "View", "Grid lines, formula bar, column/row headers", Icons.Rounded.GridView),
            OptionItem("cellina_calculate", "Calculate", "Iterative references, precision, and date baseline", Icons.Rounded.Calculate),
            OptionItem("cellina_formula", "Formula", "Formula syntax (Calc A1 / Excel A1), capitalization", Icons.Rounded.Functions),
            OptionItem("cellina_sort_lists", "Sort Lists", "Custom sort lists (Days, Months, Custom series)", Icons.Rounded.Sort),
            OptionItem("cellina_changes", "Changes", "Track sheet modifications and cell edit history", Icons.Rounded.Edit),
            OptionItem("cellina_compatibility", "Compatibility", "Key bindings and formula evaluation mode", Icons.Rounded.SwapHoriz),
            OptionItem("cellina_grid", "Grid", "Grid line color and snap options", Icons.Rounded.Grid4x4),
            OptionItem("cellina_print", "Print", "Page order, grid printing, and header/footer margins", Icons.Rounded.Print)
        )
    )

    val slidiaGroup = OptionGroup(
        groupKey = "slidia_group",
        title = "Slidia Settings",
        items = listOf(
            OptionItem("slidia_general", "General", "Slide transition speeds and presentation defaults", Icons.Rounded.Slideshow),
            OptionItem("slidia_view", "View", "Rulers, guides, master slide layout, and slide pane", Icons.Rounded.ViewCarousel),
            OptionItem("slidia_grid", "Grid", "Snap objects to grid, guide line alignment", Icons.Rounded.Grid3x3),
            OptionItem("slidia_print", "Print", "Handouts, speaker notes, and slide frame printing", Icons.Rounded.Print)
        )
    )

    val allGroups = remember(sourceModule) {
        val baseGroups = mutableListOf(generalGroup, loadingSavingGroup, languagesGroup)
        when (sourceModule) {
            "Inky" -> baseGroups.add(inkyGroup)
            "Cellina" -> baseGroups.add(cellinaGroup)
            "Slidia" -> baseGroups.add(slidiaGroup)
        }
        baseGroups
    }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text(stringResource(R.string.restart_required_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.restart_required_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        showRestartDialog = false
                        onCloseOptions()
                        onRestartRequested?.invoke()
                    },
                    modifier = Modifier.testTag("btn_restart_now")
                ) {
                    Text(stringResource(R.string.restart_now), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showRestartDialog = false
                        onCloseOptions()
                    },
                    modifier = Modifier.testTag("btn_restart_later")
                ) {
                    Text(stringResource(R.string.restart_later))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            // GOOGLE PIXEL SYSTEM SETTINGS STYLED APP BAR
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Leading Button: Back Icon
                    IconButton(
                        onClick = {
                            if (activeSubSubpage != null) {
                                activeSubSubpage = null
                            } else if (activeSubpage != null) {
                                activeSubpage = null
                            } else {
                                onCloseOptions()
                            }
                        },
                        modifier = Modifier.testTag("btn_options_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Headline Title (No Subtitle)
                    val headerTitle = when {
                        activeSubSubpage == "auto_recovery" -> "Save Auto Recovery Options"
                        activeSubSubpage == "backup_copies" -> "Always Create Backup Copy"
                        activeSubpage != null -> activeSubpage!!.title
                        else -> "Papirus Office Options"
                    }

                    Text(
                        text = headerTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    // Trailing Action Group (Horizontal row with 2 buttons: Done & More Options)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 1. "Done" Button: SHOWN ONLY IN SUBPAGES (Wide, filled color)
                        // When clicked, completes applying changes and closes entire options screen and subpages
                        if (activeSubpage != null) {
                            Button(
                                onClick = {
                                    if (activeSubpage?.id == "load_save_general") {
                                        showRestartDialog = true
                                    } else {
                                        onCloseOptions()
                                    }
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("btn_options_done")
                            ) {
                                Text(stringResource(R.string.options_done), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        // 2. "More Options" Button: SHOWN ON BOTH MAIN PAGE AND SUBPAGES
                        Box {
                            IconButton(
                                onClick = { showMoreMenu = true },
                                modifier = Modifier.testTag("btn_options_more")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More Options",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.options_help)) },
                                    leadingIcon = { Icon(Icons.Rounded.HelpOutline, contentDescription = null) },
                                    onClick = {
                                        showMoreMenu = false
                                        showHelpDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.options_reset)) },
                                    leadingIcon = { Icon(Icons.Rounded.RestartAlt, contentDescription = null) },
                                    onClick = {
                                        showMoreMenu = false
                                        Toast.makeText(context, "Settings reset to Papirus Office defaults", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = activeSubpage,
                transitionSpec = {
                    if (targetState != null && initialState == null) {
                        // Entering subpage: Slide up vertically + slide in horizontally
                        (slideInHorizontally { width -> width / 2 } + slideInVertically { height -> height / 10 } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width / 3 } + fadeOut()
                        )
                    } else if (targetState == null && initialState != null) {
                        // Exiting subpage (Predictive Back Gesture): Slide back horizontally + slide down vertically
                        (slideInHorizontally { width -> -width / 3 } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width / 2 } + slideOutVertically { height -> height / 10 } + fadeOut()
                        )
                    } else {
                        (fadeIn() + scaleIn(initialScale = 0.96f)).togetherWith(
                            fadeOut() + scaleOut(targetScale = 0.96f)
                        )
                    }
                },
                label = "OptionsSubpageTransition"
            ) { subpage ->
                if (subpage == null) {
                    // MAIN OPTIONS SCREEN - Pixel Settings Style List
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 24.dp)
                    ) {
                        // Search bar inside settings
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search settings...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(28.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )

                        // Filtered or Grouped Options
                        allGroups.forEach { group ->
                            val filteredItems = group.items.filter {
                                searchQuery.isEmpty() ||
                                        it.title.contains(searchQuery, ignoreCase = true) ||
                                        (it.description?.contains(searchQuery, ignoreCase = true) == true)
                            }

                            if (filteredItems.isNotEmpty()) {
                                CategoryHeader(title = group.title)

                                Column(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                ) {
                                    filteredItems.forEachIndexed { index, item ->
                                        SettingItemRow(
                                            item = item,
                                            onClick = {
                                                activeSubpage = item
                                                activeSubSubpage = null
                                            }
                                        )
                                        if (index < filteredItems.size - 1) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(start = 56.dp),
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                } else {
                    // SUBPAGE SCREEN VIEW
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Interactive Subpage Controls based on setting type
                        when (subpage.id) {
                            "load_save_general" -> {
                                LoadSaveGeneralSubpage(
                                    activeSubSubpage = activeSubSubpage,
                                    onNavigateSubSubpage = { activeSubSubpage = it }
                                )
                            }
                            "appearance" -> {
                                DynamicColorSettingCard(
                                    context = context,
                                    onDynamicColorChange = onDynamicColorChange
                                )
                            }
                            "security" -> {
                                SecuritySettingCard(context = context)
                            }
                            else -> {
                                // Default Empty / Placeholder Subpage Card
                                EmptySubpageCard(subpage = subpage)
                            }
                        }
                    }
                }
            }
        }
    }

    // Contextual Help Dialog
    if (showHelpDialog) {
        val (helpTitle, helpBody) = when {
            activeSubpage?.id == "load_save_general" -> {
                when (activeSubSubpage) {
                    "auto_recovery" -> Pair(
                        stringResource(R.string.help_dialog_auto_recovery_title),
                        stringResource(R.string.help_dialog_auto_recovery_body)
                    )
                    "backup_copies" -> Pair(
                        stringResource(R.string.help_dialog_backup_copy_title),
                        stringResource(R.string.help_dialog_backup_copy_body)
                    )
                    else -> Pair(
                        stringResource(R.string.help_dialog_load_save_title),
                        stringResource(R.string.help_dialog_load_save_body)
                    )
                }
            }
            activeSubpage != null -> Pair(
                "Help: ${activeSubpage!!.title}",
                "Configurations changed in '${activeSubpage!!.title}' take effect immediately without requiring an application restart."
            )
            else -> Pair(
                stringResource(R.string.help_dialog_main_title),
                stringResource(R.string.help_dialog_main_body)
            )
        }

        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            icon = { Icon(Icons.Rounded.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(helpTitle) },
            text = {
                Text(
                    text = helpBody,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun CategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingItemRow(
    item: OptionItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!item.description.isNullOrEmpty()) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun DynamicColorSettingCard(
    context: Context,
    onDynamicColorChange: ((Boolean) -> Unit)?
) {
    var isDynamicEnabled by remember {
        mutableStateOf(ThemeSettings.isDynamicColorEnabled(context))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Appearance & Theme",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dynamic Color (Material You)",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Use system wallpaper accent colors (Android 12+). Changes take effect immediately.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isDynamicEnabled,
                    enabled = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S,
                    onCheckedChange = { isChecked ->
                        isDynamicEnabled = isChecked
                        ThemeSettings.setDynamicColorEnabled(context, isChecked)
                        onDynamicColorChange?.invoke(isChecked)
                        Toast.makeText(context, "Dynamic Color ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
private fun SecuritySettingCard(context: Context) {
    var macroProtection by remember { mutableStateOf(true) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Security Preferences",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Macro Execution Warning", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text("Prompt before executing document macros", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = macroProtection,
                    onCheckedChange = {
                        macroProtection = it
                        Toast.makeText(context, "Macro warning updated", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptySubpageCard(subpage: OptionItem) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = subpage.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = subpage.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!subpage.description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subpage.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Subpage configuration settings for '${subpage.title}' are active. Changes apply immediately upon selection.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
