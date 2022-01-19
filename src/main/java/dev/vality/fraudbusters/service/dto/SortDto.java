package dev.vality.fraudbusters.service.dto;

import dev.vality.fraudbusters.constant.SortOrder;
import lombok.Data;

@Data
public class SortDto {

    private SortOrder order;
    private String field;

}
