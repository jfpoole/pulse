package com.cinnamonbob.acceptance.forms.setup;

import com.cinnamonbob.acceptance.forms.BaseForm;
import net.sourceforge.jwebunit.WebTester;

/**
 * <class-comment/>
 */
public class CreateAdminForm extends BaseForm
{
    public CreateAdminForm(WebTester tester)
    {
        super(tester);
    }

    public String getFormName()
    {
        return "admin.create";
    }

    public String[] getFieldNames()
    {
        return new String[]{"bobHome"};
    }
}
