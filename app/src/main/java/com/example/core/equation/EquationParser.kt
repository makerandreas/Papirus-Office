package com.example.core.equation

import android.util.Log

/**
 * Modular Equation Parser and Converter for Papirus Office.
 * Facilitates bidirectional translation between LaTeX (In-App Editing),
 * MathML (ODF standard used by Inky/LibreOffice), and OMML (OOXML standard used by MS Office).
 */
object EquationParser {
    private const val TAG = "EquationParser"

    /**
     * Converts LaTeX syntax to MathML format (used in ODF files like .odt, .ods, .odp).
     */
    fun latexToMathML(latex: String): String {
        Log.d(TAG, "Converting LaTeX to MathML: $latex")
        var clean = latex.trim()
        
        // Simple token substitutions for demonstrative layout parsing
        val builder = StringBuilder()
        builder.append("<math xmlns=\"http://www.w3.org/1998/Math/MathML\">")
        
        try {
            // Parse fractions: \frac{num}{den} -> <mfrac><num>...</num><den>...</den></mfrac>
            if (clean.contains("\\frac")) {
                clean = parseFractionsToMathML(clean)
            }
            // Parse square root: \sqrt{arg} -> <msqrt><arg>...</arg></msqrt>
            if (clean.contains("\\sqrt")) {
                clean = parseRootsToMathML(clean)
            }
            
            // Standard symbol mapping (e.g. \alpha, +, -, subscripts, superscripts)
            val parsedContent = parseBasicSymbolsToMathML(clean)
            builder.append(parsedContent)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing LaTeX to MathML, running fallback wrapper", e)
            builder.append("<mtext>").append(clean).append("</mtext>")
        }
        
        builder.append("</math>")
        return builder.toString()
    }

    /**
     * Converts LaTeX syntax to Office Math Markup Language (OMML) format (used in DOCX, XLSX, PPTX).
     */
    fun latexToOMML(latex: String): String {
        Log.d(TAG, "Converting LaTeX to OMML: $latex")
        var clean = latex.trim()
        
        val builder = StringBuilder()
        builder.append("<m:oMath xmlns:m=\"http://schemas.openxmlformats.org/officeDocument/2006/math\">")
        
        try {
            if (clean.contains("\\frac")) {
                clean = parseFractionsToOMML(clean)
            }
            if (clean.contains("\\sqrt")) {
                clean = parseRootsToOMML(clean)
            }
            
            val parsedContent = parseBasicSymbolsToOMML(clean)
            builder.append(parsedContent)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing LaTeX to OMML, returning raw text tag", e)
            builder.append("<m:r><m:t>").append(clean).append("</m:t></m:r>")
        }
        
        builder.append("</m:oMath>")
        return builder.toString()
    }

    /**
     * Converts MathML markup back into LaTeX syntax for the visual equation editor.
     */
    fun mathMLToLatex(mathML: String): String {
        Log.d(TAG, "Converting MathML to LaTeX")
        // Basic parser parsing standard MathML tags back into LaTeX
        var latex = mathML
            .replace("<math[^>]*>".toRegex(), "")
            .replace("</math>", "")
            .replace("<mfrac>", "\\frac{")
            .replace("</mfrac>", "}")
            .replace("<msqrt>", "\\sqrt{")
            .replace("</msqrt>", "}")
            .replace("<mrow>", "")
            .replace("</mrow>", "")
            .replace("<mi>([a-zA-Z]+)</mi>".toRegex(), "$1")
            .replace("<mo>([+\\-*/=])</mo>".toRegex(), "$1")
            .replace("<mn>([0-9.]+)</mn>".toRegex(), "$1")
        
        // Remove nested/extra tags if any
        latex = latex.replace("<[^>]+>".toRegex(), "").trim()
        return latex
    }

    /**
     * Converts OMML markup back into LaTeX syntax.
     */
    fun ommlToLatex(omml: String): String {
        Log.d(TAG, "Converting OMML to LaTeX")
        // Standard mapping for Word's OMML equations back to readable LaTeX
        var latex = omml
            .replace("<m:oMath[^>]*>".toRegex(), "")
            .replace("</m:oMath>", "")
            .replace("<m:f>".toRegex(), "\\frac{")
            .replace("</m:f>".toRegex(), "}")
            .replace("<m:num[^>]*>".toRegex(), "")
            .replace("</m:num>".toRegex(), "}{")
            .replace("<m:den[^>]*>".toRegex(), "")
            .replace("</m:den>".toRegex(), "")
            .replace("<m:rad[^>]*>".toRegex(), "\\sqrt{")
            .replace("</m:rad>".toRegex(), "}")
            .replace("<m:t[^>]*>([^<]+)</m:t>".toRegex(), "$1")
            .replace("<[^>]+>".toRegex(), "").trim()
        
        return latex
    }

