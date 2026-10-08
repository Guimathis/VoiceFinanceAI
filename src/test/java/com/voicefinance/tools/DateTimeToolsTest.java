package com.voicefinance.tools;

import com.voicefinance.ai.tools.DateTimeTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DateTimeToolsTest {

    private DateTimeTools dateTimeTools;

    @BeforeEach
    void setUp() {
        dateTimeTools = new DateTimeTools();
    }

    @Test
    void testGetCurrentDateTime() {
        String currentDateTime = dateTimeTools.getCurrentDateTime();
        assertNotNull(currentDateTime);
        assertDoesNotThrow(() -> java.time.LocalDateTime.parse(currentDateTime));
    }

    @Test
    void testGetCurrentDate() {
        String currentDate = dateTimeTools.getCurrentDate();
        assertNotNull(currentDate);
        assertDoesNotThrow(() -> LocalDate.parse(currentDate));
    }

    @Test
    void testGetCurrentTime() {
        String currentTime = dateTimeTools.getCurrentTime();
        assertNotNull(currentTime);
        assertTrue(currentTime.matches("\\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    void testGetDayOfWeek() {
        String dayOfWeek = dateTimeTools.getDayOfWeek();
        assertNotNull(dayOfWeek);
        assertFalse(dayOfWeek.isBlank());
    }

    @Test
    void testCalculateRelativeDate() {
        String result = dateTimeTools.calculateRelativeDate(5, "2026-10-05");
        assertEquals("2026-10-10", result);

        String resultPast = dateTimeTools.calculateRelativeDate(-5, "2026-10-05");
        assertEquals("2026-09-30", resultPast);

        String resultCurrent = dateTimeTools.calculateRelativeDate(1, null);
        assertNotNull(resultCurrent);
    }

    @Test
    void testDaysBetween() {
        long days = dateTimeTools.daysBetween("2026-10-01", "2026-10-05");
        assertEquals(4, days);

        long negativeDays = dateTimeTools.daysBetween("2026-10-05", "2026-10-01");
        assertEquals(-4, negativeDays);
    }

    @Test
    void testGetMonthBoundary() {
        String firstDay = dateTimeTools.getMonthBoundary("FIRST", "2026-10-15");
        assertEquals("2026-10-01", firstDay);

        String lastDay = dateTimeTools.getMonthBoundary("LAST", "2026-10-15");
        assertEquals("2026-10-31", lastDay);

        String febLastDay = dateTimeTools.getMonthBoundary("LAST", "2024-02-10");
        assertEquals("2024-02-29", febLastDay);
    }
}
