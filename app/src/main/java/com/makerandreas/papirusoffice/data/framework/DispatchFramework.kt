package com.makerandreas.papirusoffice.data.framework

data class URL(val complete: String, val main: String = "", val protocol: String = "")

data class FeatureStateEvent(
    val source: Any,
    val featureURL: URL,
    val featureDescriptor: String,
    val isEnabled: Boolean,
    val requery: Boolean,
    val state: Any?
)

interface XStatusListener : XEventListener {
    fun statusChanged(event: FeatureStateEvent)
}

interface XDispatch {
    fun dispatch(url: URL, arguments: MediaDescriptor)
    fun addStatusListener(control: XStatusListener, url: URL)
    fun removeStatusListener(control: XStatusListener, url: URL)
}

data class DispatchResultEvent(
    val source: Any,
    val state: Short,
    val result: Any?
)

interface XDispatchResultListener : XEventListener {
    fun dispatchFinished(event: DispatchResultEvent)
}

interface XNotifyingDispatch : XDispatch {
    fun dispatchWithNotification(url: URL, arguments: MediaDescriptor, listener: XDispatchResultListener)
}

interface XDispatchProvider {
    fun queryDispatch(url: URL, targetFrameName: String, searchFlags: Int): XDispatch?
    fun queryDispatches(requests: List<DispatchDescriptor>): List<XDispatch?>
}

data class DispatchDescriptor(
    val featureURL: URL,
    val frameName: String,
    val searchFlags: Int
)

interface XDispatchProviderInterceptor : XDispatchProvider {
    fun getSlaveDispatchProvider(): XDispatchProvider?
    fun setSlaveDispatchProvider(slave: XDispatchProvider?)
    fun getMasterDispatchProvider(): XDispatchProvider?
    fun setMasterDispatchProvider(master: XDispatchProvider?)
}

interface XDispatchProviderInterception {
    fun registerDispatchProviderInterceptor(interceptor: XDispatchProviderInterceptor)
    fun releaseDispatchProviderInterceptor(interceptor: XDispatchProviderInterceptor)
}