    // --- Helper Methods ---

    private fun parseFractionsToMathML(latex: String): String {
        // Simple regex-based replacement for matching \frac{num}{den}
        val regex = "\\\\frac\\{([^\\}]+)\\}\\{([^\\}]+)\\}".toRegex()
        return latex.replace(regex) { matchResult ->
            val num = matchResult.groupValues[1]
            val den = matchResult.groupValues[2]
            "<mfrac><mrow>${parseBasicSymbolsToMathML(num)}</mrow><mrow>${parseBasicSymbolsToMathML(den)}</mrow></mfrac>"
        }
    }

    private fun parseRootsToMathML(latex: String): String {
        val regex = "\\\\sqrt\\{([^\\}]+)\\}".toRegex()
        return latex.replace(regex) { matchResult ->
            val arg = matchResult.groupValues[1]
            "<msqrt><mrow>${parseBasicSymbolsToMathML(arg)}</mrow></msqrt>"
        }
    }

    private fun parseBasicSymbolsToMathML(latex: String): String {
        val builder = StringBuilder()
        var i = 0
        while (i < latex.length) {
            val c = latex[i]
            when {
                c.isLetter() -> {
                    // Check for Greek letters or custom LaTeX symbols
                    if (c == '\\') {
                        val word = StringBuilder()
                        i++
                        while (i < latex.length && latex[i].isLetter()) {
                            word.append(latex[i])
                            i++
                        }
                        val symbol = word.toString()
                        builder.append("<mi>&").append(symbol).append(";</mi>")
                        continue
                    } else {
                        builder.append("<mi>").append(c).append("</mi>")
                    }
                }
                c.isDigit() -> builder.append("<mn>").append(c).append("</mn>")
                c in "+-*/=" -> builder.append("<mo>").append(c).append("</mo>")
                c == '^' -> {
                    // Superscript handling simulation
                    builder.append("<mo><sup></mo>")
                }
                c == '_' -> {
                    // Subscript handling simulation
                    builder.append("<mo><sub></mo>")
                }
                c == ' ' -> { /* skip */ }
                else -> builder.append("<mo>").append(c).append("</mo>")
            }
            i++
        }
        return builder.toString()
    }

    private fun parseFractionsToOMML(latex: String): String {
        val regex = "\\\\frac\\{([^\\}]+)\\}\\{([^\\}]+)\\}".toRegex()
        return latex.replace(regex) { matchResult ->
            val num = matchResult.groupValues[1]
            val den = matchResult.groupValues[2]
            "<m:f><m:num><m:r><m:t>${parseBasicSymbolsToOMML(num)}</m:t></m:r></m:num><m:den><m:r><m:t>${parseBasicSymbolsToOMML(den)}</m:t></m:r></m:den></m:f>"
        }
    }

    private fun parseRootsToOMML(latex: String): String {
        val regex = "\\\\sqrt\\{([^\\}]+)\\}".toRegex()
        return latex.replace(regex) { matchResult ->
            val arg = matchResult.groupValues[1]
            "<m:rad><m:radPr><m:degHide m:val=\"on\"/></m:radPr><m:deg/><m:e><m:r><m:t>${parseBasicSymbolsToOMML(arg)}</m:t></m:r></m:e></m:rad>"
        }
    }

    private fun parseBasicSymbolsToOMML(latex: String): String {
        val builder = StringBuilder()
        var i = 0
        while (i < latex.length) {
            val c = latex[i]
            when {
                c == '\\' -> {
                    val word = StringBuilder()
                    i++
                    while (i < latex.length && latex[i].isLetter()) {
                        word.append(latex[i])
                        i++
                    }
                    val symbol = word.toString()
                    builder.append("<m:r><m:t>\\").append(symbol).append("</m:t></m:r>")
                    continue
                }
                c.isLetterOrDigit() || c in "+-*/=" -> {
                    builder.append("<m:r><m:t>").append(c).append("</m:t></m:r>")
                }
            }
            i++
        }
        return builder.toString()
    }
}
