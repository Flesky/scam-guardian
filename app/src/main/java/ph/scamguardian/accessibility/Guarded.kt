package ph.scamguardian.accessibility

import android.util.Log
import kotlinx.coroutines.CancellationException

private const val TAG = "SG"

/**
 * Runs [block] and logs any failure as "Could not [action]". The accessibility service must never crash,
 * so this catches everything except coroutine cancellation.
 */
@Suppress("TooGenericExceptionCaught")
internal inline fun guarded(
    action: String,
    block: () -> Unit,
) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Could not $action", e)
    }
}
