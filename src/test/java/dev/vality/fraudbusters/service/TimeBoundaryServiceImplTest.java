package dev.vality.fraudbusters.service;

import dev.vality.fraudo.model.TimeWindow;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeBoundaryServiceImplTest {

    private TimeBoundaryService timeBoundaryService = new TimeBoundaryServiceImpl();

    @Test
    void getBoundaryWithEmptyTimeWindow() {
        Instant now = Instant.now();

        var timeBound = timeBoundaryService.getBoundary(now, TimeWindow.builder().build());

        assertEquals(now.truncatedTo(ChronoUnit.SECONDS), timeBound.getLeft().truncatedTo(ChronoUnit.SECONDS));
        assertEquals(now.truncatedTo(ChronoUnit.SECONDS), timeBound.getRight().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    void getBoundaryWithOnlyStartTime() {
        Instant now = Instant.now();
        long startValue = 5L;
        TimeWindow timeWindow = TimeWindow.builder()
                .timeUnit(ChronoUnit.MINUTES)
                .startWindowTime(startValue)
                .build();

        var timeBound = timeBoundaryService.getBoundary(now, timeWindow);

        assertEquals(now.minus(startValue, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS),
                timeBound.getLeft().truncatedTo(ChronoUnit.SECONDS));
        assertEquals(now.truncatedTo(ChronoUnit.SECONDS), timeBound.getRight().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    void getBoundaryWithDaysUnit() {
        Instant now = Instant.now();
        long startValue = 5L;
        long endValue = 2L;
        TimeWindow timeWindow = TimeWindow.builder()
                .timeUnit(ChronoUnit.DAYS)
                .startWindowTime(startValue)
                .endWindowTime(endValue)
                .build();

        var timeBound = timeBoundaryService.getBoundary(now, timeWindow);

        assertEquals(now.minus(startValue, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS),
                timeBound.getLeft().truncatedTo(ChronoUnit.SECONDS));
        assertEquals(now.minus(endValue, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS),
                timeBound.getRight().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    void getBoundaryWithHoursUnit() {
        Instant now = Instant.now();
        long startValue = 5L;
        long endValue = 2L;
        TimeWindow timeWindow = TimeWindow.builder()
                .timeUnit(ChronoUnit.HOURS)
                .startWindowTime(startValue)
                .endWindowTime(endValue)
                .build();

        var timeBound = timeBoundaryService.getBoundary(now, timeWindow);

        assertEquals(now.minus(startValue, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS),
                timeBound.getLeft().truncatedTo(ChronoUnit.SECONDS));
        assertEquals(now.minus(endValue, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS),
                timeBound.getRight().truncatedTo(ChronoUnit.SECONDS));
    }

}