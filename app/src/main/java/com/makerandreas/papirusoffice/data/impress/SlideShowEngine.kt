package com.makerandreas.papirusoffice.data.impress

// ============================================================================
// LibreOffice SDK Guide: Chapter 18. Slide Shows
// Papirus Engine Mock Implementation
// ============================================================================

// --- Mock Interfaces ---

interface XPresentationSupplier {
    fun getPresentation(): XPresentation
}

interface XPresentation {
    fun start()
    fun end()
}

interface XPresentation2 : XPresentation {
    fun getController(): XSlideShowController?
}

interface XSlideShowController {
    fun getCurrentSlideIndex(): Int
    fun getSlideCount(): Int
    fun deactivate()
}

interface XPropertySet {
    fun setPropertyValue(propertyName: String, value: Any)
    fun getPropertyValue(propertyName: String): Any?
}

interface XCustomPresentationSupplier {
    fun getCustomPresentations(): XNameContainer
}

interface XNameContainer {
    fun insertByName(name: String, element: Any)
    fun getByName(name: String): Any
}

interface XIndexContainer {
    fun insertByIndex(index: Int, element: Any)
    fun getByIndex(index: Int): Any
}

interface XSingleServiceFactory {
    fun createInstance(): Any
}

// --- Implementation Classes ---

class PapirusPresentation : XPresentation2, XPropertySet {
    private val props = mutableMapOf<String, Any>()
    private var controller: PapirusSlideShowController? = null

    init {
        // Default properties
        props["AllowAnimations"] = true
        props["IsAlwaysOnTop"] = false
        props["IsAutomatic"] = false
        props["IsEndless"] = false
        props["IsFullScreen"] = true
        props["IsMouseVisible"] = false
        props["IsShowAll"] = true
        props["IsShowLogo"] = false
        props["IsTransitionOnClick"] = true
        props["Pause"] = 10
        props["StartWithNavigator"] = false
        props["UsePen"] = false
        props["CustomShow"] = ""
    }

    override fun start() {
        controller = PapirusSlideShowController()
        println("Slide Show Started")
    }

    override fun end() {
        controller = null
        println("Slide Show Ended")
    }

    override fun getController(): XSlideShowController? = controller

    override fun setPropertyValue(propertyName: String, value: Any) {
        props[propertyName] = value
    }

    override fun getPropertyValue(propertyName: String): Any? = props[propertyName]
}

class PapirusSlideShowController : XSlideShowController {
    var currentIdx = 0
    var totalSlides = 10 // Mock value
    var isActive = true

    override fun getCurrentSlideIndex(): Int = if (isActive) currentIdx else -1

    override fun getSlideCount(): Int = totalSlides

    override fun deactivate() {
        isActive = false
    }
}

class PapirusCustomPresentations : XNameContainer, XSingleServiceFactory {
    private val presentations = mutableMapOf<String, Any>()

    override fun insertByName(name: String, element: Any) {
        presentations[name] = element
    }

    override fun getByName(name: String): Any = presentations[name] ?: throw Exception("Not found")

    override fun createInstance(): Any {
        return PapirusIndexContainer()
    }
}

class PapirusIndexContainer : XIndexContainer {
    private val elements = mutableMapOf<Int, Any>()

    override fun insertByIndex(index: Int, element: Any) {
        elements[index] = element
    }

    override fun getByIndex(index: Int): Any = elements[index] ?: throw Exception("Not found")
}

// Enum mocks
enum class FadeEffect {
    NONE, FADE_FROM_LEFT, FADE_FROM_TOP, FADE_FROM_RIGHT, FADE_FROM_BOTTOM, DISSOLVE, RANDOM
}

enum class AnimationSpeed {
    SLOW, MEDIUM, FAST
}

// --- SDK Utility Methods (Draw class equivalent) ---
object SlideShowDraw {
    const val CLICK_ALL_CHANGE = 0
    const val AUTO_CHANGE = 1
    const val CLICK_PAGE_CHANGE = 2

    val slideProperties = mutableMapOf<XDrawPage, MutableMap<String, Any>>()

    fun getShow(doc: XComponent): XPresentation2 {
        // Mock returning a presentation
        return PapirusPresentation()
    }

    fun getShowController(show: XPresentation2): XSlideShowController? {
        var sc = show.getController()
        var numTries = 1
        while (sc == null && numTries < 4) {
            Thread.sleep(100) // Mock delay
            numTries++
            sc = show.getController()
        }
        return sc
    }

    fun waitEnded(sc: XSlideShowController) {
        while (sc.getCurrentSlideIndex() != -1) {
            Thread.sleep(100)
            // Mock ending after a bit
            (sc as PapirusSlideShowController).deactivate()
        }
        println("End of presentation detected")
    }

    fun waitLast(sc: XSlideShowController, delay: Int) {
        val numSlides = sc.getSlideCount()
        while (sc.getCurrentSlideIndex() < numSlides - 1) {
            Thread.sleep(100)
            (sc as PapirusSlideShowController).currentIdx++
        }
        Thread.sleep(delay.toLong())
    }

    fun setTransition(
        slide: XDrawPage,
        fadeEffect: FadeEffect,
        speed: AnimationSpeed,
        change: Int,
        duration: Int
    ) {
        val props = slideProperties.getOrPut(slide) { mutableMapOf() }
        props["Effect"] = fadeEffect
        props["Speed"] = speed
        props["Change"] = change
        props["Duration"] = duration
        println("Set transition on slide: Effect=\$fadeEffect, Speed=\$speed, Change=\$change, Duration=\$duration")
    }

    fun getPlayList(doc: XComponent): XNameContainer {
        // Mock
        return PapirusCustomPresentations()
    }

    fun buildPlayList(doc: XComponent, slideIdxs: IntArray, customName: String): XNameContainer? {
        val playList = getPlayList(doc)
        try {
            val factory = playList as XSingleServiceFactory
            val slidesCon = factory.createInstance() as XIndexContainer

            println("Building play list using: ")
            for (j in slideIdxs.indices) {
                // Mock getting slide from previous Deck Engine
                val slide = PapirusDrawPage() 
                slidesCon.insertByIndex(j, slide)
                println("  Slide " + slideIdxs[j])
            }
            playList.insertByName(customName, slidesCon)
            println("Playlist has name: $customName\n")
            return playList
        } catch (e: Exception) {
            println("Unable to build play list: $e")
            return null
        }
    }
}
