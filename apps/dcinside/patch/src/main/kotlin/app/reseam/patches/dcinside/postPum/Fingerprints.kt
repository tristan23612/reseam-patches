package app.reseam.patches.dcinside.postPum

import app.reseam.patch.Type
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.methods

internal val postPum = methods("postPum method") {
    flags(AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR)
    name("<init>")
    returns(Type.Void)
    strings("writeConfigNotAllowedPum")
}.single()