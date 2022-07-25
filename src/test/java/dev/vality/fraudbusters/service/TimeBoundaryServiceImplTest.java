package dev.vality.fraudbusters.service;

import dev.vality.fraudo.model.TimeWindow;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static dev.vality.fraudo.constant.TimeUnit.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeBoundaryServiceImplTest {

    private final TimeBoundaryService timeBoundaryService = new TimeBoundaryServiceImpl();

    @Test
    void getBoundaryWithOnlyStartTime() {
        Instant now = Instant.now();
        int startValue = 5;
        TimeWindow timeWindow = TimeWindow.builder()
                .timeUnit(MINUTES)
                .start(5)
                .build();

        var timeBound = timeBoundaryService.getBoundary(now, timeWindow);

        assertEquals(now.minus(startValue, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS),
                timeBound.getLeft());
        assertEquals(now.truncatedTo(ChronoUnit.SECONDS), timeBound.getRight());
    }

    @Test
    void getBoundaryWithDaysUnit() {
        Instant now = Instant.now();
        int startValue = 5;
        int endValue = 2;
        TimeWindow timeWindow = TimeWindow.builder()
                .timeUnit(DAYS)
                .start(startValue)
                .end(endValue)
                .build();

        var timeBound = timeBoundaryService.getBoundary(now, timeWindow);

        assertEquals(now.minus(startValue, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS),
                timeBound.getLeft());
        assertEquals(now.minus(endValue, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS),
                timeBound.getRight());
    }

    @Test
    void getBoundaryWithHoursUnit() {
        Instant now = Instant.now();
        int startValue = 5;
        int endValue = 2;
        TimeWindow timeWindow = TimeWindow.builder()
                .timeUnit(HOURS)
                .start(startValue)
                .end(endValue)
                .build();

        var timeBound = timeBoundaryService.getBoundary(now, timeWindow);

        assertEquals(now.minus(startValue, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS),
                timeBound.getLeft());
        assertEquals(now.minus(endValue, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS),
                timeBound.getRight());
    }

    @Test
    void withOneCalMonthsTimeUnitTest() {
        Instant now = Instant.now();
        int startValue = 1;
        TimeWindow timeWindow = TimeWindow.builder()
                .timeUnit(CALENDAR_MONTHS)
                .start(startValue)
                .build();

        var timeBound = timeBoundaryService.getBoundary(now, timeWindow);

        LocalDate dateNow = LocalDate.ofInstant(now, ZoneOffset.UTC);
        assertEquals(now.minus(dateNow.getDayOfMonth() - 1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
                timeBound.getLeft());
        assertEquals(now.truncatedTo(ChronoUnit.SECONDS), timeBound.getRight().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    void withThreeCalMonthsTimeUnitTest() {
        Instant now = Instant.now();
        int startValue = 3;
        TimeWindow timeWindow = TimeWindow.builder()
                .timeUnit(CALENDAR_MONTHS)
                .start(startValue)
                .build();

        var timeBound = timeBoundaryService.getBoundary(now, timeWindow);

        LocalDate dateNow = LocalDate.ofInstant(now, ZoneOffset.UTC);
        int leftForThreeCalMonths = dateNow.getDayOfMonth() - 1 + dateNow.minusMonths(1).lengthOfMonth() +
                dateNow.minusMonths(2).lengthOfMonth();
        assertEquals(now.minus(leftForThreeCalMonths, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
                timeBound.getLeft());
        assertEquals(now.truncatedTo(ChronoUnit.SECONDS), timeBound.getRight().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    void withCalMonthsTimeUnitAndWithEndTimeTest() {
        Instant now = Instant.now();
        int startValue = 4;
        int endValue = 2;
        TimeWindow timeWindow = TimeWindow.builder()
                .timeUnit(CALENDAR_MONTHS)
                .start(startValue)
                .end(endValue)
                .build();

        var timeBound = timeBoundaryService.getBoundary(now, timeWindow);

        LocalDate dateNow = LocalDate.ofInstant(now, ZoneOffset.UTC);
        int startForFourCalMonths = dateNow.getDayOfMonth() - 1 + dateNow.minusMonths(1).lengthOfMonth() +
                dateNow.minusMonths(2).lengthOfMonth() + dateNow.minusMonths(3).lengthOfMonth();
        int endForTwoCalMonths = dateNow.minusMonths(1).lengthOfMonth() + dateNow.minusMonths(2).lengthOfMonth();
        assertEquals(now.minus(startForFourCalMonths, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
                timeBound.getLeft());
        assertEquals(now.minus(endForTwoCalMonths, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS),
                timeBound.getRight().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    void withOneCalDaysTimeUnitTest() {
        Instant now = Instant.now();
        int startValue = 1;
        TimeWindow timeWindow = TimeWindow.builder()
                .timeUnit(CALENDAR_DAYS)
                .start(startValue)
                .build();

        var timeBound = timeBoundaryService.getBoundary(now, timeWindow);

        LocalDateTime dateTimeNow = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        assertEquals(now.truncatedTo(ChronoUnit.DAYS),
                timeBound.getLeft());
        Instant expectedRight = dateTimeNow
                .withHour(23)
                .withMinute(59)
                .withSecond(59)
                .truncatedTo(ChronoUnit.SECONDS)
                .toInstant(ZoneOffset.UTC);
        assertEquals(expectedRight, timeBound.getRight());
    }

    @Test
    void withThreeCalDaysTimeUnitTest() {
        Instant now = Instant.now();
        int startValue = 3;
        TimeWindow timeWindow = TimeWindow.builder()
                .timeUnit(CALENDAR_DAYS)
                .start(startValue)
                .build();

        var timeBound = timeBoundaryService.getBoundary(now, timeWindow);

        LocalDateTime dateTimeNow = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        assertEquals(now.minus(startValue - 1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
                timeBound.getLeft());
        Instant expectedRight = dateTimeNow
                .withHour(23)
                .withMinute(59)
                .withSecond(59)
                .truncatedTo(ChronoUnit.SECONDS)
                .toInstant(ZoneOffset.UTC);
        assertEquals(expectedRight, timeBound.getRight());
    }

    @Test
    void withCalDaysTimeUnitAndWithEndTimeTest() {
        Instant now = Instant.now();
        int startValue = 6;
        int endValue = 2;
        TimeWindow timeWindow = TimeWindow.builder()
                .timeUnit(CALENDAR_DAYS)
                .start(startValue)
                .end(endValue)
                .build();

        var timeBound = timeBoundaryService.getBoundary(now, timeWindow);

        LocalDateTime dateTimeNow = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        assertEquals(now.minus(startValue - 1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
                timeBound.getLeft());
        Instant expectedRight = dateTimeNow
                .minus(endValue - 1, ChronoUnit.DAYS)
                .withHour(23)
                .withMinute(59)
                .withSecond(59)
                .truncatedTo(ChronoUnit.SECONDS)
                .toInstant(ZoneOffset.UTC);
        assertEquals(expectedRight, timeBound.getRight());
    }

}