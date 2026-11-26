@file:JvmName("GallScope")

package app.reseam.dcinside.floatingButton.gallScope

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import app.reseam.dcinside.floatingButton.FloatingButtonRegistry
import app.reseam.dcinside.management.ui.ManagementDialogUi
import app.reseam.runtime.Export
import app.reseam.runtime.ToggleValue
import java.util.WeakHashMap

private const val GALL_SCOPE_BUTTON_ID = "reseam_gall_scope_button"

@Volatile private var showGallScopeValue: ToggleValue? = null
@Volatile private var enableUserIdDirectScopeValue: ToggleValue? = null

@Export
fun showGallScope(key: String, defaultValue: Boolean) {
    showGallScopeValue = ToggleValue("tristan-patches", key, defaultValue)
    FloatingButtonRegistry.register("gall_scope") { rootView, visible ->
        setGallScopeButtonVisibility(rootView, visible, GALL_SCOPE_BUTTON_ID)
    }
}

@Export
fun enableUserIdDirectScope(key: String, defaultValue: Boolean) {
    enableUserIdDirectScopeValue = ToggleValue("tristan-patches", key, defaultValue)
}

@SuppressLint("DiscouragedApi")
private fun setGallScopeButtonVisibility(targetView: View, visible: Boolean, buttonIdName: String): Boolean {
    val root = targetView.rootView
    val id = root.resources.getIdentifier(buttonIdName, "id", root.context.packageName)
    if (id == 0) return false
    val button = root.findViewById<View>(id) ?: return false
    val icon = (button as? ViewGroup)?.let { group ->
        (0 until group.childCount).asSequence().map { group.getChildAt(it) }.filterIsInstance<ImageView>().firstOrNull()
    }
    if (icon != null) GallScopeController.bindIcon(icon)
    button.setOnClickListener { GallScopeController.openOrResume(button.context) }

    val shouldShow = visible && showGallScopeValue?.value == true
    button.visibility = if (shouldShow) View.VISIBLE else View.GONE
    return shouldShow
}

@Export
fun showGallScopeDialogWithUserId(
    context: Context?,
    prefillUserId: String?,
    prefillGalleryType: String?,
    prefillGalleryId: String?,
) {
    if (enableUserIdDirectScopeValue?.value != true || context == null) return
    GallScopeController.openOrResume(context, prefillUserId, prefillGalleryType, prefillGalleryId)
}

internal object GallScopeController {
    @Volatile private var activeSession: GallScopeSession? = null
    @Volatile private var searching = false
    private val icons = WeakHashMap<ImageView, Unit>()

    fun openOrResume(context: Context, userId: String? = null, galleryType: String? = null, galleryId: String? = null) {
        val session = synchronized(this) {
            activeSession?.also { return@synchronized it }
                ?: GallScopeSession(context, userId, galleryType, galleryId) { ended ->
                    synchronized(this) {
                        if (activeSession === ended) activeSession = null
                    }
                }.also { activeSession = it }
        }
        if (session === activeSession && session.isStarted()) session.resume() else session.start()
    }

    fun bindIcon(icon: ImageView) {
        synchronized(icons) { icons[icon] = Unit }
        icon.post { applyColor(icon) }
    }

    fun searchingChanged(value: Boolean) {
        searching = value
        val snapshot = synchronized(icons) { icons.keys.toList() }
        snapshot.forEach { icon -> icon.post { applyColor(icon) } }
    }

    private fun applyColor(icon: ImageView) {
        if (!searching) {
            icon.clearColorFilter()
            return
        }
        val color = runCatching {
            ManagementDialogUi.create(icon.context).colors.primary
        }.getOrNull() ?: return
        icon.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
    }
}
