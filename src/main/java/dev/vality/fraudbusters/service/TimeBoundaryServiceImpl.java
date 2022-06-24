package dev.vality.fraudbusters.service;

import dev.vality.fraudbusters.domain.TimeBound;
import dev.vality.fraudo.model.TimeWindow;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static dev.vality.fraudo.constant.TimeUnit.*;

@Service
public class TimeBoundaryServiceImpl implements TimeBoundaryService {

    @Override
    public TimeBound getBoundary(Instant target, TimeWindow timeWindow) {
        ChronoUnit chronoUnit = resolveTimeUnit(timeWindow.getTimeUnit());
        if (CALENDAR_MONTHS.equals(timeWindow.getTimeUnit())) {
            return buildCalendarMonthsTimeBound(target, timeWindow, chronoUnit);
        }
        Instant left = target.minus(timeWindow.getStart(), chronoUnit);
        Instant right = target.minus(timeWindow.getEnd(), chronoUnit);
        return TimeBound.builder()
                .left(left)
                .right(right)
                .build();
    }

    private ChronoUnit resolveTimeUnit(String timeUnit) {
        return switch (timeUnit) {
            case MINUTES -> ChronoUnit.MINUTES;
            case DAYS, CALENDAR_MONTHS -> ChronoUnit.DAYS;
            default -> ChronoUnit.HOURS;
        };
    }

    private TimeBound buildCalendarMonthsTimeBound(Instant target, TimeWindow timeWindow, ChronoUnit chronoUnit) {
        LocalDate targetDate = LocalDate.ofInstant(target, ZoneId.systemDefault());
        int start = calculateStart(timeWindow.getStart(), targetDate);
        int end = calculateEnd(timeWindow.getEnd(), targetDate);
        Instant left = targetDate
                .minus(start, chronoUnit)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .truncatedTo(ChronoUnit.DAYS);
        Instant right = target
                .minus(end, chronoUnit);
        return TimeBound.builder()
                .left(left)
                .right(right)
                .build();
    }

    private int calculateStart(int start, LocalDate targetDate) {
        int startInDays = targetDate.getDayOfMonth() - 1;
        for (int i = 1; i <= start - 1; i++) {
            startInDays += targetDate.minusMonths(i).lengthOfMonth();
        }
        return startInDays;
    }

    private int calculateEnd(int end, LocalDate targetDate) {
        int endInDays = 0;
        for (int i = 1; i <= end; i++) {
            endInDays += targetDate.minusMonths(i).lengthOfMonth();
        }
        return endInDays;
    }

}
