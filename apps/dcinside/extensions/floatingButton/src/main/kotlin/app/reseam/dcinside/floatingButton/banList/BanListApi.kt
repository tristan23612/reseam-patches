package app.reseam.dcinside.floatingButton.banList

import app.reseam.shared.html.HtmlDocument
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

internal data class IdentifierSearchResponse(
    val headers: List<String>,
    val rows: List<JSONObject>,
)

internal object BanListApi {
    private val workerClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private const val SIGN_ORIGIN = "https://sign.dcinside.com"
    private const val REDIRECT_URL = "https://www.dcinside.com/"
    private const val USER_AGENT = "Mozilla/5.0"

    fun getLatestData(galleryId: String): JSONObject? {
        val response = workerPost(
            Token.VIEW,
            JSONObject().put("action", "getLatestData").put("galleryId", galleryId),
        )
        requireSuccess(response)
        return response.optJSONObject("latestData")
    }

    fun searchIdentifier(galleryId: String, identifier: String): IdentifierSearchResponse {
        val response = workerPost(
            Token.VIEW,
            JSONObject().put("action", "searchIdentifier")
                .put("galleryId", galleryId)
                .put("identifier", identifier),
        )
        requireSuccess(response)
        val headerArray = response.optJSONArray("headers")
            ?: throw IOException("Worker 응답에 headers가 없습니다.")
        val rowArray = response.optJSONArray("rows")
            ?: throw IOException("Worker 응답에 rows가 없습니다.")
        return IdentifierSearchResponse(
            headers = List(headerArray.length()) { headerArray.optString(it) },
            rows = List(rowArray.length()) { rowArray.optJSONObject(it) ?: JSONObject() },
        )
    }

    fun ingest(galleryId: String, records: JSONArray): JSONObject = workerPost(
        Token.INGEST,
        JSONObject()
            .put("action", "ingest")
            .put("galleryId", galleryId)
            .put("banList", records)
            .put("updateRefreshStatus", true),
        refreshSnapshot = true,
    ).also(::requireSuccess)

    @Throws(IOException::class, JSONException::class)
    fun resolveGalleryPath(galleriesJson: String, galleryId: String): String {
        if (galleryId.isBlank()) throw JSONException("현재 갤러리 ID를 확인할 수 없습니다.")
        val galleries = JSONArray(galleriesJson)
        for (index in 0 until galleries.length()) {
            val gallery = galleries.optJSONObject(index) ?: continue
            if (galleryId != gallery.optString("id").trim()) continue
            val path = gallery.optString("path").trim()
            if (path in setOf("mgallery", "gallery", "mini")) return path
            throw JSONException("갤러리 $galleryId 의 path는 mgallery, gallery, mini 중 하나여야 합니다.")
        }
        throw JSONException("GALLERIES_JSON에 현재 갤러리 $galleryId 설정이 없습니다.")
    }

    fun collectBanList(galleryId: String, latestData: JSONObject?): JSONArray {
        val path = resolveGalleryPath(BanListSettings.galleriesJsonValue, galleryId)
        val session = login(BanListSettings.dcinsideUserIdValue, BanListSettings.dcinsidePasswordValue)
        val firstPage = fetchPage(session, path, galleryId, 1)
        var lastPage = BanListHtmlParser.extractLastPage(firstPage, 1)
        val collected = mutableListOf<JSONObject>()
        if (appendUntilDuplicate(collected, BanListHtmlParser.parsePage(firstPage), latestData)) {
            return collected.toJsonArray()
        }

        var page = 2
        while (page <= lastPage) {
            val document = fetchPage(session, path, galleryId, page)
            lastPage = maxOf(lastPage, BanListHtmlParser.extractLastPage(document, page))
            if (appendUntilDuplicate(collected, BanListHtmlParser.parsePage(document), latestData)) break
            page++
        }
        return collected.toJsonArray()
    }

    private fun workerPost(token: Token, payload: JSONObject, refreshSnapshot: Boolean = false): JSONObject {
        val url = BanListSettings.banListWorkerUrlValue.trim()
        if (url.isEmpty()) throw IOException("Cloudflare Worker URL이 설정되지 않았습니다.")
        val tokenValue = when (token) {
            Token.VIEW -> BanListSettings.banListWorkerViewTokenValue
            Token.INGEST -> BanListSettings.banListWorkerIngestTokenValue
        }
        if (tokenValue.isEmpty()) throw IOException("Cloudflare Worker 토큰이 설정되지 않았습니다.")

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $tokenValue")
            .header("Accept", "application/json")
            .apply { if (refreshSnapshot) header("X-Refresh-Snapshot", "true") }
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        workerClient.newCall(request).execute().use { response ->
            val body = response.body.string()
            if (!response.isSuccessful) {
                val message = runCatching { JSONObject(body).optString("message") }.getOrNull()
                throw IOException(message?.takeIf(String::isNotBlank) ?: "Cloudflare Worker 요청 실패 (HTTP ${response.code})")
            }
            return try {
                JSONObject(body)
            } catch (error: JSONException) {
                throw IOException("Cloudflare Worker 응답 JSON을 읽지 못했습니다.", error)
            }
        }
    }

    private fun requireSuccess(response: JSONObject) {
        if (response.optString("status") != "success") {
            throw IOException(response.optString("message").ifBlank { "Cloudflare Worker 요청이 실패했습니다." })
        }
    }

