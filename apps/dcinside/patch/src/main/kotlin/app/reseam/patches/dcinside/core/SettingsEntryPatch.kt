package app.reseam.patches.dcinside.core

import app.example.patches.DcinsidePatchStrings
import app.reseam.patch.patch

val settingsEntry = patch(DcinsidePatchStrings.settingsEntry) {
    compatibleWith(DCINSIDE)
    dependsOn(dcinsideSettings)
}