package app.reseam.dcinside.customInterceptor

import app.reseam.dcinside.settings.DcinsideSettings
import app.reseam.runtime.Export
import org.json.JSONArray
import org.json.JSONObject

@Export
object HideAdminNoticeFilter : JsonResponseFilter {
    @Export
    @JvmStatic
    fun register() {
        JsonResponseFilters.register(this)
    }

    override val id = "hide-admin-notice"

    override fun isEnabled(): Boolean = DcinsideSettings.hideAdminNotice.value

    override fun matches(context: JsonResponseContext): Boolean = context.isListRequest

    override fun apply(context: JsonResponseContext, json: JSONObject): JSONObject {
        val gallList = json.optJSONArray("gall_list") ?: return json
        val filtered = JSONArray()

        for (index in 0 until gallList.length()) {
            val item = gallList.opt(index)
            val post = item as? JSONObject
            if (post == null || post.optString("no").isNotEmpty()) {
                filtered.put(item)
            }
        }

        json.put("gall_list", filtered)
        return json
    }
}
