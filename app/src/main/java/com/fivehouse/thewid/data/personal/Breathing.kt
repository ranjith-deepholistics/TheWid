package com.fivehouse.thewid.data.personal

const val BREATHE_ROUNDS = 6

enum class BreathPhase(val seconds: Int) {
    Inhale(4),
    Hold(7),
    Exhale(8)
}

private val RoundMillis = BreathPhase.entries.sumOf { it.seconds } * 1_000L
val SessionMillis = RoundMillis * BREATHE_ROUNDS

/**
 * Where a session is at a moment in time.
 *
 * @property round from 1 to [BREATHE_ROUNDS].
 * @property secondsLeft whole seconds remaining in the phase, counting down to 1.
 * @property sessionProgress from 0 to 1 across the whole session.
 */
data class BreathState(
    val round: Int,
    val phase: BreathPhase,
    val secondsLeft: Int,
    val sessionProgress: Float,
    val isComplete: Boolean
)

fun breathStateAt(elapsedMillis: Long): BreathState {
    val elapsed = elapsedMillis.coerceAtLeast(0)
    if (elapsed >= SessionMillis) {
        return BreathState(BREATHE_ROUNDS, BreathPhase.Exhale, 0, 1f, isComplete = true)
    }
    val round = (elapsed / RoundMillis).toInt() + 1
    var withinRound = elapsed % RoundMillis
    for (phase in BreathPhase.entries) {
        val length = phase.seconds * 1_000L
        if (withinRound < length) {
            val left = ((length - withinRound + 999) / 1_000).toInt()
            return BreathState(round, phase, left, elapsed.toFloat() / SessionMillis, isComplete = false)
        }
        withinRound -= length
    }
    error("A round's phases cover its whole length.")
}
