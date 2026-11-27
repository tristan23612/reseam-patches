package app.reseam.patches.dcinside.manager.floatingButton

import app.reseam.dcinside.floatingButton.gallScope.bindings.GallScope
import app.reseam.dcinside.settings.DcinsideSettings
import app.reseam.dcinside.settings.DcinsideStrings
import app.reseam.patch.*
import app.reseam.patch.settings.section
import app.reseam.patches.dcinside.DcinsidePatchStrings
import app.reseam.patches.dcinside.core.DCINSIDE
import app.reseam.patches.dcinside.core.DcinsideSettingsPages
import app.reseam.patches.dcinside.core.dcinsideSettings

@Suppress("unused")
val gallScope = patch(DcinsidePatchStrings.gallScope) {
    description(DcinsidePatchStrings.gallScopeDescription)
    compatibleWith(DCINSIDE)
    dependsOn(floatingButton)
    settings(
        dcinsideSettings,
        section(
            DcinsideSettingsPages.GallScope,
            DcinsideStrings.sectionGallScopeButton,
            DcinsideSettings.showGallScope,
        )
    )

    execute {
        appEntry {
            GallScope.showGallScope(string(DcinsideSettings.showGallScope.key), bool(DcinsideSettings.showGallScope.default))
        }

        addFloatingButtonResource("gall_scope")
    }
}
