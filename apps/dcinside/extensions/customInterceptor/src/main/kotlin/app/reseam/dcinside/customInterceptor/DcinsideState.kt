package app.reseam.dcinside.customInterceptor

private data class State(
    val appId: String? = null,
    val userId: String? = null,
    val galleryId: String? = null,
    val galleryType: String? = null,
    /** Null means the manager status has not been determined for the current gallery. */
    val isGalleryManager: Boolean? = null,
)

/** Shared state for the interceptor and other DCInside extension modules. */
object DcinsideState {
    @Volatile
    private var state = State()

    val appId: String?
        get() = state.appId

    val userId: String?
        get() = state.userId

    val galleryId: String?
        get() = state.galleryId

    val galleryType: String?
        get() = state.galleryType

    val isGalleryManager: Boolean?
        get() = state.isGalleryManager

    @Synchronized
    fun updateAppId(appId: String?) {
        state = state.copy(appId = appId)
    }

    @Synchronized
    fun updateUserId(userId: String?) {
        state = state.copy(userId = userId)
    }

    /** A gallery change invalidates the manager status observed for the previous gallery. */
    @Synchronized
    fun updateGalleryId(galleryId: String?) {
        state = state.copy(
            galleryId = galleryId,
            galleryType = null,
            isGalleryManager = null,
        )
    }

    @Synchronized
    fun updateGalleryType(galleryType: String?) {
        state = state.copy(galleryType = galleryType)
    }

    @Synchronized
    fun updateGalleryManagerStatus(isGalleryManager: Boolean?) {
        state = state.copy(isGalleryManager = isGalleryManager)
    }

    @Synchronized
    fun reset() {
        state = State()
    }
}
