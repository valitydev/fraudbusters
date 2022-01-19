package dev.vality.fraudbusters.stream;

import java.util.Map;

public interface TemplateListVisitor<T, U> {

    Map<String, U> visit(T t);

}
