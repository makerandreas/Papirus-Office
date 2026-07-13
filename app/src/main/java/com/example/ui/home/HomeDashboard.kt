package com.example.ui.home

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ai.GeminiAiService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboard(
    isTablet: Boolean,
    onNavigateToModule: (String) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Opt-in AI preferences state
    var isAiEnabled by remember { mutableStateOf(GeminiAiService.isAiEnabled(context)) }
    var apiKeyInput by remember { mutableStateOf(GeminiAiService.getUserApiKey(context)) }
    var selectedModel by remember { mutableStateOf(GeminiAiService.getSelectedModel(context)) }
    var showModelMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Modern Bento-style Header Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Dark modern bento logo with rotated square inside
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF1A1C1E), shape = RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .border(2.dp, Color.White, RoundedCornerShape(3.dp))
                                .graphicsLayer(rotationZ = 12f)
                        )
                    }
                    Text(
                        text = "Papirus Office",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        letterSpacing = (-0.5).sp
                    )
                }

                // Profile initials avatar "MA"
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFE2E8F0), shape = RoundedCornerShape(20.dp))
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "MA",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                }
            }

            // Version tags & packages
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFDBEAFE), shape = RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "V1.2.4-NIGHTLY",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D4ED8)
                    )
                }
                Text(
                    text = "com.makerandreas.papirusoffice",
                    fontSize = 10.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        // Section: Suite Modules Grid
        Text(
            text = "OFFICE WORKSPACES",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Bento 2x2 Grid: Always symmetric and perfectly spaced
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ModuleBentoCard(
                        title = "Inky",
                        subtitle = "ODF/DOCX Editor",
                        brandColor = Color(0xFF2563EB),
                        bgColor = Color(0xFFEFF6FF),
                        icon = Icons.Default.Description
                    ) { onNavigateToModule("Inky") }
                }
                Box(modifier = Modifier.weight(1f)) {
                    ModuleBentoCard(
                        title = "Cellina",
                        subtitle = "ODS/XLSX Sheets",
                        brandColor = Color(0xFF10B981),
                        bgColor = Color(0xFFECFDF5),
                        icon = Icons.Default.GridView
                    ) { onNavigateToModule("Cellina") }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ModuleBentoCard(
                        title = "Slidia",
                        subtitle = "ODP/PPTX Slides",
                        brandColor = Color(0xFFD97706),
                        bgColor = Color(0xFFFFFBEB),
                        icon = Icons.Default.Slideshow
                    ) { onNavigateToModule("Slidia") }
                }
                Box(modifier = Modifier.weight(1f)) {
                    ModuleBentoCard(
                        title = "Pagella",
                        subtitle = "Advanced PDF",
                        brandColor = Color(0xFFE11D48),
                        bgColor = Color(0xFFFFF1F2),
                        icon = Icons.Default.PictureAsPdf
                    ) { onNavigateToModule("Pagella") }
                }
            }
        }

        // Section: Google Gemini Assistant Opt-In Panel
        Text(
            text = "AI COMPANION CONFIGURATION (OPT-IN)",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Gemini Purple Bento Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE7E0FF)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFFD0C4F2)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("ai_config_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Opt-in toggle row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Shadow white circle container for AI symbol
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.White, shape = RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI",
                                tint = Color(0xFF6750A4),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Ask Gemini Assistant",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF21005D)
                            )
                            Text(
                                "Summarize documents or generate LaTeX equations",
                                fontSize = 11.sp,
                                color = Color(0xFF49454F)
                            )
                        }
                    }
                    Switch(
                        checked = isAiEnabled,
                        onCheckedChange = {
                            isAiEnabled = it
                            GeminiAiService.setAiEnabled(context, it)
                        },
                        modifier = Modifier.testTag("toggle_ai"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF6750A4)
                        )
                    )
                }

                if (isAiEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // API Key Input
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            GeminiAiService.saveUserApiKey(context, it)
                        },
                        label = { Text("Google AI Studio API Key", color = Color(0xFF21005D)) },
                        placeholder = { Text("AIzaSy...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFF6750A4),
                            unfocusedBorderColor = Color(0xFFD0C4F2),
                            focusedLabelColor = Color(0xFF21005D),
                            unfocusedLabelColor = Color(0xFF49454F)
                        ),
                        trailingIcon = {
                            if (apiKeyInput.isNotEmpty()) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Configured", tint = Color(0xFF16A34A))
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Don't have a key? Go to https://aistudio.google.com/ to retrieve your free developer API key.",
                        fontSize = 11.sp,
                        color = Color(0xFF49454F)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Model Selection Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedModel,
                            onValueChange = {},
                            label = { Text("Active LLM Model", color = Color(0xFF21005D)) },
                            readOnly = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFF6750A4),
                                unfocusedBorderColor = Color(0xFFD0C4F2),
                                focusedLabelColor = Color(0xFF21005D),
                                unfocusedLabelColor = Color(0xFF49454F)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showModelMenu = true },
                            trailingIcon = {
                                IconButton(onClick = { showModelMenu = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Model", tint = Color(0xFF6750A4))
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = showModelMenu,
                            onDismissRequest = { showModelMenu = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            GeminiAiService.SUPPORTED_MODELS.forEach { modelPair ->
                                DropdownMenuItem(
                                    text = { Text(modelPair.second) },
                                    onClick = {
                                        selectedModel = modelPair.first
                                        GeminiAiService.saveSelectedModel(context, modelPair.first)
                                        showModelMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 14.dp),
                    color = Color(0xFFD0C4F2).copy(alpha = 0.5f)
                )

                // Zero Data Retention Policy Notice
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.4f), shape = RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = "Privacy Policy",
                            tint = Color(0xFF21005D),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Zero Data Retention & Privacy Policy",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF21005D)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "1. Security: Credentials are stored locally via Android Encrypted SharedPreferences; they are never sent to external telemetry systems.\n" +
                               "2. Privacy: All user documents are fully ephemeral. Our zero-data-retention system instructions forbid Gemini from caching, storing, or logging any document buffers.",
                        fontSize = 10.sp,
                        color = Color(0xFF49454F),
                        lineHeight = 14.sp
                    )
                }
            }
        }

        // Section: Recent Documents
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RECENT DOCUMENTS",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "View All",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2563EB),
                modifier = Modifier.clickable {
                    Toast.makeText(context, "Start Center: All files are managed locally.", Toast.LENGTH_SHORT).show()
                }
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            // Document 1
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(0.5.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onNavigateToModule("Inky")
                        Toast.makeText(context, "Opening Report_Q3_Analysis.docx...", Toast.LENGTH_SHORT).show()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFDBEAFE), shape = RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("W", fontWeight = FontWeight.Bold, color = Color(0xFF2563EB), fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Report_Q3_Analysis.docx",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Edited 2h ago • 2.4 MB",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = "Open", tint = Color(0xFFCBD5E1))
                }
            }

            // Document 2
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(0.5.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onNavigateToModule("Cellina")
                        Toast.makeText(context, "Opening Inventory_Audit.ods...", Toast.LENGTH_SHORT).show()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFD1FAE5), shape = RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("S", fontWeight = FontWeight.Bold, color = Color(0xFF10B981), fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Inventory_Audit.ods",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Shared • Edited 1d ago",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = "Open", tint = Color(0xFFCBD5E1))
                }
            }
        }
    }
}

@Composable
private fun ModuleBentoCard(
    title: String,
    subtitle: String,
    brandColor: Color,
    bgColor: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.5.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable(onClick = onClick)
            .testTag("card_${title.lowercase()}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top aligned colored icon box
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(bgColor, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = brandColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Bottom aligned info text
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}
