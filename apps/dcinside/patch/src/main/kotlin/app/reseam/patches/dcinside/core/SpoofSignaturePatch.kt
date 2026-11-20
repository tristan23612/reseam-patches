package app.reseam.patches.dcinside.core

import app.example.patches.DcinsidePatchStrings
import app.reseam.dcinside.spoofsignature.bindings.SpoofSignature
import app.reseam.patch.appEntry
import app.reseam.patch.patch

@Suppress("unused")
val spoofSignature = patch(DcinsidePatchStrings.spoofSignature) {
    compatibleWith(DCINSIDE)

    execute {
        appEntry { SpoofSignature.install() }
    }
}