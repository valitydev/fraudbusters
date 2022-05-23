package dev.vality.fraudbusters.service.dto;

import lombok.Data;

import java.util.Set;

@Data
public class FilterDto {

    private static final Long DEFAULT_PAGE_SIZE = 10L;

    private String lastId;
    private Long size = DEFAULT_PAGE_SIZE;
    private String timeFrom;
    private String timeTo;
    private Set<SearchFieldDto> searchFields;
    private SortDto sort;

}
