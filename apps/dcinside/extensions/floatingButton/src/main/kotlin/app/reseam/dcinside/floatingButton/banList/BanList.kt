@file:JvmName("BanList")

package app.reseam.dcinside.floatingButton.banList

import android.annotation.SuppressLint
import android.view.View
import app.reseam.dcinside.floatingButton.FloatingButtonRegistry
import app.reseam.dcinside.customInterceptor.DcinsideState
import app.reseam.runtime.Export
import app.reseam.runtime.ToggleValue

private const val BAN_LIST_BUTTON_ID = "reseam_ban_list_button"

@Volatile private var showBanList: ToggleValue? = null

@Export
fun showBanList(key: String, defaultValue: Boolean) {
    showBanList = ToggleValue("tristan-patches", key, defaultValue)
    FloatingButtonRegistry.register("ban_list") { rootView, visible ->
        setBanListButtonVisibility(rootView, visible, BAN_LIST_BUTTON_ID)
    }
}

@Export
fun enableBanListIdentifierSearch(key: String, defaultValue: Boolean) {
    BanListSettings.enableBanListIdentifierSearch(key, defaultValue)
}

@Export
fun enableBanListIngest(key: String, defaultValue: Boolean) {
    BanListSettings.enableBanListIngest(key, defaultValue)
}

@Export
fun banListWorkerUrl(key: String, defaultValue: String) {
    BanListSettings.banListWorkerUrl(key, defaultValue)
}

@Export
fun banListWorkerViewToken(key: String, defaultValue: String) {
    BanListSettings.banListWorkerViewToken(key, defaultValue)
}

@Export
fun banListWorkerIngestToken(key: String, defaultValue: String) {
    BanListSettings.banListWorkerIngestToken(key, defaultValue)
}

@Export
fun galleriesJson(key: String, defaultValue: String) {
    BanListSettings.galleriesJson(key, defaultValue)
}

@Export
fun dcinsideUserId(key: String, defaultValue: String) {
    BanListSettings.dcinsideUserId(key, defaultValue)
}

@Export
fun dcinsidePassword(key: String, defaultValue: String) {
    BanListSettings.dcinsidePassword(key, defaultValue)
}

@SuppressLint("DiscouragedApi")
private fun setBanListButtonVisibility(targetView: View, visible: Boolean, buttonIdName: String): Boolean {
    val root = targetView.rootView
    val id = root.resources.getIdentifier(buttonIdName, "id", root.context.packageName)
    if (id == 0) return false
    val button = root.findViewById<View>(id) ?: return false
    button.setOnClickListener { BanListSession(button.context).start() }

    val shouldShow = visible &&
        showBanList?.value == true &&
        DcinsideState.isGalleryManager == true &&
        BanListSettings.hasAnyEnabledAction
    button.visibility = if (shouldShow) View.VISIBLE else View.GONE
    return shouldShow
}
