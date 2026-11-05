package org.kastoriadev.unicalendar.domain.model

import androidx.annotation.StringRes
import org.kastoriadev.unicalendar.R

enum class EventType(@StringRes val displayNameRes: Int) {
    LECTURE(R.string.event_type_lecture),
    LAB(R.string.event_type_lab),
    EXAM(R.string.event_type_exam),
    DEADLINE(R.string.event_type_deadline),
    GENERAL(R.string.event_type_general)
}
