package com.zutubi.pulse.web.project;

import com.opensymphony.util.TextUtils;
import com.zutubi.pulse.core.*;
import com.zutubi.pulse.model.CustomProjectValidationPredicate;
import com.zutubi.pulse.model.CustomPulseFileDetails;
import com.zutubi.pulse.model.PulseFileDetails;
import com.zutubi.pulse.util.StringUtils;
import com.zutubi.pulse.xwork.interceptor.Preparable;

import java.io.ByteArrayInputStream;

/**
 *
 *
 */
public class EditCustomDetailsAction extends AbstractEditDetailsAction implements Preparable
{
    private CustomPulseFileDetails details = new CustomPulseFileDetails();
    private ResourceRepository resourceRepository;

    public void prepare()
    {
        details = getPulseFileDetailsDao().findByIdAndType(getId(), CustomPulseFileDetails.class);
    }

    public PulseFileDetails getDetails()
    {
        return details;
    }

    public void validate()
    {
        super.validate();
        if(hasErrors())
        {
            return;
        }

        if(!TextUtils.stringSet(details.getPulseFile()))
        {
            addFieldError("details.pulseFile", "pulse file is required");
            return;
        }

        try
        {
            PulseFileLoader loader = new PulseFileLoader(new ObjectFactory());
            loader.load(new ByteArrayInputStream(details.getPulseFile().getBytes()), new PulseFile(), new Scope(), resourceRepository, new CustomProjectValidationPredicate());
        }
        catch(ParseException pe)
        {
            addActionError(pe.getMessage());
            if(pe.getLine() > 0)
            {
                String line = StringUtils.getLine(details.getPulseFile(), pe.getLine());
                if(line != null)
                {
                    addActionError("First line of offending element: " + line);
                }
            }
        }
        catch(Exception e)
        {
            addActionError(e.getMessage());
        }
    }

    public void setResourceRepository(ResourceRepository resourceRepository)
    {
        this.resourceRepository = resourceRepository;
    }
}
