package app.reseam.dcinside.floatingButton.banList

import app.reseam.runtime.TextValue
import app.reseam.runtime.ToggleValue

/** Runtime settings supplied by the BanList patch's app-entry hook. */
internal object BanListSettings {
    private const val BUNDLE = "tristan-patches"

    @Volatile private var enableBanListIdentifierSearch: ToggleValue? = null
    @Volatile private var enableBanListIngest: ToggleValue? = null
    @Volatile private var banListWorkerUrl: TextValue? = null
    @Volatile private var banListWorkerViewToken: TextValue? = null
    @Volatile private var banListWorkerIngestToken: TextValue? = null
    @Volatile private var galleriesJson: TextValue? = null
    @Volatile private var dcinsideUserId: TextValue? = null
    @Volatile private var dcinsidePassword: TextValue? = null

    val banListIdentifierSearchEnabled: Boolean
        get() = enableBanListIdentifierSearch?.value == true

    val banListIngestEnabled: Boolean
        get() = enableBanListIngest?.value == true

    val hasAnyEnabledAction: Boolean
        get() = banListIdentifierSearchEnabled || banListIngestEnabled

    var banListWorkerUrlValue: String
        get() = banListWorkerUrl?.value.orEmpty()
        set(value) { banListWorkerUrl?.value = value }

    var banListWorkerViewTokenValue: String
        get() = banListWorkerViewToken?.value.orEmpty()
        set(value) { banListWorkerViewToken?.value = value }

    var banListWorkerIngestTokenValue: String
        get() = banListWorkerIngestToken?.value.orEmpty()
        set(value) { banListWorkerIngestToken?.value = value }

    var galleriesJsonValue: String
        get() = galleriesJson?.value.orEmpty()
        set(value) { galleriesJson?.value = value }

    var dcinsideUserIdValue: String
        get() = dcinsideUserId?.value.orEmpty()
        set(value) { dcinsideUserId?.value = value }

    var dcinsidePasswordValue: String
        get() = dcinsidePassword?.value.orEmpty()
        set(value) { dcinsidePassword?.value = value }

    fun enableBanListIdentifierSearch(key: String, defaultValue: Boolean) {
        enableBanListIdentifierSearch = ToggleValue(BUNDLE, key, defaultValue)
    }
    fun enableBanListIngest(key: String, defaultValue: Boolean) {
        enableBanListIngest = ToggleValue(BUNDLE, key, defaultValue)
    }
    fun banListWorkerUrl(key: String, defaultValue: String) {
        banListWorkerUrl = TextValue(BUNDLE, key, defaultValue)
    }
    fun banListWorkerViewToken(key: String, defaultValue: String) {
        banListWorkerViewToken = TextValue(BUNDLE, key, defaultValue)
    }
    fun banListWorkerIngestToken(key: String, defaultValue: String) {
        banListWorkerIngestToken = TextValue(BUNDLE, key, defaultValue)
    }
    fun galleriesJson(key: String, defaultValue: String) {
        galleriesJson = TextValue(BUNDLE, key, defaultValue)
    }
    fun dcinsideUserId(key: String, defaultValue: String) {
        dcinsideUserId = TextValue(BUNDLE, key, defaultValue)
    }
    fun dcinsidePassword(key: String, defaultValue: String) {
        dcinsidePassword = TextValue(BUNDLE, key, defaultValue)
    }
}
