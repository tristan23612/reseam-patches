package app.reseam.patches.dcinside.customInterceptor

import app.reseam.dcinside.customInterceptor.bindings.HideMustReadNoticeFilter
import app.reseam.dcinside.settings.DcinsideSettings
import app.reseam.dcinside.settings.DcinsideStrings
import app.reseam.patch.appEntry
import app.reseam.patch.patch
import app.reseam.patch.settings.section
import app.reseam.patches.dcinside.DcinsidePatchStrings
import app.reseam.patches.dcinside.core.DCINSIDE
import app.reseam.patches.dcinside.core.DcinsideSettingsPages
import app.reseam.patches.dcinside.core.dcinsideSettings

@Suppress("unused")
val hideMustReadNotice = patch(DcinsidePatchStrings.hideMustReadNotice) {
    description(DcinsidePatchStrings.hideMustReadNoticeDescription)
    compatibleWith(DCINSIDE)
    dependsOn(customInterceptor)
    settings(
        dcinsideSettings,
        section(
            DcinsideSettingsPages.General,
            DcinsideStrings.sectionGallery,
            DcinsideSettings.hideMustReadNotice,
        ),
    )

    execute {
        appEntry { HideMustReadNoticeFilter.register() }
    }
}