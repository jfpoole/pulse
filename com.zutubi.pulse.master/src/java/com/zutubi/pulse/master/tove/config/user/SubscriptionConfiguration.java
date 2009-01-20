package com.zutubi.pulse.master.tove.config.user;

import com.zutubi.pulse.master.model.BuildResult;
import com.zutubi.pulse.master.tove.config.user.contacts.ContactConfiguration;
import com.zutubi.tove.annotations.Classification;
import com.zutubi.tove.annotations.Reference;
import com.zutubi.tove.annotations.SymbolicName;
import com.zutubi.tove.annotations.Table;
import com.zutubi.tove.config.api.AbstractNamedConfiguration;
import com.zutubi.validation.annotations.Required;

/**
 * Abstract base for subscriptions.
 */
@SymbolicName("zutubi.subscriptionConfig")
@Table(columns = {"name"})
@Classification(collection = "favourites")
public abstract class SubscriptionConfiguration extends AbstractNamedConfiguration
{
    @Required
    @Reference
    private ContactConfiguration contact;

    public ContactConfiguration getContact()
    {
        return contact;
    }

    public void setContact(ContactConfiguration contact)
    {
        this.contact = contact;
    }

    public abstract boolean conditionSatisfied(BuildResult buildResult);
    public abstract String getTemplate();
}