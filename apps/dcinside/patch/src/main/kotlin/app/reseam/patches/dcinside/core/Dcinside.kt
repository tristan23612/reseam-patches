package app.reseam.patches.dcinside.core

import app.reseam.patch.CompatiblePackage
import app.reseam.patch.*

const val DCINSIDE_PACKAGE = "com.dcinside.app.android"

val DCINSIDE: CompatiblePackage = DCINSIDE_PACKAGE("5.3.25")

val mainActivityOnCreate = klass(DCINSIDE_PACKAGE).method("onCreate") {
    params("android.os.Bundle")
    returns(Type.Void)
}