package dev.vality.fraudbusters.service.dto;

import dev.vality.fraudbusters.domain.Event;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HistoricalFraudResultsDto {

    private List<Event> fraudResults;
    private String lastId;

}
