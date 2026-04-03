package fr.maxlego08.sarah;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks a field as transient for schema/constructor mapping.
 * Fields annotated with this will be ignored by schema generation and mapping logic.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface TransientField {
}

