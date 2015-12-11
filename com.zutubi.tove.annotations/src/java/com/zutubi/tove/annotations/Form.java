package com.zutubi.tove.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)

/**
 * The form annotation allows for some level of customisation of the rendering of the form
 * that is generated for the annotated object.
 */
public @interface Form
{
    /**
     * Specify a custom ordering of the forms fields.
     *
     * Any fields not included in this ordering will be ordered arbitrarily.
     *
     * @return an array of field names defining the order in which they layed out
     * in the form.
     */
    String[] fieldOrder();

    /**
     * Specify the submit actions available for this form.
     * 
     * @return an array of strings representing the actions available for this form.
     */
    String[] actions() default { "save", "cancel" };

    // FIXME kendo deprecated
    int labelWidth() default 150;
}
