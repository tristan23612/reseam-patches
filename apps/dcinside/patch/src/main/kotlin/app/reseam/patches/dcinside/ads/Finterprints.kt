package app.reseam.patches.dcinside.ads

import app.reseam.patch.Type
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.klass
import app.reseam.patch.methods

internal val advertisementList = klass("advertisementList class") {
    strings("forceRemoteConfig", "warning_adult_config")
}.methods("advertisementList method") {
    flags(AccessFlags.PUBLIC or AccessFlags.STATIC)
    params(Type.String)
    returns(Type.List)
}.single()