package dev.vality.fraudbusters.service.dto;

import dev.vality.fraudbusters.constant.PaymentField;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchFieldDto {

    private PaymentField field;
    private FieldType type;
    private String value;
}
