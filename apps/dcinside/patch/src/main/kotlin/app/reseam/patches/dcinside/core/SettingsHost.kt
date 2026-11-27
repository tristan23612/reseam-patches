package app.reseam.patches.dcinside.core

import app.reseam.patch.settings.settingsHost

val dcinsideSettings = settingsHost("dcinside") {
    compatibleWith(DCINSIDE)
}