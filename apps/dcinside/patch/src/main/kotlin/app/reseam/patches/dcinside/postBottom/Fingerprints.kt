package app.reseam.patches.dcinside.postBottom

import app.reseam.patch.Type
import app.reseam.patch.methods

internal val postBottomLikeView = methods("postBottomLikeView method") {
    returns(Type.Void)
    strings("vwTitle", "vwRecycler", "vwNextPage", "vwPrevPage")
}.single()