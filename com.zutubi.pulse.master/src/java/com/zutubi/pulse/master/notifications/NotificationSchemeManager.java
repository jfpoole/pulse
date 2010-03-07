package com.zutubi.pulse.master.notifications;

import java.util.List;

/**
 * <class-comment/>
 */
public interface NotificationSchemeManager
{
    /**
     * Retrieve the list of supported notification schemes.
     *
     * @return
     */
    List<String> getNotificationSchemes();

    /**
     * Retrieve the notification handler associated with the specified scheme.
     *
     * @param scheme
     *
     * @return
     *
     * @see #getNotificationSchemes() for the list of available schemes.
     */
    Class<? extends NotificationHandler> getNotificationHandler(String scheme);
}