package app.reseam.dcinside.customInterceptor

import android.net.Uri
import org.json.JSONObject

data class JsonResponseContext(
    val url: Uri,
    val method: String,
    val isListRequest: Boolean,
)

interface JsonResponseFilter {
    val id: String
    val order: Int get() = 0

    fun isEnabled(): Boolean = true
    fun matches(context: JsonResponseContext): Boolean = true
    fun apply(context: JsonResponseContext, json: JSONObject): JSONObject
}
