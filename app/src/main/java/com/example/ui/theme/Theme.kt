package com.example.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

object ThemeSettings {
    private const val PREFS_NAME = "papirus_office_theme_prefs"
    private const val KEY_DYNAMIC_COLOR = "dynamic_color_enabled"

    fun isDynamicColorEnabled(context: Context): Boolean {
        val defaultVal = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DYNAMIC_COLOR, defaultVal)
    }

    fun setDynamicColorEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DYNAMIC_COLOR, enabled)
            .apply()
    }
}

// ==========================================
// Base Color Schemes (BrandBase: #A4C639)
// ==========================================
private val BaseLightColorScheme = lightColorScheme(
    primary = BrandBase,
    onPrimary = Color(0xFF1F2937), // high contrast dark slate on light green
    primaryContainer = Color(0xFFE2F0B3),
    onPrimaryContainer = Color(0xFF1D2600),
    secondary = Color(0xFF5D624E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E7CD),
    onSecondaryContainer = Color(0xFF1A1E0E),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

private val BaseDarkColorScheme = darkColorScheme(
    primary = BrandBase,
    onPrimary = Color(0xFF263500),
    primaryContainer = Color(0xFF3B5000),
    onPrimaryContainer = Color(0xFFBDDF51),
    secondary = Color(0xFFC6CBB2),
    onSecondary = Color(0xFF2E3322),
    secondaryContainer = Color(0xFF444A37),
    onSecondaryContainer = Color(0xFFE2E7CD),
    background = Color(0xFF020617),
    surface = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155)
)

// ==========================================
// Inky Color Schemes (BrandInky: #4285F4)
// ==========================================
private val InkyLightColorScheme = lightColorScheme(
    primary = BrandInky,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6FF),
    onPrimaryContainer = Color(0xFF001549),
    secondary = Color(0xFF585F72),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE2F9),
    onSecondaryContainer = Color(0xFF151B2C),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

private val InkyDarkColorScheme = darkColorScheme(
    primary = BrandInky,
    onPrimary = Color(0xFF00216C),
    primaryContainer = Color(0xFF003CA4),
    onPrimaryContainer = Color(0xFFDCE6FF),
    secondary = Color(0xFFC0C6DD),
    onSecondary = Color(0xFF272F42),
    secondaryContainer = Color(0xFF3E465A),
    onSecondaryContainer = Color(0xFFDCE2F9),
    background = Color(0xFF020617),
    surface = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155)
)

// ==========================================
// Cellina Color Schemes (BrandCellina: #34A853)
// ==========================================
private val CellinaLightColorScheme = lightColorScheme(
    primary = BrandCellina,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC1F1C8),
    onPrimaryContainer = Color(0xFF002209),
    secondary = Color(0xFF516351),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD4E9D2),
    onSecondaryContainer = Color(0xFF0F1F11),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

private val CellinaDarkColorScheme = darkColorScheme(
    primary = BrandCellina,
    onPrimary = Color(0xFF003914),
    primaryContainer = Color(0xFF005322),
    onPrimaryContainer = Color(0xFFC1F1C8),
    secondary = Color(0xFFB8CCB7),
    onSecondary = Color(0xFF243425),
    secondaryContainer = Color(0xFF3A4B3A),
    onSecondaryContainer = Color(0xFFD4E9D2),
    background = Color(0xFF020617),
    surface = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155)
)

// ==========================================
// Slidia Color Schemes (BrandSlidia: #EA4335)
// ==========================================
private val SlidiaLightColorScheme = lightColorScheme(
    primary = BrandSlidia,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD5),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF775652),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD5),
    onSecondaryContainer = Color(0xFF2C1512),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

private val SlidiaDarkColorScheme = darkColorScheme(
    primary = BrandSlidia,
    onPrimary = Color(0xFF680003),
    primaryContainer = Color(0xFF93000A),
    onPrimaryContainer = Color(0xFFFFDAD5),
    secondary = Color(0xFFE7BDB7),
    onSecondary = Color(0xFF442926),
    secondaryContainer = Color(0xFF5D3F3C),
    onSecondaryContainer = Color(0xFFFFDAD5),
    background = Color(0xFF020617),
    surface = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155)
)

// ==========================================
// Pagella Color Schemes (BrandPagella: #FBBC05)
// ==========================================
private val PagellaLightColorScheme = lightColorScheme(
    primary = BrandPagella,
    onPrimary = Color(0xFF211B00), // high contrast dark slate on light yellow
    primaryContainer = Color(0xFFFFF1BD),
    onPrimaryContainer = Color(0xFF241A00),
    secondary = Color(0xFF6A5D3F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E1BB),
    onSecondaryContainer = Color(0xFF231B04),
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

private val PagellaDarkColorScheme = darkColorScheme(
    primary = BrandPagella,
    onPrimary = Color(0xFF423200),
    primaryContainer = Color(0xFF5E4900),
    onPrimaryContainer = Color(0xFFFFF1BD),
    secondary = Color(0xFFD6C5A0),
    onSecondary = Color(0xFF3B2F15),
    secondaryContainer = Color(0xFF524529),
    onSecondaryContainer = Color(0xFFF3E1BB),
    background = Color(0xFF020617),
    surface = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155)
)

@Composable
fun PapirusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    workspace: String = "home",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> {
            when (workspace.lowercase()) {
                "inky" -> InkyDarkColorScheme
                "cellina" -> CellinaDarkColorScheme
                "slidia" -> SlidiaDarkColorScheme
                "pagella" -> PagellaDarkColorScheme
                else -> BaseDarkColorScheme
            }
        }
        else -> {
            when (workspace.lowercase()) {
                "inky" -> InkyLightColorScheme
                "cellina" -> CellinaLightColorScheme
                "slidia" -> SlidiaLightColorScheme
                "pagella" -> PagellaLightColorScheme
                else -> BaseLightColorScheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
