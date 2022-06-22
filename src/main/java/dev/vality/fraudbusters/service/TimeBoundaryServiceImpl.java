package dev.vality.fraudbusters.service;

import dev.vality.fraudbusters.service.dto.TimeBoundDto;
import dev.vality.fraudo.model.TimeWindow;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class TimeBoundaryServiceImpl implements TimeBoundaryService {

    @Override
    public TimeBoundDto getBoundary(Instant target, TimeWindow timeWindow) {
        Instant left = calculateTimeBoundary(target, timeWindow.getStartWindowTime(), timeWindow.getTimeUnit());
        Instant right = calculateTimeBoundary(target, timeWindow.getEndWindowTime(), timeWindow.getTimeUnit());
        return TimeBoundDto.builder()
                .left(left)
                .right(right)
                .build();
    }

    private Instant calculateTimeBoundary(Instant target, Long timeInterval, ChronoUnit timeUnit) {
        return timeInterval != null ? target.minus(timeInterval, timeUnit) : target;
    }


}
