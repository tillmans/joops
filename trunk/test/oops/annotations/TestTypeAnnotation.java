package oops.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface TestTypeAnnotation {
    String value() default "";
    EnumInAnnotation enumref() default EnumInAnnotation.anything;
    @TestAnnotationAnnotation int another() default 0;
}
