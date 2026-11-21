@file:JvmName("HideHomeElements")

package app.reseam.dcinside.hideHomeElements

import android.view.View
import android.view.ViewGroup
import app.reseam.dcinside.settings.DcinsideSettings
import app.reseam.runtime.Export

@Export
fun hideBestPickView(view: View?) {
    if (DcinsideSettings.hideBestPickItem.value) hideView(view)
}

@Export
fun hideRecommendedGalleriesView(view: View?) {
    if (DcinsideSettings.hideRecommendedGalleries.value) hideView(view)
}

@Export
fun hideCrowdView(view: View?) {
    if (DcinsideSettings.hideCrowd.value) hideView(view)
}

@Export
fun hideRecentView(view: View?) {
    if (DcinsideSettings.hideRecent.value) hideView(view)
}

@Export
fun hideNewGalleries(list: List<*>?): List<*>? =
    if (DcinsideSettings.hideNewGalleries.value && list != null) emptyList<Any>() else list

private fun hideView(view: View?) {
    if (view == null) return

    view.visibility = View.GONE
    view.setPadding(0, 0, 0, 0)
    view.minimumWidth = 0
    view.minimumHeight = 0

    val params = view.layoutParams
    if (params != null) {
        params.width = 0
        params.height = 0
        if (params is ViewGroup.MarginLayoutParams) params.setMargins(0, 0, 0, 0)
        view.layoutParams = params
    }

    if (view is ViewGroup) {
        for (index in 0 until view.childCount) hideView(view.getChildAt(index))
    }
}
@Export
fun adjustDimension(originalDimension: Int, dimensionName: String): Int = when {
    DcinsideSettings.hideCrowd.value && dimensionName == "main_space_divide4" -> 0
    DcinsideSettings.hideBestPickItem.value && dimensionName in setOf("main_space_divide5", "main_space_end") -> 0
    else -> originalDimension
}
