package com.voicefinance.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

@Slf4j
@Component
public class DateTimeTools {

    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("America/Sao_Paulo");
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    @Tool(description = "Gets the current date and time in the user's timezone (America/Sao_Paulo) in ISO-8601 format (e.g. 2026-10-05T14:30:00)")
    public String getCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now(DEFAULT_ZONE_ID);
        log.info("Tool getCurrentDateTime called -> {}", now);
        return now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    @Tool(description = "Gets the current date in YYYY-MM-DD format (e.g. 2026-10-05)")
    public String getCurrentDate() {
        LocalDate today = LocalDate.now(DEFAULT_ZONE_ID);
        log.info("Tool getCurrentDate called -> {}", today);
        return today.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    @Tool(description = "Gets the current time in HH:mm:ss format")
    public String getCurrentTime() {
        LocalTime now = LocalTime.now(DEFAULT_ZONE_ID);
        log.info("Tool getCurrentTime called -> {}", now);
        return now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    @Tool(description = "Gets the current day of the week in English (e.g. Monday, Tuesday)")
    public String getDayOfWeek() {
        LocalDate today = LocalDate.now(DEFAULT_ZONE_ID);
        String dayOfWeek = today.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, DEFAULT_LOCALE);
        log.info("Tool getDayOfWeek called -> {}", dayOfWeek);
        return dayOfWeek;
    }

    @Tool(description = "Calculates a relative date by adding or subtracting days from a base reference date (or current date if omitted). Returns in YYYY-MM-DD format")
    public String calculateRelativeDate(
            @ToolParam(description = "Number of days to add (positive) or subtract (negative), e.g. -1 for yesterday, 1 for tomorrow, -7 for last week") int daysToAdd,
            @ToolParam(description = "Base date in YYYY-MM-DD format (optional, uses current date if omitted or blank)") String baseDate) {
        LocalDate date = (baseDate != null && !baseDate.isBlank())
                ? LocalDate.parse(baseDate, DateTimeFormatter.ISO_LOCAL_DATE)
                : LocalDate.now(DEFAULT_ZONE_ID);
        LocalDate result = date.plusDays(daysToAdd);
        log.info("Tool calculateRelativeDate called: baseDate={}, daysToAdd={} -> {}", baseDate, daysToAdd, result);
        return result.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    @Tool(description = "Calculates the difference in days between two dates in YYYY-MM-DD format")
    public long daysBetween(
            @ToolParam(description = "Start date in YYYY-MM-DD format") String startDate,
            @ToolParam(description = "End date in YYYY-MM-DD format") String endDate) {
        LocalDate start = LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate end = LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE);
        long days = ChronoUnit.DAYS.between(start, end);
        log.info("Tool daysBetween called: start={}, end={} -> {}", startDate, endDate, days);
        return days;
    }

    @Tool(description = "Gets the first or last day of the month for a base reference date in YYYY-MM-DD format (optional)")
    public String getMonthBoundary(
            @ToolParam(description = "FIRST for the first day of the month or LAST for the last day of the month") String boundaryType,
            @ToolParam(description = "Base date in YYYY-MM-DD format (optional, uses current date if omitted or blank)") String baseDate) {
        LocalDate date = (baseDate != null && !baseDate.isBlank())
                ? LocalDate.parse(baseDate, DateTimeFormatter.ISO_LOCAL_DATE)
                : LocalDate.now(DEFAULT_ZONE_ID);

        LocalDate result = "FIRST".equalsIgnoreCase(boundaryType)
                ? date.with(TemporalAdjusters.firstDayOfMonth())
                : date.with(TemporalAdjusters.lastDayOfMonth());

        log.info("Tool getMonthBoundary called: boundaryType={}, baseDate={} -> {}", boundaryType, baseDate, result);
        return result.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
