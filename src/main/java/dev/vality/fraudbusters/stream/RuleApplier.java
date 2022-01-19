package dev.vality.fraudbusters.stream;

import dev.vality.fraudbusters.domain.CheckedResultModel;

import java.util.List;
import java.util.Optional;

public interface RuleApplier<T> {

    Optional<CheckedResultModel> apply(T model, String templateKey);

    Optional<CheckedResultModel> applyForAny(T model, List<String> templateKeys);

}
