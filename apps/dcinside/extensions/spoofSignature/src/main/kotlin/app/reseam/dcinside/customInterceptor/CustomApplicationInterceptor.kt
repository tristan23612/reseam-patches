@file:JvmName("CustomApplicationInterceptor")

package app.reseam.dcinside.customInterceptor

import app.reseam.runtime.Export
import okhttp3.Interceptor
import okhttp3.Response

/** Registers the extension-owned interceptor on the app's shared OkHttp builder. */
@Export
fun register(builder: okhttp3.OkHttpClient.Builder) {
    builder.addInterceptor(DcinsideInterceptor())
}

private class DcinsideInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(chain.request())
}
