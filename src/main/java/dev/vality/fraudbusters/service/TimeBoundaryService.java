package dev.vality.fraudbusters.service;

import dev.vality.fraudbusters.domain.TimeBound;
import dev.vality.fraudo.model.TimeWindow;

import java.time.Instant;

public interface TimeBoundaryService {

    TimeBound getBoundary(Instant target, TimeWindow timeWindow);
}
