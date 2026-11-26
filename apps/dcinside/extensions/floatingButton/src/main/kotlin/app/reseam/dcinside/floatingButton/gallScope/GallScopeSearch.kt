package app.reseam.dcinside.floatingButton.gallScope

import android.net.Uri
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException
import java.util.concurrent.TimeUnit

internal data class GallScopeResult(
    val type: Type,
    val postNo: String,
    val title: String,
    val url: String,
    val nickname: String,
    val date: String,
    val replyCount: String = "",
    val views: String = "",
    val recommend: String = "",
    val fcno: String = "",
) {
    enum class Type { POST, COMMENT }
}

internal data class SearchBatch(
    val results: List<GallScopeResult>,
    val lastPage: Int,
    val rangeExceeded: Boolean,
)

/** Reuses comment cursor state between batches, matching the legacy gallScope behavior. */
internal class GallScopeSearch {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private var commentRequestPage = 1
    private var commentAccumulatedPageCount = 0
    private var commentSearchPos = ""
    private var previousCommentDocument: Document? = null

    fun resetComments() {
        commentRequestPage = 1
        commentAccumulatedPageCount = 0
        commentSearchPos = ""
        previousCommentDocument = null
    }

    @Throws(IOException::class)
    fun searchPosts(
        galleryType: String,
        galleryId: String,
        userId: String,
        startPage: Int,
        endPage: Int,
        onProgress: (Int, Int) -> Unit,
    ): SearchBatch {
        val results = mutableListOf<GallScopeResult>()
        val total = endPage - startPage + 1
        var lastValidPage = startPage - 1
        var exceeded = false

        for (page in startPage..endPage) {
            val document = fetch(buildListUrl(galleryType, galleryId, page, comments = false))
            val actualPage = document.selectFirst("div.bottom_paging_box em")?.text()?.trim()?.toIntOrNull()
            if (actualPage != null && actualPage != page) {
                exceeded = true
                break
            }

            lastValidPage = page
            results += parsePosts(document, userId)
            onProgress(page - startPage + 1, total)
        }

        return SearchBatch(results.sortedByDescending { it.postNo.toLongOrNull() ?: 0L }, lastValidPage, exceeded)
    }

    @Throws(IOException::class)
    fun searchComments(
        galleryType: String,
        galleryId: String,
        userId: String,
        pageLimit: Int,
        onProgress: (Int, Int) -> Unit,
    ): SearchBatch {
        val results = mutableListOf<GallScopeResult>()
        val startingCount = commentAccumulatedPageCount
        val targetCount = startingCount + pageLimit
        var exceeded = false

        while (commentAccumulatedPageCount < targetCount) {
            val document = fetch(buildListUrl(
                galleryType = galleryType,
                galleryId = galleryId,
                page = commentRequestPage,
                comments = true,
                searchPos = commentSearchPos,
            ))
            val actualPage = document.location().let { Uri.parse(it).getQueryParameter("page")?.toIntOrNull() } ?: commentRequestPage
            if (actualPage != commentRequestPage) {
                val nextSearchPos = extractNextSearchPosition(previousCommentDocument)
                if (nextSearchPos == null) {
                    exceeded = true
                    break
                }
                commentSearchPos = nextSearchPos
                commentRequestPage = 1
                commentAccumulatedPageCount++
                onProgress(commentAccumulatedPageCount - startingCount, pageLimit)
                Thread.sleep(500)
                continue
            }

            val rows = document.select(".listwrap2 .search.search_comment")
            if (rows.isEmpty()) {
                exceeded = true
                break
            }

            results += parseComments(document, userId)
            previousCommentDocument = document
            commentRequestPage++
            commentAccumulatedPageCount++
            onProgress(commentAccumulatedPageCount - startingCount, pageLimit)
            if (commentAccumulatedPageCount < targetCount) Thread.sleep(100)
        }

        return SearchBatch(results, commentAccumulatedPageCount, exceeded)
    }

