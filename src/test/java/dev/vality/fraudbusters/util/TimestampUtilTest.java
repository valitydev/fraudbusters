package dev.vality.fraudbusters.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimestampUtilTest {

    @Test
    void getStringDate() {
        long epochSeconds = 1627988400L;
        assertEquals("2021-08-03T11:00:00Z", TimestampUtil.getStringDate(epochSeconds));
    }
}
