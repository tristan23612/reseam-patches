package app.reseam.shared.html

import android.text.Html

/** A small tolerant HTML document with the selector subset used by ReSeam extensions. */
class HtmlDocument private constructor(private val root: HtmlElement) {
    fun selectFirst(selector: String): HtmlElement? = root.selectFirst(selector)

    fun select(selector: String): List<HtmlElement> = root.select(selector)

    companion object {
        private val tokenPattern = Regex("(?s)<!--.*?-->|<![^>]*>|</?[^>]+>|[^<]+")
        private val tagPattern = Regex("^<\\s*(/?)\\s*([a-zA-Z][\\w:-]*)")
        private val attributePattern = Regex("([^\\s=/'\"><]+)(?:\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+)))?")
        private val voidTags = setOf(
            "area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr",
        )
        private val autoCloseTags = setOf("li", "option", "p", "td", "th", "tr")

        fun parse(html: String): HtmlDocument {
            val root = HtmlElement("#document")
            val stack = ArrayDeque<HtmlElement>().apply { addLast(root) }
            val source = html
                .replace(Regex("(?is)<script\\b[^>]*>.*?</script\\s*>"), " ")
                .replace(Regex("(?is)<style\\b[^>]*>.*?</style\\s*>"), " ")

            tokenPattern.findAll(source).forEach { match ->
                val token = match.value
                if (!token.startsWith('<')) {
                    stack.last().append(token)
                    return@forEach
                }
                if (token.startsWith("<!--") || token.startsWith("<!")) return@forEach

                val tagMatch = tagPattern.find(token) ?: return@forEach
                val isClosing = tagMatch.groupValues[1].isNotEmpty()
                val tag = tagMatch.groupValues[2].lowercase()
                if (isClosing) {
                    closeThrough(stack, tag)
                    return@forEach
                }

                if (tag in autoCloseTags && stack.last().tag == tag) stack.removeLast()
                val attributes = attributePattern.findAll(token.substring(tagMatch.range.last + 1))
                    .associate { attribute ->
                        attribute.groupValues[1].lowercase() to
                            (attribute.groups[2]?.value ?: attribute.groups[3]?.value ?: attribute.groups[4]?.value.orEmpty())
                    }
                val element = HtmlElement(tag, attributes)
                stack.last().append(element)
                if (tag !in voidTags && !token.trimEnd().endsWith("/>") && stack.size < 256) stack.addLast(element)
            }
            return HtmlDocument(root)
        }

        private fun closeThrough(stack: ArrayDeque<HtmlElement>, tag: String) {
            while (stack.size > 1) {
                if (stack.removeLast().tag == tag) break
            }
        }
    }
}

/** DOM-like element with tag, class, descendant and direct-child selector support. */
class HtmlElement internal constructor(
    val tag: String,
    private val attributes: Map<String, String> = emptyMap(),
) {
    private val content = mutableListOf<Any>()

    val children: List<HtmlElement>
        get() = content.filterIsInstance<HtmlElement>()

    fun attr(name: String): String = attributes[name.lowercase()]?.let(::decodeHtml).orEmpty()

    fun hasClass(name: String): Boolean = attr("class").split(Regex("\\s+")).contains(name)

    fun text(): String = normalizeText(content.joinToString(" ") { item ->
        when (item) {
            is HtmlElement -> item.text()
            is String -> decodeHtml(item)
            else -> ""
        }
    })

    fun ownText(): String = normalizeText(content.filterIsInstance<String>().joinToString("") { decodeHtml(it) })

    fun selectFirst(selector: String): HtmlElement? = select(selector).firstOrNull()

    fun select(selector: String): List<HtmlElement> = selector.split(',').flatMap { group ->
        val terms = Regex("[^\\s>]+")
        val matches = terms.findAll(group).toList()
        if (matches.isEmpty()) return@flatMap emptyList()

        var current = descendants().filter { it.matches(matches.first().value) }.toList()
        for (index in 1 until matches.size) {
            val relation = group.substring(matches[index - 1].range.last + 1, matches[index].range.first)
            val directChild = relation.contains('>')
            current = current.asSequence()
                .flatMap { element -> if (directChild) element.children.asSequence() else element.descendants() }
                .filter { it.matches(matches[index].value) }
                .distinct()
                .toList()
        }
        current
    }.distinct()

    internal fun append(value: Any) {
        content += value
    }

    private fun descendants(): Sequence<HtmlElement> =
        children.asSequence().flatMap { sequenceOf(it) + it.descendants() }

    private fun matches(selector: String): Boolean {
        val tagName = Regex("^[a-zA-Z][\\w:-]*").find(selector)?.value
        if (tagName != null && tag != tagName.lowercase()) return false
        val classes = Regex("\\.([\\w-]+)").findAll(selector).map { it.groupValues[1] }
        return classes.all(::hasClass)
    }

    companion object {
        private fun normalizeText(text: String): String = text.replace(Regex("\\s+"), " ").trim()

        @Suppress("DEPRECATION")
        private fun decodeHtml(text: String): String = Html.fromHtml(text).toString()
    }
}

