package dev.vality.fraudbusters.domain;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class TimeBound {

    private Instant left;
    private Instant right;
}
