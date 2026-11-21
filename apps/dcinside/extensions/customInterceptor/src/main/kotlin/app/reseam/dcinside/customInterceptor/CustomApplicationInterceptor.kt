@file:JvmName("CustomApplicationInterceptor")

package app.reseam.dcinside.customInterceptor

import app.reseam.runtime.Export
import okhttp3.Interceptor
import okhttp3.Response

private val URL_FILTER_KEYWORD_LIST = mutableListOf<String?>(
    "gall_list_new",
    "gall_view_new"
)

@Export
fun register(builder: okhttp3.OkHttpClient.Builder?) {
    if (builder == null || builder.interceptors().any { it is DcinsideInterceptor }) return
    builder.addInterceptor(DcinsideInterceptor())
}

private class DcinsideInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.proceed(chain.request())
    }
}
