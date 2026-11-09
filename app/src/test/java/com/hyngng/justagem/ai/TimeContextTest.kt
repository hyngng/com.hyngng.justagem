package com.hyngng.justagem.ai

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeContextTest {

    private val seoul = ZoneId.of("Asia/Seoul")

    @Test
    fun `day label supports date only and weekday forms`() {
        val time = Instant.parse("2026-10-30T05:32:00Z").atZone(seoul)

        assertEquals("2026-10-30 (금)", TimeContext.dayLabel(time))
        assertEquals("2026-10-30", TimeContext.dayLabel(time, weekday = false))
    }
}
