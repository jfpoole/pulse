package com.zutubi.pulse.master.tove.handler;

import com.zutubi.pulse.master.rest.model.forms.FieldModel;
import com.zutubi.pulse.master.tove.model.Descriptor;
import com.zutubi.tove.type.CompositeType;
import com.zutubi.tove.type.TypeProperty;

import java.lang.annotation.Annotation;

/**
 * Tags fields for properties marked @Wizard.Ignore with an extended parameter.
 */
public class WizardIgnoreAnnotationHandler implements AnnotationHandler
{
    @Override
    public void process(CompositeType annotatedType, Annotation annotation, Descriptor descriptor) throws Exception
    {
        // FIXME kendo remove this old version
    }

    @Override
    public void process(CompositeType annotatedType, TypeProperty property, Annotation annotation, FieldModel field) throws Exception
    {
        field.addParameter("wizardIgnore", true);
    }
}