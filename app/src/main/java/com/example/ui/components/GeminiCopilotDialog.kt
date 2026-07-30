package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.ai.GeminiAiService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiCopilotDialog(
    currentDocumentText: String,
    moduleType: String = "WRITER", // WRITER, CALC, IMPRESS, INKY
    onDismiss: () -> Unit,
    onInsertTextToDocument: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var userPrompt by remember { mutableStateOf("") }
    var aiResponse by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var useSearchGrounding by remember { mutableStateOf(false) }
    var searchQueries by remember { mutableStateOf<List<String>>(emptyList()) }
    var citations by remember { mutableStateOf<List<GeminiAiService.WebCitation>>(emptyList()) }
    var selectedModel by remember { mutableStateOf(GeminiAiService.getSelectedModel(context)) }

    val primaryGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF6750A4), Color(0xFF9C27B0), Color(0xFF006399))
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(28.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // --- Header ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(primaryGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "Gemini",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Gemini Intelligence Copilot",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Powered by Google AI • Papirus Engine",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- Mode & Model Options ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = useSearchGrounding,
                        onClick = { useSearchGrounding = !useSearchGrounding },
                        label = {
                            Text(if (useSearchGrounding) "Google Search Grounded" else "Standard AI")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (useSearchGrounding) Icons.Default.Search else Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )

                    // Model Selection dropdown / chip
                    AssistChip(
                        onClick = {
                            val nextModel = when (selectedModel) {
                                GeminiAiService.MODEL_FLASH -> GeminiAiService.MODEL_PRO
                                GeminiAiService.MODEL_PRO -> GeminiAiService.MODEL_LITE
                                else -> GeminiAiService.MODEL_FLASH
                            }
                            selectedModel = nextModel
                            GeminiAiService.saveSelectedModel(context, nextModel)
                        },
                        label = {
                            Text(
                                text = when (selectedModel) {
                                    GeminiAiService.MODEL_PRO -> "Gemini 3.1 Pro"
                                    GeminiAiService.MODEL_LITE -> "Gemini 3.1 Flash-Lite"
                                    else -> "Gemini 3.5 Flash"
                                },
                                fontSize = 12.sp
                            )
                        },
                        trailingIcon = {
                            Icon(Icons.Default.Tune, contentDescription = "Switch Model", modifier = Modifier.size(14.dp))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // --- Preset Productivity Quick Chips ---
                Text(
                    text = "Quick Productivity Tools",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    item {
                        SuggestionChip(
                            onClick = {
                                isGenerating = true
                                scope.launch {
                                    aiResponse = GeminiAiService.summarizeDocument(context, currentDocumentText, moduleType)
                                    isGenerating = false
                                }
                            },
                            label = { Text("📝 Summarize Doc") },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        )
                    }
                    item {
                        SuggestionChip(
                            onClick = {
                                isGenerating = true
                                scope.launch {
                                    aiResponse = GeminiAiService.proofreadAndPolish(context, currentDocumentText)
                                    isGenerating = false
                                }
                            },
                            label = { Text("✨ Grammar & Polish") }
                        )
                    }
                    if (moduleType == "CALC" || moduleType == "SPREADSHEET") {
                        item {
                            SuggestionChip(
                                onClick = {
                                    userPrompt = "Generate formula for calculating total revenue with 10% tax discount"
                                },
                                label = { Text("📊 Formula Helper") }
                            )
                        }
                    }
                    if (moduleType == "IMPRESS" || moduleType == "SLIDIA") {
                        item {
                            SuggestionChip(
                                onClick = {
                                    userPrompt = "Generate 5-slide outline for Business Strategy Proposal"
                                },
                                label = { Text("💡 Slide Deck Outline") }
                            )
                        }
                    }
                    item {
                        SuggestionChip(
                            onClick = {
                                useSearchGrounding = true
                                userPrompt = "Find modern document templates and stock reference photos for business report"
                            },
                            label = { Text("🔍 Search Templates & Images") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- AI Response / Result Output Area ---
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {
                        if (isGenerating) {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (useSearchGrounding) "Searching Google & Grounding Data..." else "Thinking with Gemini...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else if (aiResponse.isEmpty()) {
                            Text(
                                text = "Ask Gemini anything or tap a quick productivity chip above to analyze, edit, or generate content for your document.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item {
                                    Text(
                                        text = aiResponse,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (searchQueries.isNotEmpty()) {
                                    item {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Search Queries Used:",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        searchQueries.forEach { q ->
                                            Text("• $q", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }

                                if (citations.isNotEmpty()) {
                                    item {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Grounded Sources & Web References:",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    items(citations) { citation ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    try {
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(citation.url))
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                    }
                                                }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Link,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = citation.title,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Actions for Response (Insert / Copy)
                if (aiResponse.isNotEmpty() && !isGenerating) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { aiResponse = "" }
                        ) {
                            Text("Clear")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onInsertTextToDocument(aiResponse)
                                onDismiss()
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Insert into Document")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // --- Prompt Input Row ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = userPrompt,
                        onValueChange = { userPrompt = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask Gemini to generate or edit...") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (userPrompt.isNotBlank()) {
                                isGenerating = true
                                val currentPrompt = userPrompt
                                userPrompt = ""
                                scope.launch {
                                    if (useSearchGrounding) {
                                        val result = GeminiAiService.generateWithSearchGrounding(context, currentPrompt, selectedModel)
                                        aiResponse = result.textResponse
                                        searchQueries = result.searchQueries
                                        citations = result.citations
                                    } else {
                                        searchQueries = emptyList()
                                        citations = emptyList()
                                        aiResponse = GeminiAiService.generateContent(context, currentPrompt, targetModel = selectedModel)
                                    }
                                    isGenerating = false
                                }
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(primaryGradient)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
