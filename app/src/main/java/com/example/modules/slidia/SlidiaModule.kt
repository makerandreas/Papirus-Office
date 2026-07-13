package com.example.modules.slidia

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.OfficeSidebar

@Composable
fun SlidiaModule(
    isTablet: Boolean,
    onTransitionSelected: (String) -> Unit
) {
    // List of presentation slides
    val slides = remember {
        listOf(
            SlideItem(1, "Papirus Suite Overview", "A modular PC-level office suite for mobile and tablet."),
            SlideItem(2, "Inky Core & ODF", "Advanced WordprocessingML and OpenDocument text compliance layers."),
            SlideItem(3, "Cellina Calculations", "Cell references, formula parsers, and multi-sheet grids."),
            SlideItem(4, "Slidia Slides", "M3 design transitions and interactive slide shows."),
            SlideItem(5, "Pagella PDF Reader", "PDF viewing, styling, annotations and fallbacks.")
        )
    }

    var activeSlideIndex by remember { mutableStateOf(0) }
    var isSlideSorterMode by remember { mutableStateOf(false) }
    var isSlideShowMode by remember { mutableStateOf(false) }
    var showTransitionsSidebar by remember { mutableStateOf(false) }
    var activeTransition by remember { mutableStateOf("Fade") }

    val activeSlide = slides[activeSlideIndex]

    if (isSlideShowMode) {
        // Fullscreen Slide Show Simulation Layout
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("slideshow_canvas")
        ) {
            // Slide Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = activeSlide.title,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = activeSlide.subtitle,
                    color = Color.LightGray,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Controls overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { if (activeSlideIndex > 0) activeSlideIndex-- },
                    enabled = activeSlideIndex > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Previous Slide", tint = Color.White)
                }

                Text(
                    text = "Slide ${activeSlideIndex + 1} of ${slides.size} (${activeTransition})",
                    color = Color.Gray,
                    fontSize = 14.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { if (activeSlideIndex < slides.size - 1) activeSlideIndex++ },
                        enabled = activeSlideIndex < slides.size - 1,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Next Slide", tint = Color.White)
                    }

                    Button(
                        onClick = { isSlideShowMode = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.FullscreenExit, contentDescription = "Exit Show", tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Exit", color = Color.White)
                    }
                }
            }
        }
    } else {
        // Standard Editing workspace
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                // Toolbar Control Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { isSlideSorterMode = !isSlideSorterMode },
                            colors = if (isSlideSorterMode) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors(),
                            modifier = Modifier.testTag("btn_slide_sorter")
                        ) {
                            Icon(Icons.Default.GridView, contentDescription = "Slide Sorter")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Slide Sorter")
                        }

                        Button(
                            onClick = { isSlideShowMode = true },
                            modifier = Modifier.testTag("btn_slideshow")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Slide Show")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Slide Show")
                        }
                    }

                    IconButton(onClick = { showTransitionsSidebar = true }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Slide Transitions", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isSlideSorterMode) {
                    // Slide Sorter Grid View (Compose Grid cells)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(if (isTablet) 3 else 2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(slides) { slide ->
                            val isSelected = slide.id == activeSlide.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .clickable {
                                        activeSlideIndex = slide.id - 1
                                        isSlideSorterMode = false
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${slide.id}. ${slide.title}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = slide.subtitle,
                                        fontSize = 10.sp,
                                        color = Color.DarkGray,
                                        maxLines = 2,
                                        lineHeight = 12.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Standard visual Active Slide editor Canvas
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .border(1.dp, Color.LightGray, MaterialTheme.shapes.large)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = activeSlide.title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = activeSlide.subtitle,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Slide indicator bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (activeSlideIndex > 0) activeSlideIndex-- },
                            enabled = activeSlideIndex > 0
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Prev")
                        }
                        Text(
                            text = "${activeSlideIndex + 1} / ${slides.size}",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        IconButton(
                            onClick = { if (activeSlideIndex < slides.size - 1) activeSlideIndex++ },
                            enabled = activeSlideIndex < slides.size - 1
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next")
                        }
                    }
                }
            }

            // Transitions settings Sidebar
            if (showTransitionsSidebar) {
                OfficeSidebar(
                    title = "Slide Transitions",
                    onClose = { showTransitionsSidebar = false },
                    onApply = {
                        showTransitionsSidebar = false
                        onTransitionSelected("Applied transition: $activeTransition")
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Select transition effect for Slide Show rendering:", fontSize = 12.sp, color = Color.Gray)
                        
                        val transitionTypes = listOf("Fade", "Slide-in", "Cube Roll", "3D Spin", "Zoom")
                        transitionTypes.forEach { type ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activeTransition = type }
                                    .background(
                                        if (activeTransition == type) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = activeTransition == type,
                                    onClick = { activeTransition = type }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(type)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class SlideItem(
    val id: Int,
    val title: String,
    val subtitle: String
)
