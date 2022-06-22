package dev.vality.fraudbusters.service;

import dev.vality.fraudbusters.service.dto.TimeBoundDto;
import dev.vality.fraudo.model.TimeWindow;

import java.time.Instant;

public interface TimeBoundaryService {

    TimeBoundDto getBoundary(Instant target, TimeWindow timeWindow);
}
