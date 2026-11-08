package com.hyngng.gemtalk.ai

import com.hyngng.gemtalk.ui.i18n.tr
import java.time.ZonedDateTime

/** Date labels shared by the local date/time tool. */
object TimeContext {

    private val englishWeekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    fun dayLabel(time: ZonedDateTime, weekday: Boolean = true): String {
        val day = when (time.dayOfWeek.value) {
            1 -> tr("월", englishWeekdays[0])
            2 -> tr("화", englishWeekdays[1])
            3 -> tr("수", englishWeekdays[2])
            4 -> tr("목", englishWeekdays[3])
            5 -> tr("금", englishWeekdays[4])
            6 -> tr("토", englishWeekdays[5])
            else -> tr("일", englishWeekdays[6])
        }
        return if (weekday) {
            "%04d-%02d-%02d (%s)".format(time.year, time.monthValue, time.dayOfMonth, day)
        } else {
            "%04d-%02d-%02d".format(time.year, time.monthValue, time.dayOfMonth)
        }
    }
}
