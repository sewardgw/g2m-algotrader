package com.g2m.services.tradingservices.annotations;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Added 6/9/2015.
 * 
 * @author Michael Borromeo
 */
@Retention(RetentionPolicy.RUNTIME)
@Document(collection = "ticks")
public @interface TickEntity {
}
