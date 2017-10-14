package com.g2m.services.variables.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Added 3/1/2015.
 * 
 * @author Michael Borromeo
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface VariableDelegates {
	Class<?> value();
}
