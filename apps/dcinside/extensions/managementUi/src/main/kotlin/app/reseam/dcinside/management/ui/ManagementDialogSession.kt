package app.reseam.dcinside.management.ui

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import app.reseam.runtime.ui.BrandDialog
import app.reseam.runtime.ui.Ui
import java.util.concurrent.atomic.AtomicLong

/**
 * Small state holder for dialogs that replace their contents as work progresses.
 * Use a plain [ManagementDialogUi.dialog] for one-step confirmations and choices.
 */
abstract class ManagementDialogSession<S : Any>(
    protected val context: Context,
    initialStep: S,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val transitionVersion = AtomicLong()
    private var currentDialog: Dialog? = null
    private var currentStep = initialStep
    private var renderGeneration = 0L
    private var started = false
    private var dismissed = false

    /** Starts the session and displays its initial step on the main thread. */
    fun start() {
        mainHandler.post {
            if (dismissed || started) return@post
            started = true
            renderStep()
        }
    }

    /** Builds the dialog contents for [step]. */
    protected abstract fun createDialog(ui: Ui, step: S): BrandDialog.Builder

    /** Called after a step is displayed; useful for starting its asynchronous work. */
    protected open fun onStepShown(step: S) = Unit

    /** Called once the dialog is dismissed or the session is explicitly closed. */
    protected open fun onSessionDismissed() = Unit

    /** Replaces the current dialog with the requested state on the main thread. */
    protected fun transitionTo(step: S) {
        val requestedVersion = transitionVersion.incrementAndGet()
        mainHandler.post {
            if (dismissed || requestedVersion != transitionVersion.get()) return@post
            currentStep = step
            renderStep()
        }
    }

    /** Closes the session and prevents later asynchronous callbacks from reopening it. */
    protected fun dismiss() {
        mainHandler.post {
            if (dismissed) return@post
            dismissed = true
            currentDialog?.dismiss()
            currentDialog = null
            onSessionDismissed()
        }
    }

    private fun renderStep() {
        if (context is Activity && (context.isFinishing || context.isDestroyed)) {
            dismissed = true
            onSessionDismissed()
            return
        }

        val generation = ++renderGeneration
        val versionAtRender = transitionVersion.get()
        currentDialog?.dismiss()

        val builder = createDialog(ManagementDialogUi.create(context), currentStep)
            .onDismiss {
                mainHandler.post {
                    // Ignore dismissal caused by replacing this dialog or by a state transition
                    // requested from one of its action callbacks.
                    if (generation == renderGeneration &&
                        versionAtRender == transitionVersion.get() &&
                        !dismissed
                    ) {
                        dismissed = true
                        currentDialog = null
                        onSessionDismissed()
                    }
                }
            }

        currentDialog = builder.show()
        onStepShown(currentStep)
    }
}
