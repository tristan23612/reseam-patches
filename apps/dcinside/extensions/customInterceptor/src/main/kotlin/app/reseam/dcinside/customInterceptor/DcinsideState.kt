package app.reseam.dcinside.customInterceptor

private data class State(
    val appId: String? = null,
    val userId: String? = null,
    val galleryId: String? = null,
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

    val isGalleryManager: Boolean?
        get() = state.isGalleryManager

    @Synchronized
    fun updateAccount(appId: String?, userId: String?) {
        state = state.copy(appId = appId, userId = userId)
    }

    /** A gallery change invalidates the manager status observed for the previous gallery. */
    @Synchronized
    fun updateGalleryId(galleryId: String?) {
        state = state.copy(
            galleryId = galleryId,
            isGalleryManager = null,
        )
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
