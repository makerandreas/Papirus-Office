package com.makerandreas.papirusoffice.data.navigation

/**
 * What the Navigator may say when a category has nothing to show.
 *
 * The distinction is not cosmetic. "No bookmarks in this document" is a
 * statement about the document, so it may only be shown for a class some
 * parser actually produces. For every other class the app cannot see the
 * object at all, and the only honest answer is "not yet available in this
 * build" (antislop R-26/R-27; plan-03 3.32).
 *
 * Data always wins: a category whose index list is non-empty renders that
 * list whatever its availability says. Availability only picks the wording of
 * the empty state.
 */
enum class NavigatorCategoryAvailability {
    /** A parser constructs the element class, so an empty list is a fact about the document. */
    PARSED_DOCUMENT_CLASS,

    /** No element class: the list is session or layout state, so an empty list is still a fact. */
    SESSION_OR_LAYOUT_STATE,

    /** No parser constructs the class yet; an empty list proves nothing about the document. */
    NOT_READABLE_YET
}

/**
 * One Navigator category: the key it folds under, where its rows come from,
 * and which plan makes it readable when it is not readable yet.
 *
 * [elementClasses] names the model classes a parser would have to construct
 * for this category to be trustworthy. [NavigatorCategoryHonestyTest] searches
 * `app/src/main/java` for those constructors and fails when a category
 * classified [NavigatorCategoryAvailability.NOT_READABLE_YET] starts being
 * produced (or the reverse), so the classification cannot rot silently.
 * It asserts constructors, not field writes: `OfficeParagraph.bookmark` is
 * written by the editing API (`DocumentCoreEngines.insertBookmark`) without
 * any parser reading the file's bookmarks, which is exactly why the category
 * is not readable yet.
 *
 * The search skips this package so the index engine's own reshaping never
 * counts as a parser producing the class.
 *
 * [readableFormats] narrows a readable category to the formats whose parser
 * builds the class (Plan 7C reads ODF indexes and sections, not DOCX ones).
 * Empty means every format. For any other format the empty state falls back
 * to "not yet available", because an empty list proves nothing there.
 */
data class NavigatorCategory(
    val key: String,
    val availability: NavigatorCategoryAvailability,
    val elementClasses: List<String> = emptyList(),
    val ownerPlan: String? = null,
    val readableFormats: Set<String> = emptySet()
) {
    /** Availability for a document parsed as [format] (`ParserReport.format`). */
    fun availabilityFor(format: String?): NavigatorCategoryAvailability {
        if (readableFormats.isEmpty() || availability == NavigatorCategoryAvailability.NOT_READABLE_YET) {
            return availability
        }
        val normalized = format?.trim()?.uppercase().orEmpty()
        return if (normalized in readableFormats) availability else NavigatorCategoryAvailability.NOT_READABLE_YET
    }
}

object NavigatorCategories {

    val ALL: List<NavigatorCategory> = listOf(
        // Readable today: a parser builds these classes (or the list is app state).
        NavigatorCategory("headings", NavigatorCategoryAvailability.PARSED_DOCUMENT_CLASS, listOf("OfficeHeading", "OfficeParagraph")),
        NavigatorCategory("tables", NavigatorCategoryAvailability.PARSED_DOCUMENT_CLASS, listOf("OfficeTable")),
        NavigatorCategory("images", NavigatorCategoryAvailability.PARSED_DOCUMENT_CLASS, listOf("OfficeImage")),
        NavigatorCategory("pages", NavigatorCategoryAvailability.SESSION_OR_LAYOUT_STATE),
        NavigatorCategory("reminders", NavigatorCategoryAvailability.SESSION_OR_LAYOUT_STATE),

        // Not readable yet: the index has an arm for the class, but no parser
        // in the tree constructs it, so the limb never gets data on a real
        // file. Owner plan = the PR that makes it readable.
        // Link runs are read (OfficeTextRun.hyperlink), but no parser builds an
        // OfficeHyperlink element and no plan item owns the category yet.
        NavigatorCategory("hyperlinks", NavigatorCategoryAvailability.NOT_READABLE_YET, listOf("OfficeHyperlink"), null),
        NavigatorCategory("bookmarks", NavigatorCategoryAvailability.PARSED_DOCUMENT_CLASS, listOf("OfficeBookmark")),
        // Plan 7C: ODF text:section ranges (DocumentSectionRange, built by the
        // ODF importer). DOCX sections are not read, so DOCX keeps the
        // not-yet-available wording.
        NavigatorCategory(
            "sections",
            NavigatorCategoryAvailability.PARSED_DOCUMENT_CLASS,
            listOf("DocumentSectionRange"),
            readableFormats = setOf("ODT")
        ),
        NavigatorCategory("fields", NavigatorCategoryAvailability.NOT_READABLE_YET, listOf("OfficeField"), "plan-8B"),
        // No plan item owns these yet. Recorded as "unassigned" rather than
        // borrowing the nearest plan number: a wrong marker is worse than an
        // honest gap, and §8 of the roadmap lists these gaps for the user.
        NavigatorCategory("comments", NavigatorCategoryAvailability.NOT_READABLE_YET, listOf("OfficeComment"), null),
        NavigatorCategory("footnotes", NavigatorCategoryAvailability.NOT_READABLE_YET, listOf("OfficeFootnoteElement"), null),
        NavigatorCategory("shapes", NavigatorCategoryAvailability.NOT_READABLE_YET, listOf("OfficeShape"), null),
        // Frames have no element class at all: ODT draw:frames reach the model
        // as OfficeImage, so "Text Frames" would list pictures under a second
        // name. Not a stub to keep, a gap to name.
        NavigatorCategory("frames", NavigatorCategoryAvailability.NOT_READABLE_YET, emptyList(), null),
        // Plan 7C: authored ODF indexes (DocumentIndexRange, built by the ODF
        // importer). DOCX indexes are not read yet.
        NavigatorCategory(
            "indexes",
            NavigatorCategoryAvailability.PARSED_DOCUMENT_CLASS,
            listOf("DocumentIndexRange"),
            readableFormats = setOf("ODT")
        ),
        // OLE rows come from OfficeResources.objects, which no parser fills.
        // The class itself is constructed (as a default), so there is no
        // constructor to assert on here: this one stays hand-checked.
        NavigatorCategory("ole", NavigatorCategoryAvailability.NOT_READABLE_YET, emptyList(), null)
    )

    private val byKey: Map<String, NavigatorCategory> = ALL.associateBy { it.key }

    /** Fails loudly instead of defaulting: an unstamped category must not slip through. */
    fun of(key: String): NavigatorCategory =
        byKey[key] ?: error("Navigator category '$key' is not declared in NavigatorCategories.ALL")

    fun isReadable(key: String): Boolean =
        of(key).availability != NavigatorCategoryAvailability.NOT_READABLE_YET
}
