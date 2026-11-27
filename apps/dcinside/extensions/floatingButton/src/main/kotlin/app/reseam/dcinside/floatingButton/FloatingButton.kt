@file:JvmName("FloatingButton")

package app.reseam.dcinside.floatingButton

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import app.reseam.runtime.Export

internal object FloatingButtonRegistry {
    private val visibilityHandlers = linkedMapOf<String, (View, Boolean) -> Boolean>()

    @Synchronized
    fun register(buttonName: String, handler: (View, Boolean) -> Boolean) {
        visibilityHandlers[buttonName] = handler
    }

    fun updateAll(rootView: View, visible: Boolean): Boolean {
        val handlers = synchronized(this) { visibilityHandlers.values.toList() }
        val results = handlers.map { handler -> handler(rootView, visible) }
        return results.any { it }
    }
}

/** Updates all registered buttons before deciding whether the shared toggle is needed. */
@SuppressLint("DiscouragedApi")
@Export
fun setFloatingButtonToggleVisibility(targetView: View?, visible: Boolean, idPrefix: String) {
    Log.d("FloatingButton", "setFloatingButtonToggleVisibility: $visible")
    targetView ?: return

    val rootView = targetView.rootView
    val anyButtonVisible = FloatingButtonRegistry.updateAll(rootView, visible)
    val resources = rootView.resources
    val packageName = rootView.context.packageName
    val toggleId = resources.getIdentifier("${idPrefix}_toggle", "id", packageName)
    val subContainerId = resources.getIdentifier("${idPrefix}_sub_container", "id", packageName)
    if (toggleId == 0) return

    val toggleButton = rootView.findViewById<View>(toggleId) ?: return
    val subContainer = rootView.findViewById<View>(subContainerId)
    val toggleIcon = findImageViewChild(toggleButton) ?: toggleButton

    if (subContainer != null) {
        toggleButton.setOnClickListener {
            val expanded = subContainer.visibility == View.VISIBLE
            animateToggle(toggleIcon, subContainer, !expanded)
        }
    }

    val shouldShow = visible && anyButtonVisible
    toggleButton.visibility = if (shouldShow) View.VISIBLE else View.GONE
    if (!shouldShow && subContainer?.visibility == View.VISIBLE) {
        subContainer.animate().cancel()
        subContainer.visibility = View.GONE
        subContainer.alpha = 1f
        subContainer.translationY = 0f
        toggleIcon.animate().cancel()
        toggleIcon.rotation = 0f
    }
}

private fun findImageViewChild(parent: View): View? {
    if (parent !is ViewGroup) return null
    for (index in 0 until parent.childCount) {
        val child = parent.getChildAt(index)
        if (child is ImageView) return child
    }
    return null
}

private fun animateToggle(toggleIcon: View, subContainer: View, expand: Boolean) {
    if (expand) {
        subContainer.visibility = View.VISIBLE
        subContainer.alpha = 0f
        subContainer.translationY = 40f
        subContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(200)
            .start()
    } else {
        subContainer.animate()
            .alpha(0f)
            .translationY(40f)
            .setDuration(200)
            .withEndAction { subContainer.visibility = View.GONE }
            .start()
    }

    toggleIcon.animate()
        .rotation(if (expand) 45f else 0f)
        .setDuration(200)
        .start()
}

