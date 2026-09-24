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
 * The search skips this package. `DocumentIndexEngine` does construct
 * `OfficeSection` from `doc.sections`, but nothing fills `doc.sections`, so
 * counting that as production would classify sections as readable on the
 * strength of a list no parser ever populates.
 */
data class NavigatorCategory(
    val key: String,
    val availability: NavigatorCategoryAvailability,
    val elementClasses: List<String> = emptyList(),
    val ownerPlan: String? = null
)

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
        NavigatorCategory("hyperlinks", NavigatorCategoryAvailability.NOT_READABLE_YET, listOf("OfficeHyperlink"), "plan-18/20"),
        NavigatorCategory("bookmarks", NavigatorCategoryAvailability.NOT_READABLE_YET, listOf("OfficeBookmark"), "plan-18/20"),
        NavigatorCategory("sections", NavigatorCategoryAvailability.NOT_READABLE_YET, listOf("OfficeSection"), "plan-19/21"),
        NavigatorCategory("fields", NavigatorCategoryAvailability.NOT_READABLE_YET, listOf("OfficeField"), "plan-21"),
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
        // Indexes are authored TOC/index content (a table of contents is not a
        // heading, a table or an image): the parsers drop the whole snapshot
        // today, so the category may not claim the document has none. Read by
        // plan 19 (ODF) and plan 21 (DOCX).
        NavigatorCategory("indexes", NavigatorCategoryAvailability.NOT_READABLE_YET, emptyList(), "plan-19/21"),
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
