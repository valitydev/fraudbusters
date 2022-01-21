package dev.vality.fraudbusters.service.dto;

import dev.vality.fraudbusters.constant.PaymentField;
import lombok.Data;

import java.util.Map;

@Data
public class FilterDto {

    private static final Long DEFAULT_PAGE_SIZE = 10L;

    private String lastId;
    private Long size = DEFAULT_PAGE_SIZE;
    private String timeFrom;
    private String timeTo;
    private Map<PaymentField, String> searchPatterns;
    private SortDto sort;

}
