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
    val hidePostBottomLikeView by toggle(default = false)
    val showUserId by toggle(default = false)
    val enableUserIdDirectScope by toggle(default = false)
    val postLongPress by toggle(default = false)
    val showGallScope by toggle(default = false)
    val showBanList by toggle(default = false)
    val banListWorkerUrl by text(default = "")
    val enableBanListIdentifierSearch by toggle(default = false)
    val enableBanListIngest by toggle(default = false)
    val banListWorkerViewToken by text(default = "")
    val banListWorkerIngestToken by text(default = "")
    val galleriesJson by folder(default = "")
    val dcinsideUserId by text(default = "")
    val dcinsidePassword by text(default = "")
}
