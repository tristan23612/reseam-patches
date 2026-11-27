package app.reseam.patches.dcinside.postBottom

import app.reseam.dcinside.settings.DcinsideSettings
import app.reseam.dcinside.settings.DcinsideStrings
import app.reseam.patch.patch
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.dcinside.DcinsidePatchStrings
import app.reseam.patches.dcinside.core.DCINSIDE
import app.reseam.patches.dcinside.core.DcinsideSettingsPages
import app.reseam.patches.dcinside.core.dcinsideSettings

@Suppress("unused")
val hidePostBottomLikeView = patch(DcinsidePatchStrings.hidePostBottomLikeView) {
    description(DcinsidePatchStrings.hidePostBottomLikeViewDescription)
    compatibleWith(DCINSIDE)
    settings(dcinsideSettings, section(DcinsideSettingsPages.General, DcinsideStrings.sectionPost, DcinsideSettings.hidePostBottomLikeView))

    execute {
        gate(DcinsideSettings.hidePostBottomLikeView) {
            postBottomLikeView.alwaysReturn()
        }
    }
}