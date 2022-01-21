package dev.vality.fraudbusters.stream;

@FunctionalInterface
public interface TemplateVisitor<T, U> {

    U visit(T t);

}
