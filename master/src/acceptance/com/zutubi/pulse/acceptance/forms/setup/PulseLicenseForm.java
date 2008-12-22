package com.zutubi.pulse.acceptance.forms.setup;

import com.zutubi.pulse.acceptance.forms.BaseForm;
import net.sourceforge.jwebunit.WebTester;

/**
 * <class-comment/>
 */
public class PulseLicenseForm extends BaseForm
{
    public PulseLicenseForm(WebTester tester)
    {
        super(tester);
    }

    public String getFormName()
    {
        return "setup.license";
    }

    public String[] getFieldNames()
    {
        return new String[]{"license"};
    }

}