package com.fivehouse.thewid.data.personal

import kotlinx.serialization.Serializable

const val FOCUS_SESSIONS = 4
const val FOCUS_WORK_MS = 25 * 60_000L
const val FOCUS_BREAK_MS = 5 * 60_000L

/** Four work sessions with a break after each, read from the clock so it survives the app closing. */
@Serializable
data class FocusState(
    val session: Int = 1,
    val onBreak: Boolean = false,
    val remainingMs: Long = FOCUS_WORK_MS,
    val endsAtEpochMs: Long? = null
) {
    val isRunning: Boolean get() = endsAtEpochMs != null

    private val phaseLength: Long get() = if (onBreak) FOCUS_BREAK_MS else FOCUS_WORK_MS

    fun remainingAt(nowMs: Long): Long = (endsAtEpochMs?.let { it - nowMs } ?: remainingMs).coerceAtLeast(0)

    fun progressAt(nowMs: Long): Float = 1f - remainingAt(nowMs).toFloat() / phaseLength

    /** The state as of [nowMs], moving through any phases that ran out. Stops after the last session. */
    fun settledAt(nowMs: Long): FocusState {
        var state = this
        while (true) {
            val end = state.endsAtEpochMs ?: return state
            if (nowMs < end) return state
            val following = state.next()
            val finishedAll = following == FocusState()
            state = if (finishedAll) following else following.copy(endsAtEpochMs = end + following.remainingMs)
        }
    }

    fun started(nowMs: Long): FocusState = copy(endsAtEpochMs = nowMs + remainingMs)

    fun paused(nowMs: Long): FocusState = copy(remainingMs = remainingAt(nowMs), endsAtEpochMs = null)

    fun restarted(): FocusState = copy(remainingMs = phaseLength, endsAtEpochMs = null)

    /** What comes after this phase, not yet running. After the last session it's back at the start. */
    fun next(): FocusState = when {
        !onBreak && session == FOCUS_SESSIONS -> FocusState()
        !onBreak -> FocusState(session, onBreak = true, remainingMs = FOCUS_BREAK_MS)
        else -> FocusState(session + 1, onBreak = false, remainingMs = FOCUS_WORK_MS)
    }
}
