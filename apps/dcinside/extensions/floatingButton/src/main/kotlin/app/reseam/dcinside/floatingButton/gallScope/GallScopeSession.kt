package app.reseam.dcinside.floatingButton.gallScope

import android.app.Activity
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import app.reseam.dcinside.customInterceptor.DcinsideState
import app.reseam.dcinside.management.ui.ManagementDialogUi
import app.reseam.runtime.ui.BrandDialog
import app.reseam.runtime.ui.TextField
import app.reseam.runtime.ui.Ui
import java.util.concurrent.Executors
import kotlin.math.max

internal class GallScopeSession(
    private val context: Context,
    prefillUserId: String?,
    prefillGalleryType: String?,
    prefillGalleryId: String?,
    private val onClosed: (GallScopeSession) -> Unit,
) {
    private enum class Step { IDENTIFIER, MODE, RANGE, SEARCHING, RESULTS, ERROR }
    private enum class Mode(val label: String) { POSTS("게시글"), COMMENTS("댓글") }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val search = GallScopeSearch()
    private val results = mutableListOf<GallScopeResult>()
    private var targetUserId = prefillUserId?.trim().orEmpty()
    private var galleryType = prefillGalleryType?.trim().takeUnless { it.isNullOrEmpty() } ?: DcinsideState.galleryType.orEmpty()
    private var galleryId = prefillGalleryId?.trim().takeUnless { it.isNullOrEmpty() } ?: DcinsideState.galleryId.orEmpty()
    private var mode: Mode? = null
    private var step = if (targetUserId.isNotEmpty() && galleryId.isNotEmpty() && galleryType.isNotEmpty()) Step.MODE else Step.IDENTIFIER
    private var startPage = 1
    private var endPage = 10
    private var rangeSize = 10
    private var firstPage = 1
    private var lastPage = 0
    private var rangeExceeded = false
    private var progressView: TextView? = null
    private var dialog: Dialog? = null
    private var renderVersion = 0L
    private var minimized = false
    private var searching = false
    private var closed = false
    private var errorMessage = "검색에 실패했습니다."

    @Volatile private var started = false

    fun isStarted() = started

    fun start() {
        started = true
        show()
    }

    fun resume() {
        mainHandler.post {
            if (!closed) show()
        }
    }

    private fun show() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { show() }
            return
        }
        if (closed) return
        if (context is Activity && (context.isFinishing || context.isDestroyed)) {
            finish()
            return
        }
        minimized = false
        val version = ++renderVersion
        val previousDialog = dialog
        dialog = null
        progressView = null
        if (previousDialog?.isShowing == true) {
            previousDialog.dismiss()
            mainHandler.post {
                if (!closed && version == renderVersion) renderDialog(version)
            }
            return
        }
        renderDialog(version)
    }

    private fun renderDialog(version: Long) {
        if (closed || version != renderVersion) return
        val ui = ManagementDialogUi.create(context)
        val builder = buildDialog(ui)
            .onDismiss {
                mainHandler.post {
                    if (closed || version != renderVersion) return@post
                    dialog = null
                    if (searching || step == Step.SEARCHING) {
                        minimized = true
                    } else {
                        finish()
                    }
                }
            }
        dialog = builder.show()
    }

    private fun buildDialog(ui: Ui): BrandDialog.Builder = when (step) {
        Step.IDENTIFIER -> identifierDialog(ui)
        Step.MODE -> modeDialog(ui)
        Step.RANGE -> rangeDialog(ui)
        Step.SEARCHING -> searchingDialog(ui)
        Step.RESULTS -> resultsDialog(ui)
        Step.ERROR -> errorDialog(ui)
    }

    private fun identifierDialog(ui: Ui): BrandDialog.Builder {
        val field = ui.textField("식별코드 또는 IP").apply {
            text(targetUserId)
            this.field.inputType = InputType.TYPE_CLASS_TEXT
            this.field.isSingleLine = true
        }
        return ui.dialog()
            .title("갤스코프")
            .message("검색할 유저의 식별코드를 입력하세요.")
            .view(ManagementDialogUi.column(context, field))
            .action("다음") {
                val value = field.text.trim()
                if (value.isEmpty()) {
                    Toast.makeText(context, "식별코드를 입력해주세요.", Toast.LENGTH_SHORT).show()
                    return@action
                }
                if (galleryId.isEmpty() || galleryType.isEmpty()) {
                    errorMessage = "현재 갤러리 정보를 확인할 수 없습니다. 갤러리 목록을 다시 불러온 뒤 시도해주세요."
                    step = Step.ERROR
                } else {
                    targetUserId = value
                    step = Step.MODE
                }
                show()
            }
            .dismissAction("취소")
    }

    private fun modeDialog(ui: Ui): BrandDialog.Builder = ui.dialog()
        .title("갤스코프")
        .message(targetUserId)
        .choices(arrayOf("게시글 검색", "댓글 검색"), -1) { choice ->
            mode = if (choice == 0) Mode.POSTS else Mode.COMMENTS
            if (choice == 1) search.resetComments()
            step = Step.RANGE
            show()
        }
        .dismissAction("취소")

    private fun rangeDialog(ui: Ui): BrandDialog.Builder {
        val selectedMode = mode ?: Mode.POSTS
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(8))
        }
        val startField: TextField?
        val endField: TextField
        if (selectedMode == Mode.POSTS) {
            val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            startField = ui.textField("시작 페이지").text(startPage.toString()).apply {
                field.inputType = InputType.TYPE_CLASS_NUMBER
            }
            endField = ui.textField("끝 페이지").text(endPage.toString()).apply {
                field.inputType = InputType.TYPE_CLASS_NUMBER
            }
            row.addView(startField, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(View(context), LinearLayout.LayoutParams(dp(8), 1))
            row.addView(endField, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            content.addView(row)
        } else {
            startField = null
            endField = ui.textField("이번에 검색할 페이지 수").text(rangeSize.toString()).apply {
                field.inputType = InputType.TYPE_CLASS_NUMBER
            }
            content.addView(endField)
        }

        return ui.dialog()
            .title("${selectedMode.label} 검색")
            .message(targetUserId)
            .view(content)
            .action("검색 시작") {
                val parsedEnd = endField.text.trim().toIntOrNull()
                val parsedStart = startField?.text?.trim()?.toIntOrNull() ?: 1
                if (parsedStart <= 0 || parsedEnd == null || parsedEnd <= 0 || parsedStart > parsedEnd) {
                    Toast.makeText(context, "검색 범위를 확인해주세요.", Toast.LENGTH_SHORT).show()
                    return@action
                }
                startPage = parsedStart
                endPage = if (selectedMode == Mode.POSTS) parsedEnd else parsedStart + parsedEnd - 1
                rangeSize = endPage - startPage + 1
                if (results.isEmpty()) firstPage = startPage
                startSearch()
            }
            .dismissAction("취소")
    }

    private fun searchingDialog(ui: Ui): BrandDialog.Builder {
        val body = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(12))
            addView(ProgressBar(context), LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            })
            progressView = ui.text(progressText(0, rangeSize), ui.type.bodyMedium, ui.colors.onSurface).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(12), 0, 0)
            }
            addView(progressView)
        }
        return ui.dialog()
            .title("갤스코프")
            .message("${mode?.label} 검색 중")
            .view(body)
            .action("최소화") { minimize() }
    }

    private fun resultsDialog(ui: Ui): BrandDialog.Builder {
        val snapshot = results.toList()
        val summary = ui.text("$targetUserId · ${mode?.label} · ${snapshot.size}건 · ${firstPage}~${lastPage}페이지", ui.type.bodyMedium, ui.colors.onSurface)
            .apply { setPadding(dp(20), dp(8), dp(20), dp(8)) }
        val list = ManagementDialogUi.heightLimitedList(context, 0.52f)
        list.adapter = object : ArrayAdapter<GallScopeResult>(context, 0, snapshot) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val item = getItem(position) ?: return View(context)
                val supporting = if (item.type == GallScopeResult.Type.COMMENT) {
                    "${item.nickname} · ${item.date}"
                } else {
                    "${item.nickname} · ${item.date} · 댓글 ${item.replyCount} · 조회 ${item.views} · 추천 ${item.recommend}"
                }
                return ui.listItem(item.title.ifEmpty { "(제목 없음)" }).supporting(supporting).chevron()
            }
        }
        list.setOnItemClickListener { _, _, position, _ -> openResult(snapshot[position]) }
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(summary)
            addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        return ui.dialog()
            .title("갤스코프 결과")
            .view(content)
            .action("복사") { copyResults(snapshot) }
            .apply {
                if (!rangeExceeded) action("계속 검색") {
                    if (mode == Mode.POSTS) {
                        startPage = lastPage + 1
                        endPage = startPage + rangeSize - 1
                    }
                    startSearch(append = true)
                }
            }
            .dismissAction("닫기")
    }

    private fun errorDialog(ui: Ui): BrandDialog.Builder = ui.dialog()
        .title("갤스코프")
        .message(errorMessage)
        .action("다시 시도") {
            if (mode == null) step = Step.IDENTIFIER else step = Step.RANGE
            show()
        }
        .dismissAction("닫기")

    private fun startSearch(append: Boolean = false) {
        val selectedMode = mode ?: return
        if (!append) results.clear()
        step = Step.SEARCHING
        searching = true
        GallScopeController.searchingChanged(true)
        show()

        val currentStart = startPage
        val currentEnd = endPage
        val count = rangeSize
        executor.execute {
            try {
                val batch = when (selectedMode) {
                    Mode.POSTS -> search.searchPosts(galleryType, galleryId, targetUserId, currentStart, currentEnd, ::onProgress)
                    Mode.COMMENTS -> search.searchComments(galleryType, galleryId, targetUserId, count, ::onProgress)
                }
                mainHandler.post {
                    searching = false
                    GallScopeController.searchingChanged(false)
                    results += batch.results
                    lastPage = max(lastPage, batch.lastPage)
                    rangeExceeded = batch.rangeExceeded
                    step = Step.RESULTS
                    if (isDialogShowing()) show() else Toast.makeText(context, "갤스코프 검색이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                }
            } catch (error: Exception) {
                mainHandler.post {
                    searching = false
                    GallScopeController.searchingChanged(false)
                    errorMessage = "검색 중 오류가 발생했습니다.\n${error.message.orEmpty()}"
                    step = Step.ERROR
                    if (isDialogShowing()) show() else Toast.makeText(context, "갤스코프 검색 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun onProgress(done: Int, total: Int) {
        mainHandler.post {
            progressView?.text = progressText(done, total)
        }
    }

    private fun progressText(done: Int, total: Int) = "검색한 페이지 $done / $total\n결과 ${results.size}건"
    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density + 0.5f).toInt()

    private fun minimize() {
        if (!searching) return
        minimized = true
        val current = dialog
        dialog = null
        current?.dismiss()
    }

    private fun isDialogShowing() = !minimized && dialog?.isShowing == true

    private fun openResult(item: GallScopeResult) {
        val number = item.postNo.toIntOrNull() ?: return
        runCatching {
            val intent = Intent().setClassName(context.packageName, "com.dcinside.app.PostReadActivity")
                .putExtra("com.dcinside.app.extra.GALLERY_ID", galleryId)
                .putExtra("com.dcinside.app.extra.POST_NUMBER", number)
            if (item.type == GallScopeResult.Type.COMMENT) item.fcno.toIntOrNull()?.let {
                intent.putExtra("com.dcinside.app.extra.COMMENT_NUMBER", it)
            }
            context.startActivity(intent)
        }.onFailure {
            Toast.makeText(context, "게시글을 열 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyResults(snapshot: List<GallScopeResult>) {
        val text = buildString {
            append("$targetUserId ${mode?.label} 검색 결과 (${snapshot.size}건)\n")
            append("검색 페이지: ${firstPage}~${lastPage}페이지\n\n")
            snapshot.forEach { append(it.title).append('\n').append(it.date).append('\n').append(it.url).append("\n\n") }
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("GallScope Results", text))
        Toast.makeText(context, "검색 결과를 복사했습니다.", Toast.LENGTH_SHORT).show()
    }

    private fun finish() {
        if (closed) return
        closed = true
        searching = false
        GallScopeController.searchingChanged(false)
        dialog = null
        onClosed(this)
    }

    private companion object {
        val executor = Executors.newCachedThreadPool { task ->
            Thread(task, "dcinside-gall-scope").apply { isDaemon = true }
        }
    }
}
