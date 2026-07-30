package com.makerandreas.papirusoffice.data

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FontUiState {
    object Loading : FontUiState()
    data class Success(
        val fonts: List<FontInfo>,
        val selectedFont: String
    ) : FontUiState()
    data class Error(val message: String) : FontUiState()
}

/**
 * FontViewModel
 * Provides the extracted and scanned font list from FontProvider to the UI (FontStyleSubpage).
 */
class FontViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<FontUiState>(FontUiState.Loading)
    val uiState: StateFlow<FontUiState> = _uiState.asStateFlow()

    private val _fontsList = MutableStateFlow<List<FontInfo>>(emptyList())
    val fontsList: StateFlow<List<FontInfo>> = _fontsList.asStateFlow()

    private val _selectedFont = MutableStateFlow<String>("Liberation Serif Regular")
    val selectedFont: StateFlow<String> = _selectedFont.asStateFlow()

    /**
     * Loads available fonts asynchronously using FontProvider
     */
    fun loadFonts(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = FontUiState.Loading
            try {
                val scannedFonts = FontProvider.scanAvailableFonts(context)
                
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                val isConnected = capabilities != null && 
                    (capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) || 
                     capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR))

                val combinedFonts = if (isConnected) {
                    val googleFonts = listOf(
                        FontInfo("OpenSans.ttf", "Open Sans", "Open Sans", "Google Fonts (Cloud)", "TTF", true),
                        FontInfo("Lato.ttf", "Lato", "Lato", "Google Fonts (Cloud)", "TTF", true),
                        FontInfo("Montserrat.ttf", "Montserrat", "Montserrat", "Google Fonts (Cloud)", "TTF", true),
                        FontInfo("Poppins.ttf", "Poppins", "Poppins", "Google Fonts (Cloud)", "TTF", true),
                        FontInfo("Oswald.ttf", "Oswald", "Oswald", "Google Fonts (Cloud)", "TTF", true)
                    )
                    (scannedFonts + googleFonts).sortedBy { it.displayName }
                } else {
                    scannedFonts
                }
                
                _fontsList.value = combinedFonts
                _uiState.value = FontUiState.Success(
                    fonts = combinedFonts,
                    selectedFont = _selectedFont.value
                )
            } catch (e: Exception) {
                _uiState.value = FontUiState.Error("Error scanning fonts: ${e.message}")
            }
        }
    }

    /**
     * Updates selected font
     */
    fun selectFont(fontName: String) {
        _selectedFont.value = fontName
        val currentFonts = _fontsList.value
        if (currentFonts.isNotEmpty()) {
            _uiState.value = FontUiState.Success(
                fonts = currentFonts,
                selectedFont = fontName
            )
        }
    }
}
