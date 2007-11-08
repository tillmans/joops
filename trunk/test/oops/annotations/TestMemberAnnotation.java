package oops.annotations;

import java.lang.annotation.RetentionPolicy;

import java.lang.annotation.Retention;

@Retention(RetentionPolicy.RUNTIME)
public @interface TestMemberAnnotation {
    Class<?>[] value();
}
