package app.reseam.patches.dcinside.customInterceptor

import app.reseam.patch.Type
import app.reseam.patch.className
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods

internal val okHttpClientBuilder = klass($$"okhttp3.OkHttpClient$Builder").method("build")