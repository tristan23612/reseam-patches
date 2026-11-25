@file:JvmName("PostLongPress")

package app.reseam.dcinside.postLongPress

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.View
import app.reseam.dcinside.customInterceptor.DcinsideState
import app.reseam.dcinside.management.ui.ManagementDialogSession
import app.reseam.runtime.Export
import app.reseam.runtime.ToggleValue
import app.reseam.runtime.ui.BrandDialog
import app.reseam.runtime.ui.Ui
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import java.util.WeakHashMap

private const val BLOCK_ACTIVITY = "com.dcinside.app.manager.MinorExtActivity"
private const val BLOCK_ACTION = "action_block"
private const val EXTRA_GALLERY_ID = "com.dcinside.app.extra.GALLERY_ID"
private const val EXTRA_POST_NUMBER = "com.dcinside.app.extra.POST_NUMBER"
private const val BLOCK_REQUEST_CODE = 1031
private const val DELETE_URL = "https://app.dcinside.com/api/gall_del.php"
private const val TOKEN_PREFERENCES = "save"
private const val TOKEN_KEY = "firebaseInstanceToken"

@Volatile private var postLongPressToggleValue: ToggleValue? = null

@Export
fun postLongPress(key: String, defaultValue: Boolean) {
    postLongPressToggleValue = ToggleValue("tristan-patches", key, defaultValue)
}

private val deleteExecutor = Executors.newSingleThreadExecutor { runnable ->
    Thread(runnable, "dcinside-post-delete").apply { isDaemon = true }
}
private data class RowDefaults(
    val foreground: Drawable?,
    val clickable: Boolean,
    val longClickable: Boolean,
)

private val rowDefaults = WeakHashMap<View, RowDefaults>()
private val boundPostNumbers = WeakHashMap<View, Int>()

/** Adds the management menu to a post row when the current user is a gallery manager. */
@Export
fun setLongClickListener(itemView: View?, postNo: Int) {
    itemView ?: return
    synchronized(boundPostNumbers) {
        boundPostNumbers[itemView] = postNo
    }

    val defaults = synchronized(rowDefaults) {
        rowDefaults.getOrPut(itemView) {
            RowDefaults(itemView.foreground, itemView.isClickable, itemView.isLongClickable)
        }
    }

    // RecyclerView reuses the same row view. Restore the app's normal state before
    // deciding whether this bind should enable management actions.
    itemView.alpha = 1f
    itemView.foreground = defaults.foreground
    itemView.isClickable = defaults.clickable
    itemView.isLongClickable = defaults.longClickable

    if (postLongPressToggleValue?.value != true || postNo <= 0 || DcinsideState.isGalleryManager != true) {
        itemView.setOnLongClickListener(null)
        return
    }

    val attributes = itemView.context.obtainStyledAttributes(
        intArrayOf(android.R.attr.selectableItemBackground),
    )
    try {
        itemView.foreground = attributes.getDrawable(0)
    } finally {
        attributes.recycle()
    }

    itemView.isLongClickable = true
    itemView.setOnLongClickListener { view ->
        PostManagementSession(view.context, view, postNo).start()
        true
    }
}

