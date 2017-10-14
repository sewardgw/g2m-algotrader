package com.g2m.services.variables.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Added 6/9/2015.
 * 
 * @author Michael Borromeo
 */
@Retention(RetentionPolicy.RUNTIME)
@Document(collection = "variables")
public @interface VariableEntity {
}
