package dev.vality.fraudbusters.service.dto;

import dev.vality.damsel.fraudbusters.Refund;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HistoricalRefundsDto {

    private List<Refund> refunds;
    private String lastId;

}
