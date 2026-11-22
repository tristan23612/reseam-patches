package app.reseam.patches.dcinside.core

import app.reseam.dcinside.spoofsignature.bindings.SpoofSignature
import app.reseam.patch.appEntry
import app.reseam.patch.patch
import app.reseam.patches.dcinside.DcinsidePatchStrings

@Suppress("unused")
val spoofSignature = patch(DcinsidePatchStrings.spoofSignature) {
    compatibleWith(DCINSIDE)

    execute {
        appEntry { SpoofSignature.install() }
    }
}