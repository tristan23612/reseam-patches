package app.reseam.patches.dcinside.postPum

import app.reseam.dcinside.settings.DcinsideSettings
import app.reseam.dcinside.settings.DcinsideStrings
import app.reseam.patch.Type
import app.reseam.patch.patch
import app.reseam.patch.points
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.dcinside.DcinsidePatchStrings
import app.reseam.patches.dcinside.core.DCINSIDE
import app.reseam.patches.dcinside.core.DcinsideSettingsPages
import app.reseam.patches.dcinside.core.dcinsideSettings

@Suppress("unused")
val disablePostPum = patch(DcinsidePatchStrings.disablePostPum) {
    description(DcinsidePatchStrings.disablePostPumDescription)
    compatibleWith(DCINSIDE)
    settings(dcinsideSettings, section(DcinsideSettingsPages.General, DcinsideStrings.sectionWrite, DcinsideSettings.disablePostPum))

    execute {
        gate(DcinsideSettings.disablePostPum) {
            postPum.points("postPum default point") {
                invokeInterface {
                    name("getBoolean")
                }
            }.single()
                .next { resultOf(Type.Boolean) }
                .captureAs("default boolean", Type.Boolean)
                .after {
                    capture("default boolean").assign(bool(true))
                }
        }
    }
}