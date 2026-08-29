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
    private const val KEY_THEME_MODE = "theme_mode_preference" // SYSTEM, LIGHT, DARK

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

    fun getThemeMode(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
    }

    fun setThemeMode(context: Context, mode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, mode)
            .apply()
    }

    fun resolveDarkTheme(context: Context, systemInDarkTheme: Boolean): Boolean {
        return when (getThemeMode(context)) {
            "DARK" -> true
            "LIGHT" -> false
            else -> systemInDarkTheme
        }
    }
}

// ==========================================
// Base Color Schemes (Papirus Base: #2563EB)
// ==========================================
private val BaseLightColorScheme = lightColorScheme(
    primary = BrandBase,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF001B3F),
    secondary = Color(0xFF565E71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDAE2F9),
    onSecondaryContainer = Color(0xFF131C2B),
    tertiary = Color(0xFF705574),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFAD7FD),
    onTertiaryContainer = Color(0xFF28132E),
    background = Color(0xFFFEF7FF),
    surface = Color(0xFFFEF7FF),
    surfaceContainer = Color(0xFFF1F5F9),
    onBackground = Color(0xFF191C20),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFE0E2EC),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0)
)

private val BaseDarkColorScheme = darkColorScheme(
    primary = Color(0xFFADC6FF),
    onPrimary = Color(0xFF002E69),
    primaryContainer = Color(0xFF004494),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFFBEC6DC),
    onSecondary = Color(0xFF283041),
    secondaryContainer = Color(0xFF3E4759),
    onSecondaryContainer = Color(0xFFDAE2F9),
    tertiary = Color(0xFFDDBCE0),
    onTertiary = Color(0xFF3F2844),
    tertiaryContainer = Color(0xFF573E5C),
    onTertiaryContainer = Color(0xFFFAD7FD),
    background = Color(0xFF111318),
    surface = Color(0xFF111318),
    surfaceContainer = Color(0xFF1D2024),
    onBackground = Color(0xFFE2E2E9),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF44474E),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44474E)
)

// ==========================================
// Inky Color Schemes (BrandInky: #0F9D58)
// ==========================================
private val InkyLightColorScheme = lightColorScheme(
    primary = BrandInky,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCEF4DC),
    onPrimaryContainer = Color(0xFF00210E),
    secondary = Color(0xFF506352),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E8D3),
    onSecondaryContainer = Color(0xFF0E1F12),
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
    onPrimary = Color(0xFF003918),
    primaryContainer = Color(0xFF005226),
    onPrimaryContainer = Color(0xFFCEF4DC),
    secondary = Color(0xFFB7CCB8),
    onSecondary = Color(0xFF233426),
    secondaryContainer = Color(0xFF394B3B),
    onSecondaryContainer = Color(0xFFD3E8D3),
    background = Color(0xFF020617),
    surface = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155)
)

// ==========================================
// Cellina Color Schemes (BrandCellina: #16A3B7)
// ==========================================
private val CellinaLightColorScheme = lightColorScheme(
    primary = BrandCellina,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC6F2F8),
    onPrimaryContainer = Color(0xFF002025),
    secondary = Color(0xFF4A6267),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCE8ED),
    onSecondaryContainer = Color(0xFF051F23),
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
    onPrimary = Color(0xFF00363E),
    primaryContainer = Color(0xFF004F5B),
    onPrimaryContainer = Color(0xFFC6F2F8),
    secondary = Color(0xFFB0CCD1),
    onSecondary = Color(0xFF1C3438),
    secondaryContainer = Color(0xFF324B4F),
    onSecondaryContainer = Color(0xFFCCE8ED),
    background = Color(0xFF020617),
    surface = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155)
)

// ==========================================
// Slidia Color Schemes (BrandSlidia: #F59E0B)
// ==========================================
private val SlidiaLightColorScheme = lightColorScheme(
    primary = BrandSlidia,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0A8),
    onPrimaryContainer = Color(0xFF2B1700),
    secondary = Color(0xFF6F5B40),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFBE0BD),
    onSecondaryContainer = Color(0xFF271904),
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
    onPrimary = Color(0xFF452800),
    primaryContainer = Color(0xFF673E00),
    onPrimaryContainer = Color(0xFFFFE0A8),
    secondary = Color(0xFFDEC2A2),
    onSecondary = Color(0xFF3E2D16),
    secondaryContainer = Color(0xFF56432A),
    onSecondaryContainer = Color(0xFFFBE0BD),
    background = Color(0xFF020617),
    surface = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155)
)

// ==========================================
// Pagella Color Schemes (BrandPagella: #D93025)
// ==========================================
private val PagellaLightColorScheme = lightColorScheme(
    primary = BrandPagella,
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

private val PagellaDarkColorScheme = darkColorScheme(
    primary = BrandPagella,
    onPrimary = Color(0xFF680005),
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
