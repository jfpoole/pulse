package com.zutubi.validation.mock;

import com.zutubi.validation.annotations.Required;

/**
 * <class-comment/>
 */
public class MockAnimal
{
    private String head;

    @Required(defaultKeySuffix = "myrequired")
    public String getHead()
    {
        return head;
    }

    public void setHead(String head)
    {
        this.head = head;
    }
}
