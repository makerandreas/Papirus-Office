package com.example.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.aboutTitle),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_about_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back to Start Screen"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Illustration & Brand Header
            item {
                Spacer(modifier = Modifier.height(16.dp))
                AboutIllustrationSection()
            }

            // App Name & Version Info Card
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = buildAnnotatedString {
                            append("Papirus ")
                            withStyle(style = SpanStyle(color = BrandBase, fontWeight = FontWeight.ExtraBold)) {
                                append("Office")
                            }
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = stringResource(R.string.aboutAppDescription),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.testTag("ai_technology_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Built with AI technology",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Version metadata details
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        VersionRow(
                            label = stringResource(R.string.aboutAppVersion),
                            value = BuildConfig.APP_VERSION_NAME,
                            icon = Icons.Rounded.PhoneAndroid
                        )
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        VersionRow(
                            label = stringResource(R.string.aboutEngineVersion),
                            value = BuildConfig.PAPIRUS_ENGINE_VERSION,
                            icon = Icons.Rounded.SettingsSuggest
                        )
                    }
                }
            }

            // Components List Card
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.aboutComponentsTitle),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.aboutComponentsDesc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ComponentItemCard(
                            title = "Inky Module (Writer)",
                            description = stringResource(R.string.aboutComponentInky),
                            icon = Icons.Rounded.Description,
                            tint = BrandInky
                        )
                        ComponentItemCard(
                            title = "Cellina Module (Spreadsheets)",
                            description = stringResource(R.string.aboutComponentCellina),
                            icon = Icons.Rounded.GridView,
                            tint = BrandCellina
                        )
                        ComponentItemCard(
                            title = "Slidia Module (Presentations)",
                            description = stringResource(R.string.aboutComponentSlidia),
                            icon = Icons.Rounded.Slideshow,
                            tint = BrandSlidia
                        )
                        ComponentItemCard(
                            title = "Pagella Module (PDF)",
                            description = stringResource(R.string.aboutComponentPagella),
                            icon = Icons.Rounded.PictureAsPdf,
                            tint = BrandPagella
                        )
                        ComponentItemCard(
                            title = "HarfBuzz Engine",
                            description = stringResource(R.string.aboutComponentHarfbuzz),
                            icon = Icons.Rounded.TextFields,
                            tint = Color(0xFF8B5CF6) // Purple theme
                        )
                        ComponentItemCard(
                            title = "TinyXML Parser",
                            description = stringResource(R.string.aboutComponentTinyxml),
                            icon = Icons.Rounded.Code,
                            tint = Color(0xFF6B7280) // Gray theme
                        )
                    }
                }
            }

            // License & Credits Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Gavel,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.aboutLicensesTitle),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = stringResource(R.string.aboutLicensesDesc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.aboutDeveloperNote),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun AboutIllustrationSection() {
    val infiniteTransition = rememberInfiniteTransition(label = "IllustrationRotation")
    
    // Smooth infinite rotation for Orbit & float for Banana
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbitRotation"
    )

    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BananaBounce"
    )

    Box(
        modifier = Modifier
            .size(240.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Module orbit background path
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), CircleShape)
        )

        // The four module icons orbiting around
        Box(
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotationAngle)
        ) {
            // Inky - Top
            OrbitingIcon(
                icon = Icons.Rounded.Description,
                color = BrandInky,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-18).dp)
                    .rotate(-rotationAngle)
            )

            // Cellina - Right
            OrbitingIcon(
                icon = Icons.Rounded.GridView,
                color = BrandCellina,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 18.dp)
                    .rotate(-rotationAngle)
            )

            // Slidia - Bottom
            OrbitingIcon(
                icon = Icons.Rounded.Slideshow,
                color = BrandSlidia,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 18.dp)
                    .rotate(-rotationAngle)
            )

            // Pagella - Left
            OrbitingIcon(
                icon = Icons.Rounded.PictureAsPdf,
                color = BrandPagella,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-18).dp)
                    .rotate(-rotationAngle)
            )
        }

        // Centered elegant illustration representing office software on Android mobile device
        Box(
            modifier = Modifier
                .size(100.dp)
                .offset(y = bounceOffset.dp),
            contentAlignment = Alignment.Center
        ) {
            // Glow background effect
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // 1. Android Smartphone Frame
            Box(
                modifier = Modifier
                    .size(width = 54.dp, height = 86.dp)
                    .border(2.5.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                // Smartphone Screen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    MaterialTheme.colorScheme.surface
                                )
                            ),
                            shape = RoundedCornerShape(9.dp)
                        )
                ) {
                    // Speaker ear piece at top
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 2.dp)
                            .size(width = 12.dp, height = 2.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), RoundedCornerShape(1.dp))
                    )
                }
            }

            // 2. Overlapping Elegant Office Document / Sheet
            Box(
                modifier = Modifier
                    .size(width = 44.dp, height = 54.dp)
                    .offset(x = 12.dp, y = 8.dp)
                    .rotate(8f)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                    .border(1.5.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                // Inside the document: draw document lines/grid representing sheets/texts
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Title placeholder line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(3.5.dp)
                            .background(MaterialTheme.colorScheme.onPrimary, RoundedCornerShape(1.dp))
                    )
                    // Detail lines
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f), RoundedCornerShape(1.dp))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f), RoundedCornerShape(1.dp))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f), RoundedCornerShape(1.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun OrbitingIcon(
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .background(color.copy(alpha = 0.15f), CircleShape)
            .border(1.5.dp, color.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun VersionRow(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ComponentItemCard(
    title: String,
    description: String,
    icon: ImageVector,
    tint: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(tint.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
