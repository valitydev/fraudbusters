package dev.vality.fraudbusters.service.dto;

import dev.vality.fraudbusters.constant.FilterField;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchFieldDto {

    private FilterField field;
    private FieldType type;
    private String value;
}
