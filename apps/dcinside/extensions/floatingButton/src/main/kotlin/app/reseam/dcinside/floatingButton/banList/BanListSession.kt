package app.reseam.dcinside.floatingButton.banList

import android.content.Context
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Toast
import app.reseam.dcinside.customInterceptor.DcinsideState
import app.reseam.dcinside.management.ui.ManagementDialogSession
import app.reseam.dcinside.management.ui.ManagementDialogUi
import app.reseam.runtime.ui.BrandDialog
import app.reseam.runtime.ui.TextField
import app.reseam.runtime.ui.Ui
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.Executors

internal class BanListSession(context: Context) : ManagementDialogSession<BanListSession.Step>(
    context,
    initialStep(),
) {
    internal enum class Step {
        WORKER_SETTINGS,
        DCINSIDE_SETTINGS,
        ACTION_SELECTION,
        IDENTIFIER_INPUT,
        IDENTIFIER_FETCHING,
        IDENTIFIER_RESULT,
        IDENTIFIER_ERROR,
        FETCHING_LATEST_DATA,
        INITIAL_INGEST_CONFIRMATION,
        PARSING,
        INGEST_CONFIRMATION,
        INGEST_IN_PROGRESS,
        INGEST_COMPLETE,
        INGEST_UNNECESSARY,
        ERROR,
    }

    private val galleryId = DcinsideState.galleryId?.trim().orEmpty()
    private val galleryType = DcinsideState.galleryType?.trim().orEmpty()
    private var identifier = ""
    private var identifierHeaders = emptyList<String>()
    private var identifierRows = emptyList<JSONObject>()
    private var latestData: JSONObject? = null
    private var banList = JSONArray()
    private var errorMessage = "인증 또는 처리 중 오류가 발생했습니다. 다시 시도해주세요."
    private var identifierErrorMessage = "DB 데이터를 불러오지 못했습니다."
    private var ingestResultMessage = "차단 내역 업로드가 완료되었습니다."
    private var workerSettingsReturnStep: Step? = null

    override fun createDialog(ui: Ui, step: Step): BrandDialog.Builder = when (step) {
        Step.WORKER_SETTINGS -> workerSettingsDialog(ui)
        Step.DCINSIDE_SETTINGS -> dcinsideSettingsDialog(ui)
        Step.ACTION_SELECTION -> actionSelectionDialog(ui)
        Step.IDENTIFIER_INPUT -> identifierInputDialog(ui)
        Step.IDENTIFIER_FETCHING -> ui.dialog().title("식별코드 조회").message("DB에서 식별코드를 찾고 있습니다.\n${galleryDescription()}")
        Step.IDENTIFIER_RESULT -> identifierResultsDialog(ui)
        Step.IDENTIFIER_ERROR -> ui.dialog().title("식별코드 조회")
            .message("$identifierErrorMessage\n${galleryDescription()}")
            .dismissAction("닫기")
            .action("재시도") { transitionTo(Step.IDENTIFIER_FETCHING) }
        Step.FETCHING_LATEST_DATA -> ui.dialog().title("차단 내역")
            .message("이전 업로드 기록을 확인하고 있습니다.\n${galleryDescription()}")
        Step.INITIAL_INGEST_CONFIRMATION -> ui.dialog().title("차단 내역")
            .message("DB에 기존 기록이 없습니다. 차단 목록 전체를 수집해 업로드하시겠습니까?\n${galleryDescription()}")
            .dismissAction("취소")
            .action("수집") { transitionTo(Step.PARSING) }
        Step.PARSING -> ui.dialog().title("차단 내역")
            .message("디시인사이드 차단 목록을 수집하고 있습니다.\n${galleryDescription()}")
        Step.INGEST_CONFIRMATION -> ui.dialog().title("차단 내역")
            .message("${banList.length()}건의 신규 차단 내역을 업로드하시겠습니까?\n${galleryDescription()}")
            .dismissAction("취소")
            .action("업로드") { transitionTo(Step.INGEST_IN_PROGRESS) }
        Step.INGEST_IN_PROGRESS -> ui.dialog().title("차단 내역")
            .message("Worker로 데이터를 전송하고 있습니다.\n${galleryDescription()}")
        Step.INGEST_COMPLETE -> ui.dialog().title("차단 내역")
            .message("$ingestResultMessage\n${galleryDescription()}")
            .dismissAction("확인")
        Step.INGEST_UNNECESSARY -> ui.dialog().title("차단 내역")
            .message("0건의 데이터가 수집되어 업로드할 필요가 없습니다.\n${galleryDescription()}")
            .dismissAction("확인")
        Step.ERROR -> ui.dialog().title("차단 내역")
            .message("$errorMessage\n${galleryDescription()}")
            .dismissAction("취소")
            .action("재시도") { transitionTo(actionStep()) }
    }

    override fun onStepShown(step: Step) {
        when (step) {
            Step.IDENTIFIER_FETCHING -> fetchIdentifierResults()
            Step.FETCHING_LATEST_DATA -> fetchLatestData()
            Step.PARSING -> parseBanList()
            Step.INGEST_IN_PROGRESS -> executeIngest()
            else -> Unit
        }
    }

    private fun workerSettingsDialog(ui: Ui): BrandDialog.Builder {
        val url = ui.textField("Worker URL").text(BanListSettings.banListWorkerUrlValue).apply {
            field.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            field.isSingleLine = true
        }
        val viewToken = ui.textField("View Token").text(BanListSettings.banListWorkerViewTokenValue).apply {
            field.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            field.isSingleLine = true
        }
        val ingestToken = ui.textField("Ingest Token").text(BanListSettings.banListWorkerIngestTokenValue).apply {
            field.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            field.isSingleLine = true
        }
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(8))
            addView(url)
            if (BanListSettings.banListIdentifierSearchEnabled) addField(this, viewToken, dp(12))
            if (BanListSettings.banListIngestEnabled) addField(this, ingestToken, dp(12))
        }
        return ui.dialog()
            .title("Worker 설정")
            .message("연동할 Cloudflare Worker 정보를 입력하세요.")
            .view(content)
            .action("취소") {
                workerSettingsReturnStep?.let(::transitionTo) ?: dismiss()
            }
            .action("저장") {
                val newUrl = url.text.trim()
                val newViewToken = viewToken.text.trim()
                val newIngestToken = ingestToken.text.trim()
                if (newUrl.isEmpty() ||
                    (BanListSettings.banListIdentifierSearchEnabled && newViewToken.isEmpty()) ||
                    (BanListSettings.banListIngestEnabled && newIngestToken.isEmpty())
                ) {
                    Toast.makeText(context, "Worker URL과 활성 기능의 토큰을 입력해주세요.", Toast.LENGTH_SHORT).show()
                    transitionTo(Step.WORKER_SETTINGS)
                    return@action
                }
                BanListSettings.banListWorkerUrlValue = newUrl
                if (BanListSettings.banListIdentifierSearchEnabled) BanListSettings.banListWorkerViewTokenValue = newViewToken
                if (BanListSettings.banListIngestEnabled) BanListSettings.banListWorkerIngestTokenValue = newIngestToken
                transitionTo(workerSettingsReturnStep ?: nextAfterInitialWorkerSettings())
            }
    }

    private fun dcinsideSettingsDialog(ui: Ui): BrandDialog.Builder {
        val galleries = ui.textField("갤러리 설정 JSON").text(BanListSettings.galleriesJsonValue).apply {
            field.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            field.isSingleLine = false
            field.minLines = 2
            field.maxLines = 5
        }
        val userId = ui.textField("DCInside ID").text(BanListSettings.dcinsideUserIdValue).apply {
            field.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            field.isSingleLine = true
        }
        val password = ui.textField("DCInside 비밀번호").text(BanListSettings.dcinsidePasswordValue).apply {
            field.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            field.isSingleLine = true
        }
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(8))
            addView(galleries)
            addField(this, userId, dp(12))
            addField(this, password, dp(12))
        }
        return ui.dialog()
            .title("DCInside 설정")
            .message("PC 차단 목록을 수집할 계정과 갤러리 경로를 입력하세요.\n예: [{\"id\":\"onshinproject\",\"path\":\"mgallery\"}]")
            .view(content)
            .dismissAction("취소")
            .action("저장") {
                val newGalleries = galleries.text.trim()
                val newUserId = userId.text.trim()
                val newPassword = password.text
                if (newGalleries.isEmpty() || newUserId.isEmpty() || newPassword.isEmpty()) {
                    Toast.makeText(context, "갤러리 JSON과 DCInside 계정 정보를 입력해주세요.", Toast.LENGTH_SHORT).show()
                    transitionTo(Step.DCINSIDE_SETTINGS)
                    return@action
                }
                try {
                    BanListApi.resolveGalleryPath(newGalleries, galleryId)
                } catch (error: JSONException) {
                    Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
                    transitionTo(Step.DCINSIDE_SETTINGS)
                    return@action
                }
                BanListSettings.galleriesJsonValue = newGalleries
                BanListSettings.dcinsideUserIdValue = newUserId
                BanListSettings.dcinsidePasswordValue = newPassword
                transitionTo(actionStep())
            }
    }

    private fun actionSelectionDialog(ui: Ui): BrandDialog.Builder {
        val actions = buildList {
            if (BanListSettings.banListIdentifierSearchEnabled) add("식별코드 조회")
            if (BanListSettings.banListIngestEnabled) add("차단 내역 내보내기")
        }.toTypedArray()
        return ui.dialog()
            .title("차단 내역")
            .message("원하는 작업을 선택하세요.\n${galleryDescription()}")
            .choices(actions, -1) { choice ->
                if (BanListSettings.banListIdentifierSearchEnabled && choice == 0) {
                    transitionTo(Step.IDENTIFIER_INPUT)
                } else {
                    transitionTo(Step.FETCHING_LATEST_DATA)
                }
            }
            .dismissAction("취소")
            .action("Worker 설정") {
                workerSettingsReturnStep = Step.ACTION_SELECTION
                transitionTo(Step.WORKER_SETTINGS)
            }
    }

    private fun identifierInputDialog(ui: Ui): BrandDialog.Builder {
        val field = ui.textField("식별코드 입력").text(identifier).apply {
            this.field.inputType = InputType.TYPE_CLASS_TEXT
            this.field.isSingleLine = true
            this.field.gravity = Gravity.CENTER
        }
        return ui.dialog()
            .title("식별코드 조회")
            .message("DB에서 정확히 일치하는 식별코드를 찾습니다.\n${galleryDescription()}")
            .view(ManagementDialogUi.column(context, field))
            .dismissAction("취소")
            .action("Worker 설정") {
                workerSettingsReturnStep = Step.IDENTIFIER_INPUT
                transitionTo(Step.WORKER_SETTINGS)
            }
            .action("조회") {
                identifier = normalizeIdentifier(field.text)
                if (identifier.isEmpty()) {
                    Toast.makeText(context, "식별코드를 입력해주세요.", Toast.LENGTH_SHORT).show()
                    transitionTo(Step.IDENTIFIER_INPUT)
                    return@action
                }
                transitionTo(Step.IDENTIFIER_FETCHING)
            }
    }

    private fun identifierResultsDialog(ui: Ui): BrandDialog.Builder {
        val snapshot = identifierRows.toList()
        val list = ManagementDialogUi.heightLimitedList(context, 0.5f)
        list.adapter = object : ArrayAdapter<JSONObject>(context, 0, snapshot) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val item = getItem(position) ?: JSONObject()
                val title = item.optString(CONTENT_COLUMN).ifBlank { "(내용 없음)" }
                return ui.listItem(title).supporting(formatIdentifierRow(item))
            }
        }
        list.setOnItemLongClickListener { _, _, position, _ ->
            ManagementDialogUi.copyToClipboard(
                context,
                "BanList Identifier Result",
                formatIdentifierRow(snapshot[position]),
                "결과를 복사했습니다.",
            )
        }
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(ui.text("$identifier 식별코드 조회 결과 · ${snapshot.size}건", ui.type.bodyMedium, ui.colors.onSurface))
            addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        return ui.dialog()
            .title("식별코드 조회")
            .message(galleryDescription())
            .view(content)
            .dismissAction("닫기")
            .action("복사") { copyIdentifierResults(snapshot) }
    }

    private fun fetchIdentifierResults() {
        executor.execute {
            try {
                val response = BanListApi.searchIdentifier(galleryId, identifier)
                identifierHeaders = response.headers
                identifierRows = response.rows
                transitionTo(Step.IDENTIFIER_RESULT)
            } catch (error: Exception) {
                identifierErrorMessage = "데이터를 불러오지 못했습니다.\n(${error.message.orEmpty()})"
                transitionTo(Step.IDENTIFIER_ERROR)
            }
        }
    }

    private fun fetchLatestData() {
        executor.execute {
            try {
                latestData = BanListApi.getLatestData(galleryId)
                transitionTo(
                    if (latestData == null || latestData?.length() == 0) Step.INITIAL_INGEST_CONFIRMATION
                    else Step.PARSING,
                )
            } catch (error: Exception) {
                errorMessage = "이전 업로드 기록을 확인하지 못했습니다.\n(${error.message.orEmpty()})"
                transitionTo(Step.ERROR)
            }
        }
    }

    private fun parseBanList() {
        executor.execute {
            try {
                banList = BanListApi.collectBanList(galleryId, latestData)
                transitionTo(if (banList.length() == 0) Step.INGEST_UNNECESSARY else Step.INGEST_CONFIRMATION)
            } catch (error: Exception) {
                errorMessage = "차단 목록을 수집하지 못했습니다.\n(${error.message.orEmpty()})"
                transitionTo(Step.ERROR)
            }
        }
    }

    private fun executeIngest() {
        executor.execute {
            try {
                val response = BanListApi.ingest(galleryId, banList)
                ingestResultMessage = formatIngestResult(response, banList.length())
                transitionTo(Step.INGEST_COMPLETE)
            } catch (error: Exception) {
                errorMessage = "Cloudflare Worker 업로드에 실패했습니다.\n(${error.message.orEmpty()})"
                transitionTo(Step.ERROR)
            }
        }
    }

    private fun formatIngestResult(response: JSONObject, defaultInserted: Int): String {
        val database = response.optJSONObject("database")
        val snapshot = response.optJSONObject("snapshot")
        val refreshStatus = response.optJSONObject("refreshStatus")
        val inserted = database?.optInt("inserted", response.optInt("inserted", defaultInserted))
            ?: response.optInt("inserted", defaultInserted)
        return buildString {
            append("D1 저장 완료: ${inserted}건")
            when (snapshot?.optString("status")) {
                null -> append("\nKV 갱신 상태를 응답에서 확인할 수 없습니다.")
                "updated" -> {
                    append("\nKV 스냅샷 갱신 완료: ${snapshot.optInt("rows")}건")
                    if (!snapshot.optBoolean("cachePurged", true)) {
                        append("\nCDN 캐시 비우기에 실패했습니다: ${snapshot.optString("message", "알 수 없는 오류")}")
                        append("\nD1 저장은 완료됐으므로 업로드를 다시 시도하지 마세요.")
                    }
                }
                "failed" -> append("\nKV 스냅샷 갱신 실패: ${snapshot.optString("message", "알 수 없는 오류")}\nD1 저장은 완료됐으므로 업로드를 다시 시도하지 마세요.")
                "skipped" -> append("\nKV 스냅샷 갱신은 생략되었습니다.")
                else -> append("\nKV 갱신 상태를 확인할 수 없습니다.")
            }
            when {
                refreshStatus == null -> append("\n웹 갱신 상태를 응답에서 확인할 수 없습니다.")
                refreshStatus.optBoolean("updated") -> append("\n웹 갱신 상태도 업데이트했습니다.")
                else -> append("\n웹 갱신 상태는 업데이트하지 않았습니다: ${refreshStatus.optString("message", "알 수 없는 오류")}")
            }
        }
    }

    private fun copyIdentifierResults(rows: List<JSONObject>) {
        val text = buildString {
            append("$identifier 식별코드 조회 결과\n${rows.size}건\n\n")
            rows.forEach { append(formatIdentifierRow(it)).append("\n\n") }
        }
        ManagementDialogUi.copyToClipboard(context, "BanList Identifier Results", text, "결과를 복사했습니다.")
    }

    private fun formatIdentifierRow(row: JSONObject): String = identifierHeaders.joinToString("\n") { header ->
        "$header: ${row.optString(header)}"
    }

    private fun galleryDescription() = "갤러리: ${galleryId.ifBlank { "정보 없음" }}${galleryType.takeIf(String::isNotBlank)?.let { " ($it)" }.orEmpty()}"

    private fun addField(parent: LinearLayout, field: TextField, topMargin: Int) {
        parent.addView(field, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            this.topMargin = topMargin
        })
    }

    private fun dp(value: Int) = (value * context.resources.displayMetrics.density + 0.5f).toInt()

    private fun normalizeIdentifier(value: String): String =
        value.trim().replace(Regex("(?i)\\s*\\+\\s*IP\\s*$"), "").trim()

    companion object {
        private const val CONTENT_COLUMN = "게시글 / 댓글"
        private val executor = Executors.newCachedThreadPool { task ->
            Thread(task, "reseam-ban-list").apply { isDaemon = true }
        }

        private fun initialStep(): Step {
            if (workerSettingsMissing()) return Step.WORKER_SETTINGS
            if (BanListSettings.banListIngestEnabled && dcinsideSettingsMissing()) return Step.DCINSIDE_SETTINGS
            return actionStep()
        }

        private fun workerSettingsMissing() = BanListSettings.banListWorkerUrlValue.isBlank() ||
            (BanListSettings.banListIdentifierSearchEnabled && BanListSettings.banListWorkerViewTokenValue.isBlank()) ||
            (BanListSettings.banListIngestEnabled && BanListSettings.banListWorkerIngestTokenValue.isBlank())

        private fun dcinsideSettingsMissing() = BanListSettings.galleriesJsonValue.isBlank() ||
            BanListSettings.dcinsideUserIdValue.isBlank() || BanListSettings.dcinsidePasswordValue.isEmpty()

        private fun nextAfterInitialWorkerSettings() =
            if (BanListSettings.banListIngestEnabled && dcinsideSettingsMissing()) Step.DCINSIDE_SETTINGS
            else actionStep()

        private fun actionStep(): Step =
            if (BanListSettings.banListIdentifierSearchEnabled && !BanListSettings.banListIngestEnabled) Step.IDENTIFIER_INPUT
            else Step.ACTION_SELECTION
    }
}
