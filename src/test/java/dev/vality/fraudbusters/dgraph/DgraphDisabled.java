package dev.vality.fraudbusters.dgraph;

import org.junit.jupiter.api.Disabled;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Disabled("Dgraph is no longer used")
public @interface DgraphDisabled {
}
