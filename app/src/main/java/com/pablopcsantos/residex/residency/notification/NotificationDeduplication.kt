package com.pablopcsantos.residex.residency.notification

import java.time.LocalDate

object NotificationDeduplication {
    fun key(candidate: NotificationCandidate, repeat: AlertRepeat, today: LocalDate): String =
        if (repeat == AlertRepeat.DAILY_UNTIL) "${candidate.key}|$today" else candidate.key

    fun unsent(candidates: List<NotificationCandidate>, sentKeys: Set<String>, repeat: AlertRepeat, today: LocalDate): List<NotificationCandidate> =
        candidates.filter { key(it, repeat, today) !in sentKeys }
}