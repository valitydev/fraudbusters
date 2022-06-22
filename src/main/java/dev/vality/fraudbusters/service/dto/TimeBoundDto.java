package dev.vality.fraudbusters.service.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class TimeBoundDto {

    private Instant left;
    private Instant right;
}
