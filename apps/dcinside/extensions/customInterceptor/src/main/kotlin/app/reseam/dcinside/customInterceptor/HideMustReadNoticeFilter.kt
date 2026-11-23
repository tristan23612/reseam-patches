package app.reseam.dcinside.customInterceptor

import app.reseam.dcinside.settings.DcinsideSettings
import app.reseam.runtime.Export
import org.json.JSONObject

@Export
object HideMustReadNoticeFilter : JsonResponseFilter {
    @Export
    @JvmStatic
    fun register() {
        JsonResponseFilters.register(this)
    }

    override val id = "hide-must-read-notice"

    override fun isEnabled(): Boolean = DcinsideSettings.hideMustReadNotice.value

    override fun matches(context: JsonResponseContext): Boolean = context.isListRequest

    override fun apply(context: JsonResponseContext, json: JSONObject): JSONObject {
        val galleryInfo = json.optJSONArray("gall_info")?.optJSONObject(0) ?: return json
        galleryInfo.remove("must_read")
        return json
    }
}
