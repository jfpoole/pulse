package com.zutubi.validation.annotations;

import com.zutubi.validation.validators.RequiredValidator;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <class-comment/>
 */
@Constraint(RequiredValidator.class)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Required
{
    public static final String DEFAULT_messageKey = "";

    public static final String DEFAULT_defaultMessage = "";

    public static final boolean DEFAULT_shortCircuit = true;

    public String messageKey() default DEFAULT_messageKey;

    public String defaultMessage() default DEFAULT_defaultMessage;

    public boolean shortCircuit() default DEFAULT_shortCircuit;
}