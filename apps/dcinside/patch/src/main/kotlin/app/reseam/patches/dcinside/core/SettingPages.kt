package app.reseam.patches.dcinside.core

import app.reseam.patch.settings.SettingsPage
import app.reseam.patch.settings.Vector
import app.reseam.dcinside.settings.DcinsideStrings

internal object DcinsideSettingsPages {
    val Ads = SettingsPage("ads", DcinsideStrings.pageAds, order = 0, summary = DcinsideStrings.pageAdsSummary, icon = Vector.lucide("megaphone-off"))
    val Home = SettingsPage("home", DcinsideStrings.pageHome, order = 10, summary = DcinsideStrings.pageHomeSummary, icon = Vector.lucide("house"))
    val General = SettingsPage("general", DcinsideStrings.pageGeneral, order = 20, summary = DcinsideStrings.pageGeneralSummary, icon = Vector.lucide("sliders-horizontal"))
    val Management = SettingsPage("management", DcinsideStrings.pageManagement, order = 30, summary = DcinsideStrings.pageManagementSummary, icon = Vector.lucide("user-round-shield"))
    val FloatingButton = SettingsPage("floating_button", DcinsideStrings.pageFloatingButton, parent = Management, order = 0, summary = DcinsideStrings.pageFloatingButtonSummary)
    val GallScope = SettingsPage("gall_scope", DcinsideStrings.pageGallScope, parent = FloatingButton, order = 0)
    val BanList = SettingsPage("ban_list", DcinsideStrings.pageBanList, parent = FloatingButton, order = 10)
    val Misc = SettingsPage("misc", DcinsideStrings.pageMisc, order = 40, summary = DcinsideStrings.pageMiscSummary, icon = Vector.lucide("square-dashed-plus"))
}