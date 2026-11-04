package com.fivehouse.thewid.data.status

import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

sealed interface AlarmStatus {
    data object None : AlarmStatus

    data class Scheduled(val at: Instant) : AlarmStatus
}

interface AlarmSource {
    /** Next alarm trigger times in epoch milliseconds, or null when none is set. */
    val nextTriggerMillis: Flow<Long?>
}

class AlarmRepository(source: AlarmSource) {
    val status: Flow<AlarmStatus> = source.nextTriggerMillis
        .map { millis ->
            if (millis == null) AlarmStatus.None else AlarmStatus.Scheduled(Instant.ofEpochMilli(millis))
        }
        .distinctUntilChanged()
}
