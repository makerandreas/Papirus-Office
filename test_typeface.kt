import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Typeface
import android.graphics.Typeface as AndroidTypeface

fun test(androidTypeface: AndroidTypeface): FontFamily {
    return FontFamily(Typeface(androidTypeface))
}