    private fun fetch(url: HttpUrl): Document {
        val request = Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", PC_USER_AGENT)
            .apply { if (url.queryParameter("s_type") == "search_comment") header("X-Requested-With", "XMLHttpRequest") }
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body ?: throw IOException("검색 응답이 비어 있습니다.")
            return Jsoup.parse(body.string(), response.request.url.toString())
        }
    }

    private fun buildListUrl(
        galleryType: String,
        galleryId: String,
        page: Int,
        comments: Boolean,
        searchPos: String = "",
    ): HttpUrl {
        val builder = HttpUrl.Builder()
            .scheme("https")
            .host("gall.dcinside.com")
        if (!galleryType.equals("gallery", ignoreCase = true)) builder.addPathSegment(galleryType)
        builder.addPathSegment("board").addPathSegment("lists")
            .addQueryParameter("id", galleryId.removePrefix("mi\$"))
            .addQueryParameter("page", page.toString())
        if (comments) {
            builder.addQueryParameter("s_type", "search_comment")
                .addQueryParameter("s_keyword", "%20")
            if (searchPos.isNotEmpty()) builder.addQueryParameter("search_pos", searchPos)
        }
        return builder.build()
    }

    private fun parsePosts(document: Document, targetUserId: String): List<GallScopeResult> =
        document.select("tr.ub-content.us-post").mapNotNull { row ->
            val writer = row.selectFirst("td.gall_writer") ?: return@mapNotNull null
            val userId = writer.attr("data-uid").trim()
            val ip = writer.attr("data-ip").trim()
            if ((userId.ifEmpty { ip }).isEmpty() || (userId.ifEmpty { ip }) != targetUserId) return@mapNotNull null

            val titleLink = row.selectFirst("td.gall_tit.ub-word > a")
            GallScopeResult(
                type = GallScopeResult.Type.POST,
                postNo = row.textOf("td.gall_num"),
                title = titleLink?.ownText()?.trim().orEmpty(),
                url = titleLink?.attr("href")?.let(::toMobileUrl).orEmpty(),
                nickname = writer.attr("data-nick").trim(),
                date = row.selectFirst("td.gall_date")?.attr("title")?.trim().orEmpty(),
                replyCount = Regex("\\[(\\d+)]").find(row.textOf("a.reply_numbox"))?.groupValues?.get(1) ?: "0",
                views = row.textOf("td.gall_count"),
                recommend = row.textOf("td.gall_recommend"),
            )
        }

    private fun parseComments(document: Document, targetUserId: String): List<GallScopeResult> =
        document.select(".listwrap2 .search.search_comment").mapNotNull { row ->
            val writer = row.selectFirst("td.gall_writer, .gall_writer") ?: return@mapNotNull null
            val userId = writer.attr("data-uid").trim()
            val ip = writer.attr("data-ip").trim()
            if ((userId.ifEmpty { ip }).isEmpty() || (userId.ifEmpty { ip }) != targetUserId) return@mapNotNull null

            val link = row.selectFirst("div.sch_cmt a")
            val href = link?.attr("href").orEmpty()
            val uri = Uri.parse(absoluteUrl(href))
            GallScopeResult(
                type = GallScopeResult.Type.COMMENT,
                postNo = uri.getQueryParameter("no").orEmpty(),
                title = link?.text()?.trim().orEmpty(),
                url = toMobileUrl(href),
                nickname = writer.attr("data-nick").trim(),
                date = row.textOf("td.gall_date"),
                fcno = uri.getQueryParameter("fcno").orEmpty(),
            )
        }

    private fun extractNextSearchPosition(document: Document?): String? {
        val href = document?.selectFirst("div.bottom_paging_wrap a.search_next")?.attr("href") ?: return null
        return Regex("[?&]search_pos=(\\d+)").find(href)?.groupValues?.get(1)
    }

    private fun Element.textOf(selector: String): String = selectFirst(selector)?.text()?.trim().orEmpty()

    private fun absoluteUrl(href: String): String = when {
        href.isEmpty() -> ""
        href.startsWith("http://") || href.startsWith("https://") -> href
        href.startsWith("/") -> "https://gall.dcinside.com$href"
        else -> "https://gall.dcinside.com/$href"
    }

    private fun toMobileUrl(href: String): String {
        val absolute = absoluteUrl(href)
        if (absolute.isEmpty()) return ""
        val uri = Uri.parse(absolute)
        if (!uri.path.orEmpty().contains("/board/view/")) return absolute
        val gallery = uri.getQueryParameter("id") ?: return absolute
        val postNo = uri.getQueryParameter("no") ?: return absolute
        val segment = if (uri.path.orEmpty().contains("/mini/")) "mini" else "board"
        return "https://m.dcinside.com/$segment/$gallery/$postNo"
    }

    private companion object {
        const val PC_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    }
}