private class PostManagementSession(
    context: Context,
    private val postView: View,
    private val postNo: Int,
) : ManagementDialogSession<PostManagementSession.Step>(context, Step.Menu) {
    enum class Step {
        Menu,
        ConfirmBlock,
        ConfirmDelete,
        Deleting,
        Deleted,
        Failed,
    }

    private var failureMessage = "요청을 처리하지 못했습니다."

    override fun createDialog(ui: Ui, step: Step): BrandDialog.Builder = when (step) {
        Step.Menu -> ui.dialog()
            .title("게시글 관리")
            .choices(arrayOf("차단", "삭제"), -1) { choice ->
                when (choice) {
                    0 -> transitionTo(Step.ConfirmBlock)
                    1 -> transitionTo(Step.ConfirmDelete)
                }
            }
            .dismissAction("닫기")

        Step.ConfirmBlock -> ui.dialog()
            .title("게시글 차단")
            .message("이 게시글을 차단할까요?")
            .action("차단") { startBlockFlow() }
            .dismissAction("취소")

        Step.ConfirmDelete -> ui.dialog()
            .title("게시글 삭제")
            .message("이 게시글을 삭제할까요? 삭제한 게시글은 복구할 수 없습니다.")
            .action("삭제") { transitionTo(Step.Deleting) }
            .dismissAction("취소")

        Step.Deleting -> ui.dialog()
            .title("게시글 삭제")
            .message("게시글을 삭제하고 있습니다.")
            .dismissAction("백그라운드")

        Step.Deleted -> ui.dialog()
            .title("게시글 삭제")
            .message("게시글을 삭제했습니다.")
            .dismissAction("확인")

        Step.Failed -> ui.dialog()
            .title("게시글 관리")
            .message(failureMessage)
            .dismissAction("확인")
    }

    override fun onStepShown(step: Step) {
        if (step != Step.Deleting) return

        deleteExecutor.execute {
            val result = deletePost(context, postNo)
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                if (result.isSuccess) {
                    val stillBoundToDeletedPost = synchronized(boundPostNumbers) {
                        boundPostNumbers[postView] == postNo
                    }
                    if (stillBoundToDeletedPost) {
                        postView.alpha = 0.5f
                        postView.isClickable = false
                        postView.isLongClickable = false
                        postView.foreground = null
                        postView.setOnLongClickListener(null)
                    }
                    transitionTo(Step.Deleted)
                } else {
                    failureMessage = result.exceptionOrNull()?.message
                        ?: "게시글 삭제에 실패했습니다."
                    transitionTo(Step.Failed)
                }
            }
        }
    }

    private fun startBlockFlow() {
        val activity = context.findActivity()
        val galleryId = DcinsideState.galleryId
        if (activity == null || galleryId.isNullOrBlank()) {
            failureMessage = "갤러리 정보를 확인할 수 없습니다."
            transitionTo(Step.Failed)
            return
        }

        try {
            val managerActivity = Class.forName(BLOCK_ACTIVITY)
            val intent = Intent(activity, managerActivity).apply {
                setAction(BLOCK_ACTION)
                putExtra(EXTRA_GALLERY_ID, galleryId)
                putExtra(EXTRA_POST_NUMBER, postNo)
            }
            activity.startActivityForResult(intent, BLOCK_REQUEST_CODE)
            dismiss()
        } catch (exception: Exception) {
            failureMessage = exception.message ?: "차단 화면을 열지 못했습니다."
            transitionTo(Step.Failed)
        }
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun deletePost(context: Context, postNo: Int): Result<Unit> = runCatching {
    val userId = DcinsideState.userId
        ?: error("로그인 정보를 확인할 수 없습니다.")
    val galleryId = DcinsideState.galleryId
        ?: error("갤러리 정보를 확인할 수 없습니다.")
    val appId = DcinsideState.appId
        ?: error("앱 정보를 확인할 수 없습니다.")
    val token = context.getSharedPreferences(TOKEN_PREFERENCES, Context.MODE_PRIVATE)
        .getString(TOKEN_KEY, null)
        ?: error("인증 토큰을 확인할 수 없습니다.")

    val fields = linkedMapOf(
        "user_id" to userId,
        "client_token" to token,
        "id" to galleryId,
        "no" to postNo.toString(),
        "mode" to "board_del",
        "app_id" to appId,
    )
    val requestBody = fields.entries.joinToString("&") { (key, value) ->
        "${formEncode(key)}=${formEncode(value)}"
    }.toByteArray(StandardCharsets.UTF_8)

    val connection = (URL(DELETE_URL).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 10_000
        readTimeout = 10_000
        doOutput = true
        setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
    }

    try {
        connection.outputStream.use { it.write(requestBody) }
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val responseText = stream?.use { input ->
            BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).readText()
        }.orEmpty()

        if (status !in 200..299) error("서버 응답 오류 ($status)")
        val response = JSONObject(responseText)
        if (!response.optBoolean("result")) {
            error(response.optString("msg").ifBlank { "게시글 삭제에 실패했습니다." })
        }
    } finally {
        connection.disconnect()
    }
}

private fun formEncode(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8.name())
