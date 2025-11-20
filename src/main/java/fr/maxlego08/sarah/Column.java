package fr.maxlego08.sarah;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Column {
    String value();

    boolean primary() default false;

    boolean autoIncrement() default false;

    boolean foreignKey() default false;

    String foreignKeyReference() default "";

    String type() default "";

    boolean nullable() default false;

    boolean unique() default false;
}
