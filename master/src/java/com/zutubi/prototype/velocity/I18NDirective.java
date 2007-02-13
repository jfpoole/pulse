package com.zutubi.prototype.velocity;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.MethodInvocationException;

import java.io.Writer;
import java.io.IOException;
import java.util.Map;

import com.zutubi.pulse.i18n.Messages;
import com.zutubi.pulse.prototype.TemplateRecord;
import com.opensymphony.util.TextUtils;

/**
 *
 *
 */
public class I18NDirective extends PrototypeDirective
{
    private String key;

    public String getName()
    {
        return "i18n";
    }

    public int getType()
    {
        return LINE;
    }

    public boolean render(InternalContextAdapter context, Writer writer, Node node) throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException
    {
        Map params = createPropertyMap(context, node);
        wireParams(params);

        String symbolicName = projectConfigurationManager.getSymbolicName(path);
        TemplateRecord record = projectConfigurationManager.getRecord(path);
        if (record != null)
        {
            symbolicName = record.getSymbolicName();
        }
        
        Messages messages = Messages.getInstance(recordTypeRegistry.getType(symbolicName));

        String value = messages.format(this.key);
        if (!TextUtils.stringSet(value))
        {
            value = key;
        }
        
        writer.write(value);
        
        return true;
    }

    public void setKey(String key)
    {
        this.key = key;
    }
}
