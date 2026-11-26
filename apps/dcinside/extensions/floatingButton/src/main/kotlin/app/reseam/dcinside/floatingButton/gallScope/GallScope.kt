@file:JvmName("GallScope")

package app.reseam.dcinside.floatingButton.gallScope

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import app.reseam.dcinside.floatingButton.FloatingButtonRegistry
import app.reseam.dcinside.management.ui.ManagementDialogUi
import app.reseam.runtime.Export
import app.reseam.runtime.ToggleValue
import java.util.WeakHashMap

private const val GALL_SCOPE_BUTTON_ID = "reseam_gall_scope_button"

@Volatile private var showGallScopeValue: ToggleValue? = null

@Export
fun showGallScope(key: String, defaultValue: Boolean) {
    showGallScopeValue = ToggleValue("tristan-patches", key, defaultValue)
    FloatingButtonRegistry.register("gall_scope") { rootView, visible ->
        setGallScopeButtonVisibility(rootView, visible, GALL_SCOPE_BUTTON_ID)
    }
}

@SuppressLint("DiscouragedApi")
private fun setGallScopeButtonVisibility(targetView: View, visible: Boolean, buttonIdName: String): Boolean {
    val root = targetView.rootView
    val id = root.resources.getIdentifier(buttonIdName, "id", root.context.packageName)
    if (id == 0) return false
    val button = root.findViewById<View>(id) ?: return false
    GallScopeController.bindButton(button)
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
    if (context == null) return
    GallScopeController.openOrResume(context, prefillUserId, prefillGalleryType, prefillGalleryId)
}

internal object GallScopeController {
    @Volatile private var activeSession: GallScopeSession? = null
    @Volatile private var searching = false
    private val buttons = WeakHashMap<View, ColorStateList?>()

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

    fun bindButton(button: View) {
        synchronized(buttons) {
            if (!buttons.containsKey(button)) buttons[button] = button.backgroundTintList
        }
        button.post { applyColor(button) }
    }

    fun searchingChanged(value: Boolean) {
        searching = value
        val snapshot = synchronized(buttons) { buttons.keys.toList() }
        snapshot.forEach { button -> button.post { applyColor(button) } }
    }

    private fun applyColor(button: View) {
        val originalTint = synchronized(buttons) { buttons[button] }
        val searchTint = if (searching) runCatching {
            ColorStateList.valueOf(ManagementDialogUi.create(button.context).colors.primary)
        }.getOrNull() else null
        button.backgroundTintList = searchTint ?: originalTint
    }
}
