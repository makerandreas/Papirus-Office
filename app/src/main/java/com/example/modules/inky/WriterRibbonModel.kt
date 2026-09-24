package com.example.modules.inky

/**
 * Inky's ribbon deck identities. One enum value per deck that actually has
 * content; the pager hosts exactly this list.
 */
enum class WriterRibbonDeck { FILE, HOME }

/**
 * The ribbon tab strip, per `CONCEPT.md`'s Writer set: File, Home, Insert,
 * Layout, Review, View. `CONCEPT.md` does not list References or Mailings for
 * this module, so they are not declared: a tab that can never be enabled is a
 * louder lie than an absent one (plan 3B item 3.6). The contextual tabs
 * (Drawing, Object, Picture, Table, Fontwork, Chart) join the strip when a
 * selection context can raise them.
 *
 * A tab without a [deck] has no content anywhere in the app. It stays visible
 * and answers a press with the honest note instead of navigating to an empty
 * deck (plan-03 §0 decision: disabled tab + honest note). [withDecks] is both
 * the pager's page list and the strip's selected-tab source, so a deck page
 * and a strip position are never assumed to be the same number.
 *
 * Labels stay as declared here, in one place, until the wider copy sweep
 * reaches plain `Text()` arguments (plan-03 3.1/3.2 covered toasts and
 * contentDescriptions only).
 */
enum class WriterRibbonTab(val label: String, val deck: WriterRibbonDeck?) {
    FILE("File", WriterRibbonDeck.FILE),
    HOME("Home", WriterRibbonDeck.HOME),
    INSERT("Insert", null),
    LAYOUT("Layout", null),
    REVIEW("Review", null),
    VIEW("View", null);

    val isImplemented: Boolean get() = deck != null

    companion object {
        /** Tabs that own a deck, in strip order: the pager's page list. */
        val withDecks: List<WriterRibbonTab> = entries.filter { it.isImplemented }

        /** Page index for [tab], or -1 when the tab has no deck to show. */
        fun pageOf(tab: WriterRibbonTab): Int = withDecks.indexOf(tab)
    }
}
