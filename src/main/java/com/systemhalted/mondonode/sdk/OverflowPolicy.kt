package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * What happens when a named queue's buffer is full (HWM reached).
 *
 * [DROP_NEWEST] — reject the incoming item; the in-progress batch is preserved intact.
 *   Use this when batch atomicity matters: no item is silently evicted from a batch
 *   already in the buffer.  Producers slow down naturally as they observe drops.
 *
 * [DROP_OLDEST] — evict the oldest buffered item to make room for the new one.
 *   Gives ring-buffer semantics: the buffer always holds the most recent HWM items.
 *   Note: using DROP_OLDEST with DrainPolicy.ACCUMULATE breaks batch atomicity —
 *   items already counted into the batch may be evicted before the drain runs.
 */
@Serializable
enum class OverflowPolicy {
    DROP_NEWEST,
    DROP_OLDEST
}
