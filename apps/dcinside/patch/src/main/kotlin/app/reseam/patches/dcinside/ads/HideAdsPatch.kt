package app.reseam.patches.dcinside.ads

import app.reseam.dcinside.settings.DcinsideSettings
import app.reseam.dcinside.settings.DcinsideStrings
import app.reseam.patch.Type
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.klass
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
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
            klass("advertisementList class") {
                strings("forceRemoteConfig", "warning_adult_config")
            }.methods("advertisementList method") {
                flags(AccessFlags.PUBLIC or AccessFlags.STATIC)
                params(Type.String)
                returns(Type.List)
            }.single().before {
                returnValue(
                    callStatic(
                        "java.util.Collections",
                        "emptyList",
                        "()Ljava/util/List;",
                    )
                )
            }


            methods("getMinimumAdHeight method") {
                flags(AccessFlags.PUBLIC or AccessFlags.STATIC)
                params(Type.List)
                returns(Type.Int)
                literals(
                    resources.id("dimen", "ad_main_small_native")?.toLong() ?: error("Could not find dimen/ad_main_small_native"),
                    resources.id("dimen", "ad_minimum")?.toLong() ?: error("Could not find dimen/ad_minimum"),
                )
            }.single().apply {
                alwaysReturn(0)
            }

            methods("footerAd method") {
                flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
                returns(Type.Void)
                literals(
                    resources.id("dimen", "read_ad_minimum")?.toLong() ?: error("Could not find dimen/read_ad_minimum"),
                )
            }.single().apply {
                alwaysReturn()
            }

            methods("image ad method") {
                flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
                returns(Type.Void)
                literals(
                    resources.id("dimen", "image_ad")?.toLong() ?: error("Could not find dimen/image_ad"),
                    resources.id("dimen", "ad_minimum")?.toLong() ?: error("Could not find dimen/ad_minimum"),
                )
            }.single().point {
                invokeVirtual { name("setMinimumHeight") }
            }.captureArgumentAs("minimumHeight", 1, Type.Int)
                .before {
                    capture("minimumHeight").assign(int(0))
                }

            klass("imageAd class") {
                strings("and_ad_banner_read_img")
            }.methods("imageAd method") {
                calls { name("setLayoutParams") }
            }.single().before {
                param(0).assign(int(0))
            }
        }
    }
}