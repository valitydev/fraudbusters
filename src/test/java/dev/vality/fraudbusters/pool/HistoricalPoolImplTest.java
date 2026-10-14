package dev.vality.fraudbusters.pool;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HistoricalPoolImplTest {

    public static final String TEST = "Test";
    public static final String ID_REFERENCE = "id_reference-";
    private static final long BASE_TIMESTAMP = 10_000L;
    private static final long TIME_STEP = 100L;
    HistoricalPoolImpl<String> timePool = new HistoricalPoolImpl<>("time-pool");

    @Test
    void add() {
        initTimePool(40);

        String result = timePool.get(TEST, timestamp(30) + 1);
        Assertions.assertEquals(ID_REFERENCE + 30, result);

        Assertions.assertEquals(ID_REFERENCE + 29, timePool.get(TEST, timestamp(30)));

        result = timePool.get(TEST, timestamp(40));
        Assertions.assertEquals(ID_REFERENCE + 39, result);

        Assertions.assertNull(timePool.get(TEST, 0L));
        Assertions.assertNull(timePool.get(TEST, timestamp(0)));
    }

    private void initTimePool(int size) {
        for (int count = 0; count < size; count++) {
            timePool.add(TEST, timestamp(count), ID_REFERENCE + count);
        }
    }

    private long timestamp(int index) {
        return BASE_TIMESTAMP + index * TIME_STEP;
    }

    @Test
    void cleanTimePool() {
        initTimePool(20);
        Assertions.assertEquals(20, timePool.deepSize());

        timePool.cleanUntil(TEST, timestamp(11));
        Assertions.assertEquals(9, timePool.deepSize());
        Assertions.assertFalse(timePool.contains(TEST, timestamp(10)));
        Assertions.assertTrue(timePool.contains(TEST, timestamp(11)));
        Assertions.assertEquals(ID_REFERENCE + 11, timePool.get(TEST, timestamp(11) + 1));
    }
}
