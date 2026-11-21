package app.reseam.patches.dcinside.customInterceptor

import app.reseam.dcinside.customInterceptor.bindings.CustomApplicationInterceptor
import app.reseam.patch.after
import app.reseam.patch.patch
import app.reseam.patch.points
import app.reseam.patches.dcinside.core.DCINSIDE

@Suppress("unused")
val customInterceptor = patch {
    compatibleWith(DCINSIDE)

    execute {
        okHttpClientBuilder.points("addIntercept point") {
            invoke {
                name("addInterceptor")
                returns($$"Lokhttp3/OkHttpClient$Builder;")
            }
        }.single()
            .next { resultOf($$"Lokhttp3/OkHttpClient$Builder;") }
            .captureAs("builder", $$"Lokhttp3/OkHttpClient$Builder;")
            .after {
                CustomApplicationInterceptor.register(capture("builder"))
            }
    }
}
