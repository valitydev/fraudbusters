package dev.vality.fraudbusters.service.dto;

import dev.vality.fraudbusters.domain.CheckedPayment;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class HistoricalPaymentsDto {

    private List<? extends CheckedPayment> payments;
    private String lastId;

}
