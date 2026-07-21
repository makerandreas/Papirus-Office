package com.example.modules.inky

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

// Helper item for displaying standard M3 Expressive list rows
@Composable
fun M3ListItem(
    headlineText: String,
    supportingText: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Box(modifier = Modifier.padding(end = 16.dp)) {
                    leadingIcon()
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = headlineText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (supportingText != null) {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (trailingContent != null) {
                Box(modifier = Modifier.padding(start = 16.dp)) {
                    trailingContent()
                }
            }
        }
    }
}

// 3-Column action row component for compact tools
@Composable
fun ThreeColumnRow(
    col1: @Composable RowScope.() -> Unit,
    col2: @Composable RowScope.() -> Unit,
    col3: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Row { col1() }
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Row { col2() }
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Row { col3() }
        }
    }
}

// HomeSubpage - Main Dashboard Tab
@Composable
fun HomeSubpage(
    context: Context,
    isBold: Boolean,
    onBoldChange: (Boolean) -> Unit,
    isItalic: Boolean,
    onItalicChange: (Boolean) -> Unit,
    isUnderline: Boolean,
    onUnderlineChange: (Boolean) -> Unit,
    isStrikethrough: Boolean,
    onStrikethroughChange: (Boolean) -> Unit,
    activeFontFamily: String,
    activeFontSize: Int,
    fontColor: Color,
    highlightColor: Color,
    textAlignment: TextAlign,
    onTextAlignmentChange: (TextAlign) -> Unit,
    onNavigateSubpage: (String) -> Unit,
    onShowFontSizeDialog: () -> Unit
) {
    var showCapitalizationMenu by remember { mutableStateOf(false) }
    var showRtlState by remember { mutableStateOf(false) }
    var showParagraphMarks by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // --- GRUP EDIT ---
        Text(
            text = "Edit",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // Paste item with trailing options
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Click to Paste
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            Toast.makeText(context, "Menempelkan teks dari papan klip...", Toast.LENGTH_SHORT).show()
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.ContentPaste,
                        contentDescription = "Paste",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Text(
                        "Paste",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Vertical Divider & Chevron to Paste Options
                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                IconButton(
                    onClick = { onNavigateSubpage("paste_options") },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = "Paste Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Row containing Cut, Copy, Format Painter
        ThreeColumnRow(
            col1 = {
                OutlinedButton(
                    onClick = { Toast.makeText(context, "Potong teks (Cut)", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.fillMaxWidth().testTag("home_cut_btn"),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Rounded.ContentCut, contentDescription = "Cut", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cut", fontSize = 11.sp)
                }
            },
            col2 = {
                OutlinedButton(
                    onClick = { Toast.makeText(context, "Salin teks (Copy)", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.fillMaxWidth().testTag("home_copy_btn"),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy", fontSize = 11.sp)
                }
            },
            col3 = {
                OutlinedButton(
                    onClick = { Toast.makeText(context, "Penyalin format (Painter)", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.fillMaxWidth().testTag("home_painter_btn"),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Rounded.FormatPaint, contentDescription = "Format Painter", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Painter", fontSize = 11.sp)
                }
            }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // --- GRUP CHARACTER ---
        Text(
            text = "Character",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // Font style selector (1 baris 2 kolom size representation)
        M3ListItem(
            headlineText = "Font style",
            supportingText = activeFontFamily,
            leadingIcon = { Icon(Icons.Rounded.FontDownload, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
            trailingContent = { Icon(Icons.Rounded.ChevronRight, contentDescription = "Pilih Font") },
            onClick = { onNavigateSubpage("font_style") }
        )

        // Font size selector
        M3ListItem(
            headlineText = "Font size",
            supportingText = "$activeFontSize pt",
            leadingIcon = { Icon(Icons.Rounded.FormatSize, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
            trailingContent = { Icon(Icons.Rounded.Edit, contentDescription = "Ubah Ukuran Font") },
            onClick = onShowFontSizeDialog
        )

        // Bold, Italic, Underline (with options)
        ThreeColumnRow(
            col1 = {
                IconToggleButton(
                    checked = isBold,
                    onCheckedChange = onBoldChange,
                    modifier = Modifier.fillMaxWidth(),
                    colors = IconButtonDefaults.iconToggleButtonColors(
                        checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.FormatBold, contentDescription = "Bold")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bold", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            },
            col2 = {
                IconToggleButton(
                    checked = isItalic,
                    onCheckedChange = onItalicChange,
                    modifier = Modifier.fillMaxWidth(),
                    colors = IconButtonDefaults.iconToggleButtonColors(
                        checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.FormatItalic, contentDescription = "Italic")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Italic", fontWeight = FontWeight.Normal, fontSize = 12.sp)
                    }
                }
            },
            col3 = {
                // Custom Underline action row with vertical divider & chevron right
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isUnderline) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { onUnderlineChange(!isUnderline) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Rounded.FormatUnderlined,
                                contentDescription = "Underline",
                                tint = if (isUnderline) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Box(
                            modifier = Modifier
                                .height(20.dp)
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                        IconButton(
                            onClick = { onNavigateSubpage("underline_options") },
                            modifier = Modifier.width(36.dp)
                        ) {
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = "Underline Options",
                                modifier = Modifier.size(16.dp),
                                tint = if (isUnderline) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        )

        // Strikethrough, Subscript, Superscript row
        ThreeColumnRow(
            col1 = {
                IconToggleButton(
                    checked = isStrikethrough,
                    onCheckedChange = onStrikethroughChange,
                    modifier = Modifier.fillMaxWidth(),
                    colors = IconButtonDefaults.iconToggleButtonColors(
                        checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.StrikethroughS, contentDescription = "Strikethrough")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Strike", fontSize = 11.sp)
                    }
                }
            },
            col2 = {
                OutlinedButton(
                    onClick = { Toast.makeText(context, "Subscript applied", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    Icon(Icons.Rounded.Subscript, contentDescription = "Subscript", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Sub", fontSize = 10.sp)
                }
            },
            col3 = {
                OutlinedButton(
                    onClick = { Toast.makeText(context, "Superscript applied", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    Icon(Icons.Rounded.Superscript, contentDescription = "Superscript", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Super", fontSize = 10.sp)
                }
            }
        )

        // Change Capitalization Subpage Link
        M3ListItem(
            headlineText = "Change Capitalization",
            supportingText = "Sesuaikan huruf kapital",
            leadingIcon = { Icon(Icons.Rounded.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
            trailingContent = { Icon(Icons.Rounded.ChevronRight, contentDescription = "Change Capitalization Options") },
            onClick = { onNavigateSubpage("change_capitalization") }
        )

        // Font Color
        M3ListItem(
            headlineText = "Font Color",
            supportingText = "Ganti warna teks",
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(fontColor)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
            },
            trailingContent = { Icon(Icons.Rounded.ChevronRight, contentDescription = "Pilih Warna") },
            onClick = { onNavigateSubpage("font_color") }
        )

        // Highlight Text Color
        M3ListItem(
            headlineText = "Highlight Text Color",
            supportingText = "Beri warna stabilo teks",
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (highlightColor == Color.Transparent) Color.LightGray else highlightColor)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
            },
            trailingContent = { Icon(Icons.Rounded.ChevronRight, contentDescription = "Pilih Warna") },
            onClick = { onNavigateSubpage("highlight_color") }
        )

        // Delete formatting
        M3ListItem(
            headlineText = "Delete all formatting",
            supportingText = "Kembalikan format ke default",
            leadingIcon = { Icon(Icons.Rounded.FormatClear, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            onClick = {
                onBoldChange(false)
                onItalicChange(false)
                onUnderlineChange(false)
                onStrikethroughChange(false)
                Toast.makeText(context, "Semua format dihapus!", Toast.LENGTH_SHORT).show()
            }
        )

        // Character Options
        M3ListItem(
            headlineText = "Character Options",
            supportingText = "Pengaturan karakter tingkat lanjut",
            leadingIcon = { Icon(Icons.Rounded.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
            onClick = { Toast.makeText(context, "Character Options akan dikembangkan segera", Toast.LENGTH_SHORT).show() }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // --- GRUP PARAGRAPH ---
        Text(
            text = "Paragraph",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // Alignment Row (4 Columns)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val alignments = listOf(
                TextAlign.Left to Icons.Rounded.FormatAlignLeft,
                TextAlign.Center to Icons.Rounded.FormatAlignCenter,
                TextAlign.Right to Icons.Rounded.FormatAlignRight,
                TextAlign.Justify to Icons.Rounded.FormatAlignJustify
            )
            alignments.forEach { (align, icon) ->
                val isSelected = textAlignment == align
                IconButton(
                    onClick = { onTextAlignmentChange(align) },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                        .border(1.dp, if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        icon,
                        contentDescription = "Align ${align.toString()}",
                        tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Indent Row (2 Columns)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { Toast.makeText(context, "Indentasi Bertambah", Toast.LENGTH_SHORT).show() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.FormatIndentIncrease, contentDescription = "Increase Indent")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Increase Indent", fontSize = 11.sp)
            }
            OutlinedButton(
                onClick = { Toast.makeText(context, "Indentasi Berkurang", Toast.LENGTH_SHORT).show() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.FormatIndentDecrease, contentDescription = "Decrease Indent")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Decrease Indent", fontSize = 11.sp)
            }
        }

        // Set line spacing
        M3ListItem(
            headlineText = "Set line spacing",
            supportingText = "Atur jarak antar baris",
            leadingIcon = { Icon(Icons.Rounded.FormatLineSpacing, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
            trailingContent = { Icon(Icons.Rounded.ChevronRight, contentDescription = "Ubah Jarak") },
            onClick = { onNavigateSubpage("line_spacing") }
        )

        // Create bulleted list
        M3ListItem(
            headlineText = "Create bulleted list",
            supportingText = "Paragraf daftar berpoin",
            leadingIcon = { Icon(Icons.Rounded.FormatListBulleted, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
            trailingContent = { Icon(Icons.Rounded.ChevronRight, contentDescription = "Pilih Poin") },
            onClick = { onNavigateSubpage("bulleted_list") }
        )

        // Create numbered list
        M3ListItem(
            headlineText = "Create numbered list",
            supportingText = "Paragraf daftar bernomor",
            leadingIcon = { Icon(Icons.Rounded.FormatListNumbered, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
            trailingContent = { Icon(Icons.Rounded.ChevronRight, contentDescription = "Pilih Nomor") },
            onClick = { onNavigateSubpage("numbered_list") }
        )

        // Create multilevel list
        M3ListItem(
            headlineText = "Create multilevel list",
            supportingText = "Daftar bertingkat",
            leadingIcon = { Icon(Icons.Rounded.FormatListBulleted, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) }, // icon representation
            trailingContent = { Icon(Icons.Rounded.ChevronRight, contentDescription = "Pilih Multilevel") },
            onClick = { onNavigateSubpage("multilevel_list") }
        )

        // Toggle paragraph marks
        M3ListItem(
            headlineText = "Toggle paragraph marks",
            supportingText = "Tampilkan penanda paragraf",
            leadingIcon = { Icon(Icons.Rounded.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
            trailingContent = {
                Switch(
                    checked = showParagraphMarks,
                    onCheckedChange = { showParagraphMarks = it }
                )
            },
            onClick = { showParagraphMarks = !showParagraphMarks }
        )

        // Sort text/table
        M3ListItem(
            headlineText = "Sort text/table",
            supportingText = "Urutkan teks atau tabel",
            leadingIcon = { Icon(Icons.Rounded.SortByAlpha, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
            onClick = { Toast.makeText(context, "Mengurutkan dokumen...", Toast.LENGTH_SHORT).show() }
        )

        // Toggle RTL writing direction
        M3ListItem(
            headlineText = "Toggle RTL writing direction",
            supportingText = "Ganti penulisan Kanan-ke-Kiri",
            leadingIcon = { Icon(Icons.Rounded.FormatTextdirectionRToL, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
            trailingContent = {
                Switch(
                    checked = showRtlState,
                    onCheckedChange = { showRtlState = it }
                )
            },
            onClick = { showRtlState = !showRtlState }
        )

        // Paragraph Shading Color
        M3ListItem(
            headlineText = "Paragraph Shading Color",
            supportingText = "Warna shading latar paragraf",
            leadingIcon = { Icon(Icons.Rounded.FormatColorFill, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
            trailingContent = { Icon(Icons.Rounded.ChevronRight, contentDescription = "Pilih Warna Shading") },
            onClick = { onNavigateSubpage("paragraph_shading") }
        )

        // Paragraph Border
        M3ListItem(
            headlineText = "Paragraph Border",
            supportingText = "Atur batas / bingkai paragraf",
            leadingIcon = { Icon(Icons.Rounded.BorderAll, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
            trailingContent = { Icon(Icons.Rounded.ChevronRight, contentDescription = "Atur Bingkai") },
            onClick = { onNavigateSubpage("paragraph_border") }
        )

        // Paragraph Options
        M3ListItem(
            headlineText = "Paragraph Options",
            supportingText = "Opsi paragraf tingkat lanjut",
            leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
            onClick = { Toast.makeText(context, "Paragraph Options akan segera dikembangkan", Toast.LENGTH_SHORT).show() }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // --- GRUP STYLES ---
        Text(
            text = "Styles",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // Style selector
        M3ListItem(
            headlineText = "Style selector",
            supportingText = "Pilih gaya pemformatan paragraf",
            leadingIcon = { Icon(Icons.Rounded.Style, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
            trailingContent = { Icon(Icons.Rounded.ChevronRight, contentDescription = "Buka Gaya Paragraf") },
            onClick = { onNavigateSubpage("paragraph_styles") }
        )

        // Paragraph Style Options
        M3ListItem(
            headlineText = "Paragraph Style Options",
            supportingText = "Pengaturan gaya paragraf",
            leadingIcon = { Icon(Icons.Rounded.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
            onClick = { Toast.makeText(context, "Paragraph Style Options akan dikembangkan segera", Toast.LENGTH_SHORT).show() }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// 1. PasteOptionsSubpage
@Composable
fun PasteOptionsSubpage(context: Context, onShowPasteSpecial: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        M3ListItem(
            headlineText = "Keep source formatting",
            supportingText = "Pertahankan gaya format asli dari sumber",
            leadingIcon = { Icon(Icons.Rounded.Brush, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            onClick = { Toast.makeText(context, "Teks ditempelkan dengan Keep Source Formatting", Toast.LENGTH_SHORT).show() }
        )
        M3ListItem(
            headlineText = "Merge formatting",
            supportingText = "Gabungkan format sumber dengan format dokumen",
            leadingIcon = { Icon(Icons.Rounded.MergeType, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            onClick = { Toast.makeText(context, "Teks ditempelkan dengan Merge Formatting", Toast.LENGTH_SHORT).show() }
        )
        M3ListItem(
            headlineText = "Paste unformatted text",
            supportingText = "Tempelkan teks murni tanpa format apapun",
            leadingIcon = { Icon(Icons.Rounded.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            onClick = { Toast.makeText(context, "Teks murni berhasil ditempelkan", Toast.LENGTH_SHORT).show() }
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
        
        M3ListItem(
            headlineText = "Paste Special...",
            supportingText = "Pilihan pemformatan lanjutan",
            leadingIcon = { Icon(Icons.Rounded.SettingsApplications, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
            onClick = {
                onShowPasteSpecial()
            }
        )
    }
}

// 2. FontStyleSubpage
@Composable
fun FontStyleSubpage(
    context: Context,
    currentFont: String,
    onFontSelected: (String) -> Unit
) {
    val fonts = listOf(
        "Aptos Display", "Calibri", "Arial", "Roboto", "Space Grotesk",
        "JetBrains Mono", "Montserrat", "Playfair Display", "Inter",
        "Merriweather", "Lora", "Open Sans", "Poppins"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Google Fonts Database",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        fonts.forEach { fontName ->
            val isSelected = fontName == currentFont
            M3ListItem(
                headlineText = fontName,
                supportingText = if (isSelected) "Aktif" else "Klik untuk memilih",
                leadingIcon = {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        modifier = Modifier.size(24.dp)
                    )
                },
                onClick = {
                    onFontSelected(fontName)
                    Toast.makeText(context, "Font diubah ke $fontName", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

// 3. UnderlineOptionsSubpage
@Composable
fun UnderlineOptionsSubpage(
    context: Context,
    currentUnderlineState: Boolean,
    onOpenColorPage: () -> Unit
) {
    val lineStyles = listOf(
        "Single Line", "Double Line", "Thick Line", "Dotted Line",
        "Dashed Line", "Dot-Dashed Line", "Wave Line", "Double Wave Line"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        M3ListItem(
            headlineText = "Underline color",
            supportingText = "Tentukan warna garis bawah",
            leadingIcon = { Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingContent = { Icon(Icons.Rounded.ChevronRight, contentDescription = "Warna") },
            onClick = onOpenColorPage
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Text(
            text = "Line Style",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        lineStyles.forEach { style ->
            M3ListItem(
                headlineText = style,
                supportingText = "Terapkan gaya garis bawah ini",
                leadingIcon = { Icon(Icons.Rounded.HorizontalRule, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                onClick = {
                    Toast.makeText(context, "Gaya garis bawah $style diterapkan", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

// 4. LineSpacingSubpage
@Composable
fun LineSpacingSubpage(context: Context) {
    val options = listOf(
        "Single", "1,16 line", "1,5 line", "Double",
        "Proportional", "At least", "Leading", "Fixed"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        options.forEach { opt ->
            M3ListItem(
                headlineText = opt,
                supportingText = "Pilih spasi baris $opt",
                leadingIcon = { Icon(Icons.Rounded.LineStyle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                onClick = {
                    Toast.makeText(context, "Spasi $opt dipilih", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

// 5. BulletedListSubpage
@Composable
fun BulletedListSubpage(context: Context) {
    val variants = listOf(
        "Circle filled", "Circle outlined", "Rectangle filled",
        "Rectangle outlined", "Rhombus filled", "Rhombus outlined"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        variants.forEach { variant ->
            M3ListItem(
                headlineText = variant,
                supportingText = "Terapkan penanda berpoin ini",
                leadingIcon = { Icon(Icons.Rounded.RadioButtonChecked, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                onClick = {
                    Toast.makeText(context, "Bulleted $variant berhasil diterapkan", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

// 6. NumberedListSubpage
@Composable
fun NumberedListSubpage(context: Context) {
    val variants = listOf(
        "1.", "1)", "(1)", "A.", "a.", "a)", "(a)", "i.", "i)", "(i)"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        variants.forEach { variant ->
            M3ListItem(
                headlineText = "Format $variant",
                supportingText = "Gunakan pengurutan ini",
                leadingIcon = { Icon(Icons.Rounded.FormatListNumbered, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                onClick = {
                    Toast.makeText(context, "Numbered $variant berhasil diterapkan", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

// 7. MultilevelListSubpage
@Composable
fun MultilevelListSubpage(context: Context) {
    val variants = listOf(
        "1. > 1.1 > 1.1.1",
        "1. > A. > a.",
        "1) > a) > i)",
        "Chapter 1 > Section 1 > Subsection 1"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        variants.forEach { variant ->
            M3ListItem(
                headlineText = variant,
                supportingText = "Gunakan skema daftar bertingkat ini",
                leadingIcon = { Icon(Icons.Rounded.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                onClick = {
                    Toast.makeText(context, "Multilevel list $variant berhasil diterapkan", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

// 8. ParagraphBorderSubpage
@Composable
fun ParagraphBorderSubpage(context: Context) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // No border option
        M3ListItem(
            headlineText = "No border",
            supportingText = "Tanpa garis bingkai",
            leadingIcon = { Icon(Icons.Rounded.BorderClear, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            onClick = { Toast.makeText(context, "Bingkai dihapus", Toast.LENGTH_SHORT).show() }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Normal Category (4 columns representation)
        Text(
            text = "Kategori Normal",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        ThreeColumnRow(
            col1 = {
                OutlinedButton(onClick = { Toast.makeText(context, "Garis Atas Normal", Toast.LENGTH_SHORT).show() }) {
                    Icon(Icons.Rounded.BorderTop, contentDescription = "Top")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Top", fontSize = 11.sp)
                }
            },
            col2 = {
                OutlinedButton(onClick = { Toast.makeText(context, "Garis Bawah Normal", Toast.LENGTH_SHORT).show() }) {
                    Icon(Icons.Rounded.BorderBottom, contentDescription = "Bottom")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bottom", fontSize = 11.sp)
                }
            },
            col3 = {
                OutlinedButton(onClick = { Toast.makeText(context, "Garis Samping Normal", Toast.LENGTH_SHORT).show() }) {
                    Icon(Icons.Rounded.BorderOuter, contentDescription = "Borders")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sides", fontSize = 11.sp)
                }
            }
        )

        // Thick Category
        Text(
            text = "Kategori Tebal (Thick)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        ThreeColumnRow(
            col1 = {
                Button(onClick = { Toast.makeText(context, "Garis Atas Tebal", Toast.LENGTH_SHORT).show() }) {
                    Icon(Icons.Rounded.BorderTop, contentDescription = "Top Thick")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Thick Top", fontSize = 10.sp)
                }
            },
            col2 = {
                Button(onClick = { Toast.makeText(context, "Garis Bawah Tebal", Toast.LENGTH_SHORT).show() }) {
                    Icon(Icons.Rounded.BorderBottom, contentDescription = "Bottom Thick")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Thick Bot", fontSize = 10.sp)
                }
            },
            col3 = {
                Button(onClick = { Toast.makeText(context, "Bingkai Luar Tebal", Toast.LENGTH_SHORT).show() }) {
                    Icon(Icons.Rounded.BorderOuter, contentDescription = "All Thick")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Thick Box", fontSize = 10.sp)
                }
            }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Grid Box models
        M3ListItem(
            headlineText = "Box and grid",
            supportingText = "Terapkan kotak dan kisi lengkap",
            leadingIcon = { Icon(Icons.Rounded.GridView, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
            onClick = { Toast.makeText(context, "Bingkai Box and Grid diterapkan", Toast.LENGTH_SHORT).show() }
        )
        M3ListItem(
            headlineText = "Box",
            supportingText = "Terapkan kotak luar saja",
            leadingIcon = { Icon(Icons.Rounded.CheckBoxOutlineBlank, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
            onClick = { Toast.makeText(context, "Bingkai Box luar diterapkan", Toast.LENGTH_SHORT).show() }
        )
        M3ListItem(
            headlineText = "Inside (Grid)",
            supportingText = "Hanya garis grid bagian dalam",
            leadingIcon = { Icon(Icons.Rounded.GridGoldenratio, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
            onClick = { Toast.makeText(context, "Bingkai Grid dalam diterapkan", Toast.LENGTH_SHORT).show() }
        )
    }
}

// 9. ParagraphStylesSubpage
@Composable
fun ParagraphStylesSubpage(
    context: Context,
    selectedStyle: String,
    onNavigateStyleOptions: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        M3ListItem(
            headlineText = "Normal",
            supportingText = "Gaya dasar dokumen standar",
            leadingIcon = { Icon(Icons.Rounded.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingContent = { Icon(Icons.Rounded.ChevronRight, contentDescription = "Options") },
            onClick = { onNavigateStyleOptions("Normal") }
        )
    }
}

// 10. CreateNewStyleSubpage
@Composable
fun CreateNewStyleSubpage(
    context: Context,
    onSuccess: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        M3ListItem(
            headlineText = "Create New",
            supportingText = "Buat gaya kustom baru dari awal",
            leadingIcon = { Icon(Icons.Rounded.AddBox, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            onClick = {
                Toast.makeText(context, "Gaya baru berhasil dibuat", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
        )
        M3ListItem(
            headlineText = "Create New from Text",
            supportingText = "Gunakan format teks terpilih saat ini",
            leadingIcon = { Icon(Icons.Rounded.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            onClick = {
                Toast.makeText(context, "Gaya baru dari teks terpilih berhasil disimpan", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
        )
    }
}

// 11. StyleOptionsSubpage
@Composable
fun StyleOptionsSubpage(
    context: Context,
    styleName: String,
    onSuccess: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        M3ListItem(
            headlineText = "Edit",
            supportingText = "Modifikasi parameter gaya $styleName",
            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            onClick = {
                Toast.makeText(context, "Mengedit gaya $styleName...", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
        )
        M3ListItem(
            headlineText = "Update from Text",
            supportingText = "Perbarui gaya $styleName menggunakan format teks terpilih",
            leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            onClick = {
                Toast.makeText(context, "Gaya $styleName diperbarui", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
        )
        if (styleName != "Normal") {
            M3ListItem(
                headlineText = "Delete",
                supportingText = "Hapus gaya ini secara permanen",
                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                onClick = {
                    Toast.makeText(context, "Gaya $styleName berhasil dihapus", Toast.LENGTH_SHORT).show()
                    onSuccess()
                }
            )
        }
    }
}

// 12. Expressive Custom Color Picker Page
@Composable
fun ColorPickerSubpage(
    currentColor: Color,
    title: String,
    onColorSelected: (Color) -> Unit
) {
    var r by remember { mutableStateOf((currentColor.red * 255).toInt()) }
    var g by remember { mutableStateOf((currentColor.green * 255).toInt()) }
    var b by remember { mutableStateOf((currentColor.blue * 255).toInt()) }
    var hex by remember { mutableStateOf(String.format("#%02X%02X%02X", r, g, b)) }

    LaunchedEffect(r, g, b) {
        val newColor = Color(r, g, b)
        hex = String.format("#%02X%02X%02X", r, g, b)
        onColorSelected(newColor)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Preset Pilihan Warna:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        val presets = listOf(
            Color.Red, Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
            Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF00BCD4), Color(0xFF009688),
            Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFCDDC39), Color(0xFFFFEB3B),
            Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFFFF5722), Color(0xFF795548),
            Color.Black, Color.DarkGray, Color.Gray, Color.LightGray, Color.White
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(presets) { color ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            2.dp,
                            if (currentColor == color) MaterialTheme.colorScheme.primary else Color.Transparent,
                            CircleShape
                        )
                        .clickable {
                            r = (color.red * 255).toInt()
                            g = (color.green * 255).toInt()
                            b = (color.blue * 255).toInt()
                        }
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Text("Material 3 Expressive Custom Color Picker:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(r, g, b))
                    .border(1.dp, MaterialTheme.colorScheme.outline)
            )
            Column {
                Text("Preview Warna", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(hex, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // R Slider
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("R", modifier = Modifier.width(16.dp), fontWeight = FontWeight.Bold, color = Color.Red)
                Slider(
                    value = r.toFloat(),
                    onValueChange = { r = it.toInt() },
                    valueRange = 0f..255f,
                    modifier = Modifier.weight(1f)
                )
                Text(r.toString(), modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
            }
            // G Slider
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("G", modifier = Modifier.width(16.dp), fontWeight = FontWeight.Bold, color = Color.Green)
                Slider(
                    value = g.toFloat(),
                    onValueChange = { g = it.toInt() },
                    valueRange = 0f..255f,
                    modifier = Modifier.weight(1f)
                )
                Text(g.toString(), modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
            }
            // B Slider
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("B", modifier = Modifier.width(16.dp), fontWeight = FontWeight.Bold, color = Color.Blue)
                Slider(
                    value = b.toFloat(),
                    onValueChange = { b = it.toInt() },
                    valueRange = 0f..255f,
                    modifier = Modifier.weight(1f)
                )
                Text(b.toString(), modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
            }
        }

        OutlinedTextField(
            value = hex,
            onValueChange = { input ->
                if (input.length <= 7) {
                    hex = input
                    try {
                        val parsed = Color(android.graphics.Color.parseColor(input))
                        r = (parsed.red * 255).toInt()
                        g = (parsed.green * 255).toInt()
                        b = (parsed.blue * 255).toInt()
                    } catch (e: Exception) {
                        // ignore invalid format during typing
                    }
                }
            },
            label = { Text("Kode HEX") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace)
        )
    }
}

// 13. Material 3 Expressive Font Size Dialog
@Composable
fun FontSizeDialog(
    currentSize: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var sizeInput by remember { mutableStateOf(currentSize.toString()) }
    var sizeValue by remember { mutableStateOf(currentSize.toFloat()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Ubah Ukuran Font",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Font size field
                OutlinedTextField(
                    value = sizeInput,
                    onValueChange = { input ->
                        sizeInput = input
                        input.toIntOrNull()?.let {
                            if (it in 1..200) {
                                sizeValue = it.toFloat()
                            }
                        }
                    },
                    label = { Text("Font size (pt)") },
                    modifier = Modifier.fillMaxWidth().testTag("font_size_input_field"),
                    singleLine = true
                )

                // Font size slider (max 96)
                Text(
                    text = "Geser untuk atur (maks. 96 pt):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = sizeValue.coerceIn(1f, 96f),
                    onValueChange = {
                        sizeValue = it
                        sizeInput = it.toInt().toString()
                    },
                    valueRange = 1f..96f,
                    modifier = Modifier.fillMaxWidth().testTag("font_size_slider_widget")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            sizeInput.toIntOrNull()?.let {
                                onConfirm(it)
                            } ?: onConfirm(sizeValue.toInt())
                        },
                        modifier = Modifier.testTag("font_size_confirm_ok")
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

// 14. Paste Special Dialog
@Composable
fun PasteSpecialDialog(
    onDismiss: () -> Unit,
    onPasteSuccess: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Paste Special",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Pilih format penempelan lanjutan:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val formats = listOf(
                    "HTML Format",
                    "Rich Text (RTF)",
                    "Unicode Text (UTF-8)",
                    "Unformatted Unicode Text",
                    "Device Independent Bitmap"
                )

                formats.forEach { fmt ->
                    OutlinedButton(
                        onClick = {
                            onPasteSuccess(fmt)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(fmt)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
fun ChangeCapitalizationSubpage(context: Context) {
    Column(modifier = Modifier.fillMaxWidth()) {
        M3ListItem(
            headlineText = "First Character Uppercase",
            supportingText = "Huruf pertama menjadi kapital",
            leadingIcon = { Icon(Icons.Rounded.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
            onClick = {
                Toast.makeText(context, "Huruf pertama menjadi kapital", Toast.LENGTH_SHORT).show()
            }
        )
        M3ListItem(
            headlineText = "First word uppercase",
            supportingText = "Kata pertama menjadi kapital",
            leadingIcon = { Icon(Icons.Rounded.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
            onClick = {
                Toast.makeText(context, "Kata pertama menjadi kapital", Toast.LENGTH_SHORT).show()
            }
        )
        M3ListItem(
            headlineText = "ALL UPPERCASE",
            supportingText = "SEMUA HURUF BESAR",
            leadingIcon = { Icon(Icons.Rounded.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
            onClick = {
                Toast.makeText(context, "SEMUA HURUF BESAR", Toast.LENGTH_SHORT).show()
            }
        )
        M3ListItem(
            headlineText = "all lowercase",
            supportingText = "semua huruf kecil",
            leadingIcon = { Icon(Icons.Rounded.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
            onClick = {
                Toast.makeText(context, "semua huruf kecil", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

