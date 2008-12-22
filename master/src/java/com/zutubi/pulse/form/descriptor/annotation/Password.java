package com.zutubi.pulse.form.descriptor.annotation;

import com.zutubi.pulse.form.FieldType;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <class-comment/>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)

@Field(fieldType = FieldType.PASSWORD)
public @interface Password
{
}