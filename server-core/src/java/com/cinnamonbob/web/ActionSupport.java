package com.cinnamonbob.web;

import com.cinnamonbob.xwork.interceptor.Cancelable;
import org.acegisecurity.context.SecurityContextHolder;

/**
 * 
 *
 */
public class ActionSupport extends com.opensymphony.xwork.ActionSupport implements Cancelable
{
    private String cancel;

    public boolean isCancelled()
    {
        return cancel != null;
    }

    public void setCancel(String name)
    {
        this.cancel = name;
    }

    public Object getPrinciple()
    {
        return SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public String trimmedString(String s, int length)
    {
        if(s.length() > length)
        {
            return s.substring(0, length - 3) + "...";
        }

        return s;
    }
}
