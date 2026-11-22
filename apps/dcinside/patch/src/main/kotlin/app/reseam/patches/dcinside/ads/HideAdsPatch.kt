package app.reseam.patches.dcinside.ads

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
val hideAds = patch(DcinsidePatchStrings.hideAds) {
    description(DcinsidePatchStrings.hideAdsDescription)
    compatibleWith(DCINSIDE)
    settings(dcinsideSettings, section(DcinsideSettingsPages.Ads, DcinsideStrings.sectionAds, DcinsideSettings.hideAds))

    execute {
        gate(DcinsideSettings.hideAds) {
            advertisementList.before {
                returnValue(
                    callStatic(
                        "java.util.Collections",
                        "emptyList",
                        "()Ljava/util/List;",
                    )
                )
            }

            val adMainSmallNative = resources.id("dimen", "ad_main_small_native")?.toLong() ?: error("Could not find dimen/ad_main_small_native")
            val adMinimum = resources.id("dimen", "ad_minimum")?.toLong() ?: error("Could not find dimen/ad_minimum")

            getMinimumAdHeight(adMainSmallNative, adMinimum).apply {
                alwaysReturn(0)
            }

            val readAdMinimum = resources.id("dimen", "read_ad_minimum")?.toLong() ?: error("Could not find dimen/read_ad_minimum")
            footerAd(readAdMinimum).apply {
                alwaysReturn()
            }
        }
    }
}