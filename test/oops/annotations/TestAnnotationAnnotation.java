package oops.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface TestAnnotationAnnotation {
    public @interface NestedAnnotation {
        
    }
}
