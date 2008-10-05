package com.zutubi.pulse.master.webwork.interceptor;

import com.opensymphony.xwork.ActionInvocation;
import com.opensymphony.xwork.ActionProxy;
import com.zutubi.pulse.master.util.monitor.Monitor;
import com.zutubi.pulse.master.web.restore.ExecuteRestoreAction;

/**
 *
 *
 */
public class ExecAndWaitInterceptor extends com.opensymphony.webwork.interceptor.ExecuteAndWaitInterceptor
{
    public String intercept(ActionInvocation actionInvocation) throws Exception
    {
        ActionProxy proxy = actionInvocation.getProxy();
        
        Object action = proxy.getAction();

        Monitor monitor = ((ExecuteRestoreAction)action).getMonitor();

        if (monitor.isFinished())
        {
            return "success";
        }

        return super.intercept(actionInvocation);
    }
}