    private fun login(userId: String, password: String): AuthenticatedSession {
        if (userId.isBlank() || password.isEmpty()) throw IOException("DCInside 계정 정보가 설정되지 않았습니다.")
        val cookieJar = MemoryCookieJar()
        val client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        val loginPageUrl = HttpUrl.Builder()
            .scheme("https")
            .host("sign.dcinside.com")
            .addPathSegment("login")
            .addQueryParameter("s_url", REDIRECT_URL)
            .build()
        val loginHtml = executeText(client, Request.Builder().url(loginPageUrl)
            .header("Accept", "text/html,application/xhtml+xml")
            .header("User-Agent", USER_AGENT)
            .get().build(), "DCInside 로그인 페이지 요청")
        val ciToken = inputValue(loginHtml, "ci_t")
            ?: throw IOException("로그인 페이지에서 ci_t 값을 찾지 못했습니다.")

        val form = FormBody.Builder()
            .add("ci_t", ciToken)
            .add("s_url", REDIRECT_URL)
            .add("tieup", "")
            .add("url", "")
            .add("ssl", "Y")
            .add("user_id", userId)
            .add("pw", password)
            .build()
        val responseHtml = executeText(client, Request.Builder()
            .url("$SIGN_ORIGIN/login/member_check")
            .header("Accept", "text/html,application/xhtml+xml")
            .header("Origin", SIGN_ORIGIN)
            .header("Referer", loginPageUrl.toString())
            .header("User-Agent", USER_AGENT)
            .post(form)
            .build(), "DCInside 로그인")
        val loginFormStillVisible = inputTag(responseHtml, "user_id") != null || inputTag(responseHtml, "pw") != null
        if (!cookieJar.hasSessionCookie() && loginFormStillVisible) {
            throw IOException("DCInside 로그인이 완료되지 않았습니다. 계정 또는 추가 인증을 확인하세요.")
        }
        return AuthenticatedSession(client)
    }

    private fun fetchPage(session: AuthenticatedSession, path: String, galleryId: String, page: Int): HtmlDocument {
        val url = HttpUrl.Builder()
            .scheme("https")
            .host("gall.dcinside.com")
            .addPathSegment(path)
            .addPathSegment("management")
            .addPathSegment("block")
            .addQueryParameter("id", galleryId)
            .addQueryParameter("p", page.toString())
            .build()
        val referer = url.newBuilder().setQueryParameter("p", maxOf(1, page - 1).toString()).build()
        val html = executeText(session.client, Request.Builder().url(url)
            .header("Accept", "text/html,application/xhtml+xml")
            .header("Referer", referer.toString())
            .header("User-Agent", USER_AGENT)
            .get().build(), "차단 목록 ${page}페이지 요청")
        if (html.length > MAX_PAGE_LENGTH) throw IOException("차단 목록 ${page}페이지 응답이 너무 큽니다.")
        val document = HtmlDocument.parse(html)
        if (!BanListHtmlParser.hasRows(document)) {
            if (inputTag(html, "user_id") != null || html.contains("sign.dcinside.com/login", ignoreCase = true)) {
                throw IOException("DCInside 로그인 세션이 유지되지 않았습니다.")
            }
            throw IOException("차단 목록 ${page}페이지에서 목록 행을 찾지 못했습니다.")
        }
        return document
    }

    private fun executeText(client: OkHttpClient, request: Request, operation: String): String =
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("$operation 실패 (HTTP ${response.code})")
            response.body.string()
        }

    private fun inputValue(html: String, name: String): String? {
        val tag = inputTag(html, name) ?: return null
        return Regex("\\bvalue\\s*=\\s*[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE)
            .find(tag)?.groupValues?.getOrNull(1)
    }

    private fun inputTag(html: String, name: String): String? =
        Regex("<input\\b[^>]*\\bname\\s*=\\s*[\"']${Regex.escape(name)}[\"'][^>]*>", RegexOption.IGNORE_CASE)
            .find(html)?.value

    private fun appendUntilDuplicate(
        output: MutableList<JSONObject>,
        pageRecords: List<JSONObject>,
        latestData: JSONObject?,
    ): Boolean {
        for (record in pageRecords) {
            if (latestData != null && sameRecord(record, latestData)) return true
            output += record
        }
        return false
    }

    private fun sameRecord(a: JSONObject, b: JSONObject): Boolean =
        RECORD_KEYS.all { key -> a.optString(key) == b.optString(key) }

    private data class AuthenticatedSession(val client: OkHttpClient)

    private enum class Token { VIEW, INGEST }

    private class MemoryCookieJar : CookieJar {
        private val cookies = mutableMapOf<String, Cookie>()

        @Synchronized
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookies.forEach { cookie ->
                val key = "${cookie.domain}|${cookie.path}|${cookie.name}"
                if (cookie.expiresAt <= System.currentTimeMillis()) this@MemoryCookieJar.cookies.remove(key) else this@MemoryCookieJar.cookies[key] = cookie
            }
        }

        @Synchronized
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val now = System.currentTimeMillis()
            cookies.entries.removeAll { it.value.expiresAt <= now }
            return cookies.values.filter { it.matches(url) }
        }

        @Synchronized
        fun hasSessionCookie() = cookies.values.any {
            it.name.matches(Regex("(?i).*(sso_token|PHPSESSID|csid).*"))
        }
    }

    private const val MAX_PAGE_LENGTH = 2_000_000
    private val RECORD_KEYS = listOf("nickname", "identifier", "content", "reason", "duration", "dateTime", "manager")
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
}
