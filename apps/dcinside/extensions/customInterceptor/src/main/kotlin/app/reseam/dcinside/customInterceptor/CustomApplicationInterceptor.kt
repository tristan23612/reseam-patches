@file:JvmName("CustomApplicationInterceptor")

package app.reseam.dcinside.customInterceptor

import android.net.Uri
import android.util.Base64
import android.util.Log
import app.reseam.runtime.Export
import app.reseam.runtime.ToggleValue
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject

@Export
fun register(builder: okhttp3.OkHttpClient.Builder?) {
    if (builder == null || builder.interceptors().any { it is DcinsideInterceptor }) return
    builder.addInterceptor(DcinsideInterceptor())
}

@Volatile private var showUserIdToggleValue: ToggleValue? = null
@Volatile private var postLongPressToggleValue: ToggleValue? = null

@Export
fun showUserId(key: String, defaultValue: Boolean) {
    showUserIdToggleValue = ToggleValue("tristan-patches", key, defaultValue)
}
@Export
fun postLongPress(key: String, defaultValue: Boolean) {
    postLongPressToggleValue = ToggleValue("tristan-patches", key, defaultValue)
}

private class DcinsideInterceptor : Interceptor {
    private val tag = "DcinsideInterceptor"
    private val targetPaths = listOf("gall_list_new", "gall_view_new", "comment_new")

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = effectiveUrl(request) ?: return chain.proceed(request)
        val path = url.path.orEmpty()
        val isListRequest = path.contains("gall_list_new")
        val isTargetRequest = targetPaths.any(path::contains)

        updateRequestState(url, isTargetRequest)

        val response = chain.proceed(request)
        if (!isTargetRequest) return response

        val body = response.body
        val contentType = body.contentType()
        val originalBytes = body.bytes()

        return try {
            val charset = contentType?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
            val json = JSONArray(String(originalBytes, charset))
            val context = JsonResponseContext(url, request.method, isListRequest)

            // Read app state from the server's original JSON before feature filters change it.
            updateResponseState(json, isListRequest)

            val filteredJson = JsonResponseFilters.apply(context, json)
            copyKey(filteredJson, path, "user_id")
            responseWithBody(response, filteredJson.toString().toByteArray(charset), contentType)
        } catch (exception: Exception) {
            Log.e(tag, "Failed to process JSON response from $path", exception)
            // bytes() consumed the original body; restore it for the app on every fallback path.
            responseWithBody(response, originalBytes, contentType)
        }
    }

    private fun copyKey(response: JSONArray, path: String, key: String) {
        when {
            path.contains("gall_list_new") -> {
                for (index in 0 until response.length()) {
                    val payload = response.optJSONObject(index) ?: continue
                    val posts = payload.optJSONArray("gall_list") ?: continue
                    for (postIndex in 0 until posts.length()) {
                        posts.optJSONObject(postIndex)?.copyKeyToReseamKey(key)
                    }
                }
            }

            path.contains("gall_view_new") -> {
                for (index in 0 until response.length()) {
                    response.optJSONObject(index)
                        ?.optJSONObject("view_info")
                        ?.copyKeyToReseamKey(key)
                }
            }

            path.contains("comment_new") -> {
                for (index in 0 until response.length()) {
                    val payload = response.optJSONObject(index) ?: continue
                    val comments = payload.optJSONArray("comment_list") ?: continue
                    for (commentIndex in 0 until comments.length()) {
                        comments.optJSONObject(commentIndex)?.copyKeyToReseamKey(key)
                    }
                }
            }
        }
    }

    private fun JSONObject.copyKeyToReseamKey(key: String) {
        if (has(key) && !isNull(key)) {
            put("reseam_$key", get(key))
        }
    }

    private fun effectiveUrl(request: Request): Uri? {
        val requestUrl = request.url
        if (requestUrl.encodedPath != "/api/redirect.php") return Uri.parse(requestUrl.toString())

        val hash = requestUrl.queryParameter("hash") ?: return null
        return runCatching {
            Uri.parse(String(Base64.decode(hash, Base64.DEFAULT), Charsets.UTF_8))
        }.getOrNull()
    }

    private fun updateRequestState(url: Uri, isTargetRequest: Boolean) {
        url.getQueryParameter("app_id")
            ?.takeIf(String::isNotBlank)
            ?.let(DcinsideState::updateAppId)

        listOf("user_id", "confirm_id").forEach { key ->
            url.getQueryParameter(key)
                ?.takeIf(String::isNotBlank)
                ?.let(DcinsideState::updateUserId)
        }

        if (!isTargetRequest) return
        url.getQueryParameter("id")
            ?.takeIf(String::isNotBlank)
            ?.takeIf { it != DcinsideState.galleryId }
            ?.let(DcinsideState::updateGalleryId)
    }

    private fun updateResponseState(response: JSONArray, isListRequest: Boolean) {
        for (index in 0 until response.length()) {
            response.optJSONObject(index)?.let { updateGalleryMetadata(it, isListRequest) }
        }
    }

    private fun updateGalleryMetadata(json: JSONObject, isListRequest: Boolean) {
        val galleryInfo = json.optJSONArray("gall_info")?.optJSONObject(0)
            ?: json.optJSONObject("view_info")
            ?: return

        val galleryType = when {
            galleryInfo.optBoolean("is_minor", false) -> "mgallery"
            galleryInfo.optBoolean("is_mini", false) -> "mini"
            else -> "gallery"
        }
        DcinsideState.updateGalleryType(galleryType)

        if (!isListRequest) return

        val viewMain = json.optJSONObject("view_main")
        val managerSkill = if (viewMain != null && viewMain.has("managerskill")) {
            viewMain.optBoolean("managerskill")
        } else {
            galleryInfo.optBoolean("managerskill", false)
        }
        DcinsideState.updateGalleryManagerStatus(managerSkill)
    }

    private fun responseWithBody(
        response: Response,
        bytes: ByteArray,
        contentType: MediaType?,
    ): Response = response.newBuilder()
        .removeHeader("Transfer-Encoding")
        .removeHeader("Content-Length")
        .header("Content-Length", bytes.size.toString())
        .body(bytes.toResponseBody(contentType))
        .build()
}
