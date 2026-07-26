package com.makerandreas.papirusoffice.data.framework

// ---------------------------------------------------------
// Core Linguistic Service Manager
// ---------------------------------------------------------

interface XLinguServiceManager {
    fun getSpellChecker(): XSpellChecker
    fun getHyphenator(): XHyphenator
    fun getThesaurus(): XThesaurus
    fun addLinguServiceManagerListener(listener: XLinguServiceEventListener): Boolean
    fun removeLinguServiceManagerListener(listener: XLinguServiceEventListener): Boolean
}

interface XLinguServiceManager2 : XLinguServiceManager {
    fun getAvailableServices(service: String, locale: Locale): Array<String>
    fun getConfiguredServices(service: String, locale: Locale): Array<String>
    fun getAvailableLocales(service: String): Array<Locale>
    fun setConfiguredServices(service: String, locale: Locale, serviceNames: Array<String>)
}

interface XLinguServiceEventListener : XEventListener {
    fun processLinguServiceEvent(event: LinguServiceEvent)
}

open class LinguServiceEvent(
    source: Any,
    val EventId: Int
) : EventObject(source)

// ---------------------------------------------------------
// Properties and Locales
// ---------------------------------------------------------


interface XLinguProperties : XPropertySet {
    // Defines many properties like IsSpellCapitalization, DefaultLocale, etc.
}

interface XSupportedLocales {
    val locales: Array<Locale>
    fun hasLocale(locale: Locale): Boolean
}

// ---------------------------------------------------------
// Spell Checker
// ---------------------------------------------------------

interface XSpellChecker : XSupportedLocales {
    fun isValid(word: String, locale: Locale, properties: Array<PropertyValue>): Boolean
    fun spell(word: String, locale: Locale, properties: Array<PropertyValue>): XSpellAlternatives?
}

interface XSpellAlternatives {
    val word: String
    val locale: Locale
    val failureType: Short
    val alternativesCount: Short
    val alternatives: Array<String>
}

// ---------------------------------------------------------
// Thesaurus
// ---------------------------------------------------------

interface XThesaurus : XSupportedLocales {
    fun queryMeanings(term: String, locale: Locale, properties: Array<PropertyValue>): Array<XMeaning>?
}

interface XMeaning {
    val meaning: String
    val synonyms: Array<String>
}

// ---------------------------------------------------------
// Hyphenator
// ---------------------------------------------------------

interface XHyphenator : XSupportedLocales {
    fun hyphenate(word: String, locale: Locale, maxLeading: Short, properties: Array<PropertyValue>): XHyphenatedWord?
    fun queryAlternativeSpelling(word: String, locale: Locale, index: Short, properties: Array<PropertyValue>): XHyphenatedWord?
    fun createPossibleHyphens(word: String, locale: Locale, properties: Array<PropertyValue>): XPossibleHyphens?
}

interface XHyphenatedWord {
    val word: String
    val locale: Locale
    val hyphenationPos: Short
    val hyphenatedWord: String
    val hyphenPos: Short
    val isAlternativeSpelling: Boolean
}

interface XPossibleHyphens {
    val word: String
    val locale: Locale
    val possibleHyphens: String
    val possibleHyphensCount: Short
}

// ---------------------------------------------------------
// Proofreader (Grammar Checker)
// ---------------------------------------------------------

interface XProofreader : XSupportedLocales {
    fun isSpellChecker(): Boolean
    fun doProofreading(
        documentIdentifier: String,
        text: String,
        locale: Locale,
        startOfSentencePosition: Int,
        behindEndOfSentencePosition: Int,
        properties: Array<PropertyValue>
    ): ProofreadingResult
    fun ignoreRule(ruleId: String, locale: Locale)
    fun resetIgnoreRules()
}

data class ProofreadingResult(
    val aDocumentIdentifier: String,
    val aText: String,
    val aLocale: Locale,
    val nStartOfSentencePosition: Int,
    val nBehindEndOfSentencePosition: Int,
    val aErrors: Array<SingleProofreadingError>,
    val aProperties: Array<PropertyValue>
)

data class SingleProofreadingError(
    val nErrorStart: Int,
    val nErrorLength: Int,
    val nErrorType: Int,
    val aRuleIdentifier: String,
    val aShortComment: String,
    val aFullComment: String,
    val aSuggestions: Array<String>,
    val aProperties: Array<PropertyValue>
)

// ---------------------------------------------------------
// Language Guessing
// ---------------------------------------------------------

interface XLanguageGuessing {
    fun guessPrimaryLanguage(text: String, startOffset: Int, len: Int): Locale
    fun guessAlternatives(text: String, startOffset: Int, len: Int): Array<Locale>
}

// ---------------------------------------------------------
// Dictionaries
// ---------------------------------------------------------

interface XDictionary : XNamed {
    val dictionaryType: DictionaryType
    var locale: Locale
    var isActive: Boolean
    val count: Int
    fun getEntries(): Array<XDictionaryEntry>
    fun getEntry(word: String): XDictionaryEntry?
    fun addEntry(entry: XDictionaryEntry): Boolean
    fun add(word: String, isNegative: Boolean, replacementString: String): Boolean
    fun remove(word: String): Boolean
    fun clear()
}

interface XDictionaryEntry {
    val dictionaryWord: String
    val isNegative: Boolean
    val replacementText: String
}

interface XDictionaryList {
    val count: Int
    val dictionaries: Array<XDictionary>
    fun getDictionaryByName(dictionaryName: String): XDictionary?
    fun addDictionary(dictionary: XDictionary): Boolean
    fun removeDictionary(dictionary: XDictionary): Boolean
}

interface XSearchableDictionaryList : XDictionaryList {
    fun queryDictionaryEntry(word: String, locale: Locale, searchPosDics: Boolean, searchSpellEntry: Boolean): XDictionaryEntry?
}

interface XConversionDictionary : XNamed {
    val conversionType: Short
    val locale: Locale
    var isActive: Boolean
    fun getConversions(text: String, startOffset: Int, length: Int, direction: Short, textConversionOptions: Int): Array<String>
}

interface XConversionDictionaryList {
    val dictionaryContainer: XNameContainer
    fun addNewDictionary(name: String, locale: Locale, conversionType: Short): XConversionDictionary
}

enum class DictionaryType {
    POSITIVE,
    NEGATIVE,
    MIXED
}

// ---------------------------------------------------------
// Extension Management
// ---------------------------------------------------------
interface XPackageInformationProvider {
    fun getExtensionList(): Array<Array<String>>
    fun getPackageLocation(extensionId: String): String
}
