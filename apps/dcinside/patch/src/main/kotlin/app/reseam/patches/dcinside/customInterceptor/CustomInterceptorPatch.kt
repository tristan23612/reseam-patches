package app.reseam.patches.dcinside.customInterceptor

import app.reseam.dcinside.customInterceptor.bindings.CustomApplicationInterceptor
import app.reseam.patch.before
import app.reseam.patch.patch
import app.reseam.patches.dcinside.core.DCINSIDE

@Suppress("unused")
val customInterceptor = patch {
    compatibleWith(DCINSIDE)

    execute {
        okHttpClientBuilder.before {
                CustomApplicationInterceptor.register(thisObject)
            }
    }
}
