package app.reseam.patches.dcinside.home

import app.reseam.dcinside.hideHomeElements.bindings.HideHomeElements
import app.reseam.dcinside.settings.DcinsideSettings
import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.points
import app.reseam.patch.settings.gate
import app.reseam.patch.settings.section
import app.reseam.patches.dcinside.DcinsidePatchStrings
import app.reseam.patches.dcinside.core.DCINSIDE
import app.reseam.patches.dcinside.core.DcinsideSettingsPages
import app.reseam.patches.dcinside.core.dcinsideSettings

@Suppress("unused")
val hideHomeElements = patch(DcinsidePatchStrings.hideHomeElements) {
    description(DcinsidePatchStrings.hideHomeElementsDescription)
    compatibleWith(DCINSIDE)
    settings(
        dcinsideSettings,
        section(
            DcinsideSettingsPages.Home,
            DcinsidePatchStrings.hideHomeElements,
            DcinsideSettings.hideBestPickItem,
            DcinsideSettings.hideRecommendedGalleries,
            DcinsideSettings.hideCrowd,
            DcinsideSettings.hideNewGalleries,
            DcinsideSettings.hideRecent,
        )
    )

    execute {
        hideHomeElements()
    }
}

private fun PatchRuntime.hideHomeElements() {
    val bestPickItemLayout = resourceId("layout", "view_best_pick_item")
    val bestPickTitleLayout = resourceId("layout", "view_main_best_pick_title")
    val recommendedGalleriesLayout = resourceId("layout", "view_recommend_galleries")
    val crowdLayout = resourceId("layout", "view_crowd")
    val recentLayout = resourceId("layout", "view_recent_basic")
    val spacingDimensions = listOf(
        "main_space_divide4" to resourceId("dimen", "main_space_divide4"),
        "main_space_divide5" to resourceId("dimen", "main_space_divide5"),
    )

    gate(DcinsideSettings.hideBestPickItem) {
        val bestPickItem = methods("bestPickItem") {
            literals(bestPickItemLayout)
            opcode(Opcode.IGET_OBJECT)
            returns(Type.Void)
        }.single()

        bestPickItem.point("bestPickItemView") {
            literal(bestPickItemLayout)
        }.next {
            opcode(Opcode.IGET_OBJECT)
            field { type(Type.View) }
        }.captureAs("view", Type.View)
            .after {
                HideHomeElements.hideBestPickView(capture("view"))
            }

        val bestPickTitle = methods("bestPickItemTitle") {
            literals(bestPickTitleLayout)
            calls { name("inflate"); returns(Type.View) }
        }.single()

        bestPickTitle.points("bestPickItemTitleInflate") {
            invoke {
                name("inflate")
                returns(Type.View)
            }
            argument(1) {
                literal(bestPickTitleLayout)
            }
        }.single()
            .next { resultOf(Type.View) }
            .captureAs("view", Type.View)
            .after {
                HideHomeElements.hideBestPickView(capture("view"))
            }
    }

    gate(DcinsideSettings.hideRecommendedGalleries) {
        val target = methods("recommendedGalleries") {
            literals(recommendedGalleriesLayout)
            calls { name("inflate"); returns(Type.View) }
        }.single()

        target.points("recommendedGalleriesInflate") {
            invoke {
                name("inflate")
                returns(Type.View)
            }
            argument(1) {
                literal(recommendedGalleriesLayout)
            }
        }.single()
            .next { resultOf(Type.View) }
            .captureAs("view", Type.View)
            .after { HideHomeElements.hideRecommendedGalleriesView(capture("view")) }
    }

    gate(DcinsideSettings.hideCrowd) {
        val target = methods("crowdView") {
            literals(crowdLayout)
            returns(Type.Void)
        }.single()

        target.after { HideHomeElements.hideCrowdView(thisObject) }
    }

    gate(DcinsideSettings.hideNewGalleries) {
        val target = method("setNewGalleries") {
            name("setNewGalleries")
            flags(AccessFlags.PUBLIC)
            paramCount(1)
            param(0, Type.List)
            returns(Type.Void)
        }

        target.before { param(0).assign(HideHomeElements.hideNewGalleries(param(0))) }
    }

    gate(DcinsideSettings.hideRecent) {
        val target = methods("recentView constructor") {
            literals(recentLayout)
            flags(AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR)
            paramCount(5)
            returns(Type.Void)
        }.single()

        target.after { HideHomeElements.hideRecentView(thisObject) }
    }

    spacingDimensions.forEach { (name, id) ->
        val readers = methods("$name dimension reader") { literals(id) }

        readers.points("$name dimension resource") { literal(id) }.forEach {
            next {
                invokeStatic {
                    paramCount(1)
                    hasParam(Type.Int)
                    returns(Type.Int)
                }
            }.next { resultOf(Type.Int) }
                .captureAs("dimension", Type.Int)
                .after {
                    capture("dimension").assign(
                        HideHomeElements.adjustDimension(capture("dimension"), string(name))
                    )
                }
        }
    }
}

private fun PatchRuntime.resourceId(type: String, name: String): Long =
    resources.id(type, name)?.toLong() ?: error("Could not find $type/$name")