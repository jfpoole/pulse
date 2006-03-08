package com.cinnamonbob.acceptance.forms;

import net.sourceforge.jwebunit.WebTester;

/**
 * <class-comment/>
 */
public class ProjectBasicForm extends BaseForm
{
    public static final String FORM_NAME = "project.edit";
    public static final String NAME_FIELD = "project.name";
    public static final String DESCRIPTION_FIELD = "project.description";

    public ProjectBasicForm(WebTester tester)
    {
        super(tester);
    }

    public String getFormName()
    {
        return FORM_NAME;
    }

    public String[] getFieldNames()
    {
        return new String[]{NAME_FIELD, DESCRIPTION_FIELD};
    }
}
