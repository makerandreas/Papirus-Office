package com.sun.star.comp.helper

import com.sun.star.uno.XComponentContext

/**
 * Stub class for compilation fallback in AI Studio environment.
 */
object Bootstrap {
    @JvmStatic
    fun createInitialComponentContext(context: Any?): XComponentContext? {
        return object : XComponentContext {}
    }
}
