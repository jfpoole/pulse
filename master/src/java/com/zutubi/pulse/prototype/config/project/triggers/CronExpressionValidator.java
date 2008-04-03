package com.zutubi.pulse.prototype.config.project.triggers;

import com.zutubi.validation.ValidationException;
import com.zutubi.validation.validators.StringFieldValidatorSupport;
import org.quartz.CronTrigger;
import org.quartz.impl.calendar.BaseCalendar;

/**
 */
public class CronExpressionValidator extends StringFieldValidatorSupport
{
    public void validateStringField(String expression) throws ValidationException
    {
        try
        {
            new CronTrigger("triggerName", null, expression).computeFirstFireTime(new BaseCalendar());
        }
        catch (Exception e)
        {
            addErrorMessage(e.getMessage());
        }
    }
}