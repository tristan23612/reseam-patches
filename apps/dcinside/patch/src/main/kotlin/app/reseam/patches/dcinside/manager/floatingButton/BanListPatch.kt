package app.reseam.patches.dcinside.manager.floatingButton

import app.reseam.dcinside.floatingButton.banList.bindings.BanList
import app.reseam.dcinside.settings.DcinsideSettings
import app.reseam.dcinside.settings.DcinsideStrings
import app.reseam.patch.*
import app.reseam.patch.settings.section
import app.reseam.patches.dcinside.DcinsidePatchStrings
import app.reseam.patches.dcinside.core.DCINSIDE
import app.reseam.patches.dcinside.core.DcinsideSettingsPages
import app.reseam.patches.dcinside.core.dcinsideSettings

@Suppress("unused")
val banList = patch(DcinsidePatchStrings.banList) {
    description(DcinsidePatchStrings.banListDescription)
    compatibleWith(DCINSIDE)
    dependsOn(floatingButton)
    settings(
        dcinsideSettings,
        section(
            DcinsideSettingsPages.BanList,
            DcinsideStrings.sectionBanListButton,
            DcinsideSettings.showBanList,
            DcinsideSettings.banListWorkerUrl,
        ),
        section(
            DcinsideSettingsPages.BanList,
            DcinsideStrings.sectionBanListSearch,
            DcinsideSettings.enableBanListIdentifierSearch,
            DcinsideSettings.banListWorkerViewToken
        ),
        section(
            DcinsideSettingsPages.BanList,
            DcinsideStrings.sectionBanListIngest,
            DcinsideSettings.enableBanListIngest,
            DcinsideSettings.banListWorkerIngestToken,
            DcinsideSettings.galleriesJson,
            DcinsideSettings.dcinsideUserId,
            DcinsideSettings.dcinsidePassword,
        )
    )

    execute {
        appEntry {
            BanList.showBanList(string(DcinsideSettings.showBanList.key), bool(DcinsideSettings.showBanList.default))
            BanList.enableBanListIdentifierSearch(string(DcinsideSettings.enableBanListIdentifierSearch.key), bool(DcinsideSettings.enableBanListIdentifierSearch.default))
            BanList.enableBanListIngest(string(DcinsideSettings.enableBanListIngest.key), bool(DcinsideSettings.enableBanListIngest.default))
            BanList.banListWorkerUrl(string(DcinsideSettings.banListWorkerUrl.key), string(DcinsideSettings.banListWorkerUrl.default))
            BanList.banListWorkerViewToken(string(DcinsideSettings.banListWorkerViewToken.key), string(DcinsideSettings.banListWorkerViewToken.default))
            BanList.banListWorkerIngestToken(string(DcinsideSettings.banListWorkerIngestToken.key), string(DcinsideSettings.banListWorkerIngestToken.default))
            BanList.galleriesJson(string(DcinsideSettings.galleriesJson.key), string(DcinsideSettings.galleriesJson.default))
            BanList.dcinsideUserId(string(DcinsideSettings.dcinsideUserId.key), string(DcinsideSettings.dcinsideUserId.default))
            BanList.dcinsidePassword(string(DcinsideSettings.dcinsidePassword.key), string(DcinsideSettings.dcinsidePassword.default))
        }

        addFloatingButtonResource("ban_list")
    }
}