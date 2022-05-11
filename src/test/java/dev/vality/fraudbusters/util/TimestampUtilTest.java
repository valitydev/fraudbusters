package dev.vality.fraudbusters.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimestampUtilTest {

    @Test
    void getStringDate() {
        long epochSeconds = 1627988400L;
        assertEquals("2021-08-03T11:00:00Z", TimestampUtil.getStringDate(epochSeconds));
    }

    @Test
    void generateTimestampWithoutTimeUnitMillis() {
        Instant now = Instant.now();

        Long timestamp = TimestampUtil.generateTimestampMinusTimeUnitsMillis(now, null, null);

        LocalDateTime result = LocalDateTime
                .ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
                .truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime current = LocalDateTime
                .ofInstant(now, ZoneId.systemDefault())
                .truncatedTo(ChronoUnit.SECONDS);

        assertEquals(current, result);
    }

    @Test
    void generateTimestampMinusDaysMillis() {
        Instant now = Instant.now();
        long value = 5L;
        ChronoUnit unit = ChronoUnit.DAYS;

        Long timestamp = TimestampUtil.generateTimestampMinusTimeUnitsMillis(now, value, unit);

        LocalDateTime result = LocalDateTime
                .ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
                .truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime current = LocalDateTime
                .ofInstant(now, ZoneId.systemDefault())
                .truncatedTo(ChronoUnit.SECONDS);

        assertEquals(current.minusDays(value), result);
    }

    @Test
    void generateTimestampMinusHoursMillis() {
        Instant now = Instant.now();
        long value = 5L;
        ChronoUnit unit = ChronoUnit.HOURS;

        Long timestamp = TimestampUtil.generateTimestampMinusTimeUnitsMillis(now, value, unit);

        LocalDateTime result = LocalDateTime
                .ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
                .truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime current = LocalDateTime
                .ofInstant(now, ZoneId.systemDefault())
                .truncatedTo(ChronoUnit.SECONDS);

        assertEquals(current.minusHours(value), result);
    }

    @Test
    void generateTimestampMinusMinutesMillis() {
        Instant now = Instant.now();
        long value = 60L;
        ChronoUnit unit = ChronoUnit.MINUTES;

        Long timestamp = TimestampUtil.generateTimestampMinusTimeUnitsMillis(now, value, unit);

        LocalDateTime result = LocalDateTime
                .ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
                .truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime current = LocalDateTime
                .ofInstant(now, ZoneId.systemDefault())
                .truncatedTo(ChronoUnit.SECONDS);

        assertEquals(current.minusMinutes(value), result);
    }
}
