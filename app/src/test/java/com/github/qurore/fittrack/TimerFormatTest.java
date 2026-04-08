package com.github.qurore.fittrack;

import static org.junit.Assert.assertEquals;

import com.github.qurore.fittrack.services.TimerForegroundService;

import org.junit.Test;

public class TimerFormatTest {

    @Test
    public void testFormatTime_zero_withCentiseconds() {
        assertEquals("00:00:00.00", TimerForegroundService.formatTime(0, true));
    }

    @Test
    public void testFormatTime_zero_withoutCentiseconds() {
        assertEquals("00:00:00", TimerForegroundService.formatTime(0, false));
    }

    @Test
    public void testFormatTime_oneMinute_withCentiseconds() {
        assertEquals("00:01:00.00", TimerForegroundService.formatTime(60000, true));
    }

    @Test
    public void testFormatTime_oneHour_withoutCentiseconds() {
        assertEquals("01:00:00", TimerForegroundService.formatTime(3600000, false));
    }

    @Test
    public void testFormatTime_complex_withCentiseconds() {
        long millis = (1 * 3600 + 23 * 60 + 45) * 1000L + 670;
        assertEquals("01:23:45.67", TimerForegroundService.formatTime(millis, true));
    }

    @Test
    public void testFormatTime_complex_withoutCentiseconds() {
        long millis = (2 * 3600 + 30 * 60 + 15) * 1000L;
        assertEquals("02:30:15", TimerForegroundService.formatTime(millis, false));
    }

    @Test
    public void testFormatTime_centisecondsOnly() {
        assertEquals("00:00:00.50", TimerForegroundService.formatTime(500, true));
    }

    @Test
    public void testFormatTime_largeValue() {
        long millis = (99 * 3600 + 59 * 60 + 59) * 1000L;
        assertEquals("99:59:59", TimerForegroundService.formatTime(millis, false));
    }
}
