package com.makerandreas.papirusoffice.data.writer

/**
 * Abstraction interface for document layout tree pointers (upper, lower, next, prev)
 * based on LibreOffice Writer 'SwFrame'.
 */
interface SwFrame {
    val id: String
    var upper: SwFrame?
    var lower: SwFrame?
    var next: SwFrame?
    var prev: SwFrame?
}

/**
 * Manages frame flow across pages using master/follow relationship for layout splitting.
 */
open class SwFlowFrame(
    override val id: String
) : SwFrame {

    override var upper: SwFrame? = null
    override var lower: SwFrame? = null
    override var next: SwFrame? = null
    override var prev: SwFrame? = null

    var master: SwFlowFrame? = null
    var follow: SwFlowFrame? = null

    fun isMaster(): Boolean = master == null && follow != null

    fun isFollow(): Boolean = master != null

    /**
     * Splits this frame into a follow frame when text/content overflows page boundaries.
     */
    fun createFollowFrame(followId: String): SwFlowFrame {
        val newFollow = SwFlowFrame(followId)
        newFollow.master = this
        this.follow = newFollow
        return newFollow
    }

    /**
     * Merges follow frame back into master frame if content fits on a single page again.
     */
    fun mergeFollowFrame(): Boolean {
        val targetFollow = follow ?: return false
        this.follow = targetFollow.follow
        targetFollow.follow?.master = this
        targetFollow.master = null
        targetFollow.follow = null
        return true
    }

    fun getRootMaster(): SwFlowFrame {
        var current: SwFlowFrame = this
        while (current.master != null) {
            current = current.master!!
        }
        return current
    }

    fun getTailFollow(): SwFlowFrame {
        var current: SwFlowFrame = this
        while (current.follow != null) {
            current = current.follow!!
        }
        return current
    }
}
