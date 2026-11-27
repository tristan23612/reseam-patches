package app.reseam.dcinside.customInterceptor

import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CopyOnWriteArrayList

object JsonResponseFilters {
    private val filters = CopyOnWriteArrayList<JsonResponseFilter>()

    @Synchronized
    fun register(filter: JsonResponseFilter) {
        require(filter.id.isNotBlank()) { "A JSON response filter must have an id" }
        if (filters.none { it.id == filter.id }) filters += filter
    }

    internal fun apply(context: JsonResponseContext, response: JSONArray): JSONArray {
        val orderedFilters = filters.sortedWith(compareBy<JsonResponseFilter> { it.order }.thenBy { it.id })
        val result = JSONArray()

        for (index in 0 until response.length()) {
            val original = response.opt(index)
            if (original !is JSONObject) {
                result.put(original)
                continue
            }

            var current = original
            for (filter in orderedFilters) {
                if (filter.isEnabled() && filter.matches(context)) {
                    current = filter.apply(context, current as JSONObject)
                }
            }
            result.put(current)
        }

        return result
    }
}
