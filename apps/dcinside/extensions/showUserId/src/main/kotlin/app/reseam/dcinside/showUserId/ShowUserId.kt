@file:JvmName("ShowUserId")

package app.reseam.dcinside.showUserId

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import app.reseam.dcinside.customInterceptor.DcinsideState
import app.reseam.runtime.Export

private const val USER_ID_VIEW_NAME = "reseam_user_id"
private const val GALLERY_DATA_VIEW_NAME = "reseam_gallery_data_space"
private const val DEFAULT_USER_ID_COLOR = 0xFF9E9E9E.toInt()

private val MEMO_VIEW_NAMES = arrayOf(
    "read_header_user_memo",
    "reply_user_memo",
    "post_list_item_counts",
)

@Export
fun setUserId(itemView: View?, userId: String?) {
    if (itemView == null) return

    val galleryDataView = itemView.findNamedView(GALLERY_DATA_VIEW_NAME)
    galleryDataView?.tag = mapOf(
        "gallery_id" to DcinsideState.galleryId.orEmpty(),
        "gallery_type" to DcinsideState.galleryType.orEmpty(),
    )

    val userIdView = itemView.findNamedView(USER_ID_VIEW_NAME) as? TextView ?: return
    userIdView.setOnClickListener(null)
    userIdView.setOnLongClickListener(null)

    if (userId.isNullOrEmpty()) {
        userIdView.visibility = View.GONE
        return
    }

    userIdView.text = " $userId "
    userIdView.setTextColor(findMemoColor(itemView) ?: DEFAULT_USER_ID_COLOR)
    userIdView.visibility = View.VISIBLE
    userIdView.setOnLongClickListener { view ->
        copyUserId(view.context, userId)
        true
    }
}

@Export
fun addUserId(userName: String, userId: String?): String =
    if (!userId.isNullOrEmpty()) {
        "$userName (${userId})"
    } else {
        userName
    }

private fun View.findNamedView(name: String): View? {
    val id = resources.getIdentifier(name, "id", context.packageName)
    return if (id == 0) null else findViewById(id)
}

private fun findMemoColor(root: View): Int? = MEMO_VIEW_NAMES.firstNotNullOfOrNull { name ->
    val memoView = root.findNamedView(name) as? TextView ?: return@firstNotNullOfOrNull null
    if (memoView.visibility != View.VISIBLE) return@firstNotNullOfOrNull null

    val text = memoView.text ?: return@firstNotNullOfOrNull null
    if (text.isEmpty() || text[0] != '-') return@firstNotNullOfOrNull null

    if (text is Spanned) {
        text.getSpans(0, text.length, ForegroundColorSpan::class.java)
            .firstOrNull()
            ?.let { return@firstNotNullOfOrNull it.foregroundColor }
    }

    memoView.currentTextColor
}

private fun copyUserId(context: Context, userId: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        ?: return

    clipboard.setPrimaryClip(ClipData.newPlainText("User ID", userId))
    Toast.makeText(context, "User ID가 복사되었습니다: $userId", Toast.LENGTH_SHORT).show()
}
