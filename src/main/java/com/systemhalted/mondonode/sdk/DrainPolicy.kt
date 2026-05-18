package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * Controls what happens to buffered items when a [QueueDrainStep] snapshots a queue.
 *
 * [SLIDING] — ring-buffer semantics.  The snapshot returns the current contents; the
 *   buffer is NOT cleared afterward.  New items continue to arrive and the oldest are
 *   evicted at HWM (when [OverflowPolicy.DROP_OLDEST] is set).  Best for "last N
 *   measurements" windows where continuity matters more than batch isolation.
 *
 * [ACCUMULATE] — batch semantics.  The snapshot returns the current contents AND then
 *   clears the buffer so the next batch starts fresh.  Each batch is processed exactly
 *   once.  For true atomicity pair with [OverflowPolicy.DROP_NEWEST] so items already
 *   in the buffer are never evicted before the drain runs.
 */
@Serializable
enum class DrainPolicy {
    SLIDING,
    ACCUMULATE
}
