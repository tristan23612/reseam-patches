package app.reseam.dcinside.settings

import app.reseam.patch.settings.Choice
import app.reseam.patch.settings.SettingsGroup
import app.reseam.patch.settings.TakesEffect
import app.reseam.patch.settings.Vector
import app.reseam.patch.settings.action
import app.reseam.patch.settings.choice
import app.reseam.patch.settings.folder
import app.reseam.patch.settings.text
import app.reseam.patch.settings.toggle

object DcinsideSettings : SettingsGroup(DcinsideStrings) {
    val hideAds by toggle(default = true)

    val hideBestPickItem by toggle(default = false)
    val hideRecommendedGalleries by toggle(default = false)
    val hideCrowd by toggle(default = false)
    val hideNewGalleries by toggle(default = false)
    val hideRecent by toggle(default = false)
    val hideAdminNotice by toggle(default = false)
    val hideMustReadNotice by toggle(default = false)
    val disablePostPum by toggle(default = false)
}
