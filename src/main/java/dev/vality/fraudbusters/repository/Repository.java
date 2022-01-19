package dev.vality.fraudbusters.repository;

import dev.vality.fraudbusters.service.dto.FilterDto;

import java.util.List;

public interface Repository<T> {

    void insert(T t);

    void insertBatch(List<T> batch);

    List<T> getByFilter(FilterDto filter);

}
