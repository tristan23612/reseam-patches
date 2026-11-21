package app.reseam.patches.dcinside.customInterceptor

import app.reseam.patch.Type
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.methods

internal val okHttpClientBuilder = methods("okHttpClientBuilder method") {
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    paramCount(0)
    returns(Type.Object)
    strings(
        "https://json2.dcinside.com/",
        "https://upload.dcinside.com/",
        "https://app.dcinside.com/"
    )
    calls { name("addInterceptor"); returns($$"Lokhttp3/OkHttpClient$Builder;") }
}.single()