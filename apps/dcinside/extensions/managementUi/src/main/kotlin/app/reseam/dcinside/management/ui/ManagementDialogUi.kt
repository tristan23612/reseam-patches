package app.reseam.dcinside.management.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Toast
import app.reseam.runtime.Reseam
import app.reseam.runtime.ui.BrandDialog
import app.reseam.runtime.ui.Ui

/** Shared entry point for management dialogs using the user's ReSeam appearance. */
object ManagementDialogUi {
    private const val BUNDLE_NAME = "tristan-patches"

    /** Creates a ReSeam UI whose colors follow the appearance selected in ReSeam settings. */
    @JvmStatic
    fun create(context: Context): Ui = Reseam.bundle(BUNDLE_NAME).ui(context)

    /** Creates a ReSeam-styled dialog builder for a management flow. */
    @JvmStatic
    fun dialog(context: Context, title: CharSequence): BrandDialog.Builder =
        create(context).dialog().title(title)

    /** Creates a vertical container for custom dialog content. */
    @JvmStatic
    fun column(context: Context, vararg children: View): LinearLayout =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            children.forEach { child ->
                addView(
                    child,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ),
                )
            }
        }

    /** Creates a list that can scroll while keeping a dialog within a fraction of the screen. */
    @JvmStatic
    fun heightLimitedList(context: Context, heightFraction: Float = 0.5f): ListView {
        require(heightFraction in 0f..1f) { "heightFraction must be between 0 and 1" }
        val maxHeight = (context.resources.displayMetrics.heightPixels * heightFraction).toInt()

        return object : ListView(context) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                val limitedHeight = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
                super.onMeasure(widthMeasureSpec, limitedHeight)
            }
        }
    }

    /** Copies text and shows the caller-provided confirmation message. */
    @JvmStatic
    fun copyToClipboard(
        context: Context,
        label: String,
        text: String,
        confirmation: CharSequence,
    ): Boolean {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return false

        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, confirmation, Toast.LENGTH_SHORT).show()
        return true
    }
}
