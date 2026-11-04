package com.fivehouse.thewid.data.status

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

sealed interface AlertsStatus {
    data object NoAccess : AlertsStatus

    data class Count(val count: Int) : AlertsStatus
}

interface AlertsSource {
    /** How many notifications are waiting, or null without notification access. */
    val counts: Flow<Int?>
}

class AlertsRepository(source: AlertsSource) {
    val status: Flow<AlertsStatus> = source.counts
        .map { count -> if (count == null) AlertsStatus.NoAccess else AlertsStatus.Count(count.coerceAtLeast(0)) }
        .distinctUntilChanged()
}

/**
 * Whether a notification counts as an alert. Ongoing ones like music or navigation aren't waiting
 * for anyone, group summaries would count a conversation twice, and our own shouldn't count.
 */
fun countsAsAlert(isOngoing: Boolean, isClearable: Boolean, isGroupSummary: Boolean, isOwn: Boolean): Boolean =
    !isOngoing && isClearable && !isGroupSummary && !isOwn
