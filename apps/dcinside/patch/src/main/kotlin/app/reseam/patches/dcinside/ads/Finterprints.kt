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

internal fun getMinimumAdHeight(adMainSmallNative: Long, adMinimum: Long) = methods("getMinimumAdHeight method") {
    flags(AccessFlags.PUBLIC or AccessFlags.STATIC)
    params(Type.List)
    returns(Type.Int)
    literals(
        adMainSmallNative,
        adMinimum,
    )
}.single()

internal fun footerAd(readAdMinimum: Long) = methods("footerAd method") {
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    returns(Type.Void)
    literals(
        readAdMinimum,
    )
}.single()