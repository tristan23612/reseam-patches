package app.reseam.dcinside.floatingButton.banList

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

internal object BanListHtmlParser {
    private val pageParameter = Regex("[?&]p=(\\d+)(?:&|$)")

    fun hasRows(document: Document) = document.select("tbody tr .blocknik").isNotEmpty()

    fun parsePage(document: Document): List<JSONObject> =
        document.select("tbody tr")
            .filter { it.selectFirst(".blocknik") != null }
            .map(::parseRow)

    fun extractLastPage(document: Document, currentPage: Int): Int {
        val lastPageHref = document.selectFirst(".bottom_paging_box a.page_end")?.attr("href")
        lastPageHref?.let(::pageFromHref)?.takeIf { it > 0 }?.let { return maxOf(currentPage, it) }
        return document.select(".bottom_paging_box a[href]")
            .mapNotNull { pageFromHref(it.attr("href")).takeIf { page -> page > 0 } }
            .maxOrNull()
            ?.coerceAtLeast(currentPage)
            ?: currentPage
    }

    @Throws(JSONException::class)
    private fun parseRow(row: Element): JSONObject {
        val nicknameCell = row.selectFirst(".blocknik")
        var nickname = ""
        var identifierText = ""
        var nonEmptyIndex = 0
        nicknameCell?.select("p")?.forEach { paragraph ->
            val value = clean(paragraph.text())
            if (value.isNotEmpty()) {
                if (nonEmptyIndex++ == 0) nickname = value else if (nonEmptyIndex == 2) identifierText = value
            }
        }
        identifierText = identifierText.replace(Regex("(?i)\\s*\\+\\s*IP\\s*$"), "").trim()
        var identifier = identifierText.removePrefix("(").removeSuffix(")").trim()
        if (nicknameCell?.selectFirst(".txtip") != null) identifier += " + IP"

        val contentCell = row.selectFirst(".blockcontent")
        val contentType = clean(contentCell?.selectFirst("em")?.text().orEmpty())
        val contentTitle = clean(contentCell?.selectFirst("a")?.text().orEmpty())
        val content = when {
            contentType == "직접 차단" -> contentType
            contentType.isNotEmpty() && contentTitle.isNotEmpty() -> "[$contentType] $contentTitle"
            contentTitle.isNotEmpty() -> contentTitle
            else -> contentType
        }

        val dateCell = row.selectFirst(".blockday")
        val date = clean(dateCell?.selectFirst(".block_date")?.text().orEmpty())
        val time = clean(dateCell?.selectFirst(".block_time")?.text().orEmpty())
            .replace(Regex("^처리\\s*시간\\s*:\\s*"), "")
        val manager = clean(dateCell?.selectFirst(".block_conduct")?.text().orEmpty())
            .replace(Regex("^처리자\\s*:\\s*"), "")

        return JSONObject()
            .put("nickname", nickname)
            .put("identifier", identifier)
            .put("content", content)
            .put("reason", clean(row.selectFirst(".blockreason")?.text().orEmpty()))
            .put("duration", clean(row.selectFirst(".blocktime")?.text().orEmpty()))
            .put("dateTime", listOf(date, time).filter(String::isNotEmpty).joinToString(" "))
            .put("manager", manager)
    }

    private fun pageFromHref(href: String): Int =
        pageParameter.find(href)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0

    private fun clean(value: String) = value.replace(Regex("\\s+"), " ").trim()
}

internal fun List<JSONObject>.toJsonArray() = JSONArray().also { array -> forEach(array::put) }
