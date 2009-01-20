package com.zutubi.tove.squeezer.squeezers;

import com.zutubi.tove.squeezer.SqueezeException;
import com.zutubi.tove.squeezer.TypeSqueezer;
import com.zutubi.util.TextUtils;

/**
 * <class-comment/>
 */
public class ByteSqueezer implements TypeSqueezer
{
    public String squeeze(Object obj) throws SqueezeException
    {
        if (obj == null)
        {
            return "";
        }
        return obj.toString();
    }

    public Object unsqueeze(String... str) throws SqueezeException
    {
        String s = str[0];
        if (TextUtils.stringSet(s))
        {
            try
            {
                return Byte.parseByte(s);
            }
            catch (NumberFormatException e)
            {
                throw new SqueezeException(String.format("'%s' is not a valid byte value", s));
            }
        }
        else
        {
            return null;
        }
    }
}