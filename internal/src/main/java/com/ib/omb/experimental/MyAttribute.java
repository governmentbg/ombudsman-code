package com.ib.omb.experimental;

import static java.lang.annotation.ElementType.FIELD;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(FIELD)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface MyAttribute {
	String label() default "none";
	String defaultText() default "none";
}
