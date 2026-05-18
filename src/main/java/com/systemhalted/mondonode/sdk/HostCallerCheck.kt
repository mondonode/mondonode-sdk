package com.systemhalted.mondonode.sdk

import android.content.Context
import android.os.Binder
import android.os.Process

/**
 * Cached UID of the MondoNode host app, resolved lazily on first call.
 *
 * Package-level volatile so it is shared across all service instances in the same process
 * (all plugin APKs have exactly one such process). Benign double-init race: two threads both
 * seeing -1 will both resolve the same UID from PackageManager and write the same value.
 */
@Volatile private var _cachedHostUid: Int = -1

/**
 * Throws [SecurityException] if the Binder caller is not the MondoNode host process.
 *
 * Must be called on a Binder thread — [Binder.getCallingUid] returns [Process.myUid] outside
 * a live transaction, which causes the check to pass for same-process calls (tests, in-process
 * SDK use). This is intentional: same-process callers are already trusted.
 *
 * The host UID is looked up once and cached. If the host is not installed,
 * [SecurityException] is thrown immediately so no AIDL method can proceed.
 */
fun requireCallerIsHost(context: Context) {
    val callerUid = Binder.getCallingUid()
    if (callerUid == Process.myUid()) return
    val hostUid = resolveHostUid(context)
    if (callerUid != hostUid) {
        throw SecurityException(
            "Caller UID $callerUid is not authorized to call MondoNode plugin services " +
                "(expected host UID $hostUid)"
        )
    }
}

private fun resolveHostUid(context: Context): Int {
    val cached = _cachedHostUid
    if (cached != -1) return cached
    val uid = try {
        context.packageManager.getApplicationInfo(AuthConstants.HOST_PACKAGE_NAME, 0).uid
    } catch (e: Exception) {
        throw SecurityException(
            "MondoNode host (${AuthConstants.HOST_PACKAGE_NAME}) is not installed on this device"
        )
    }
    _cachedHostUid = uid
    return uid
}
