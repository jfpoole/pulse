package com.zutubi.pulse.security;

import org.acegisecurity.Authentication;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import org.acegisecurity.userdetails.UserDetails;

/**
 * <class-comment/>
 */
public class AcegiUtils
{
    /**
     * A utility method to 'log in' the specified user. After this call, all authentication
     * requests will be made against the new user.
     *
     * @param targetUser
     */
    public static void loginAs(UserDetails targetUser)
    {
        UsernamePasswordAuthenticationToken targetUserRequest =
                new UsernamePasswordAuthenticationToken(targetUser, targetUser.getPassword(), targetUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(targetUserRequest);
    }

    /**
     * Returns the username for the currently-logged-in User.
     *
     * @return the logged in user's login name, or null if there is no
     *         logged in user
     */
    public static String getLoggedInUser()
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null)
        {
            Object principle = authentication.getPrincipal();
            if (principle instanceof UserDetails)
            {
                return ((UserDetails)principle).getUsername();
            }
        }
        return null;
    }

    /**
     * Tests if the logged in user has been granted the given role.
     *
     * @param role the role to test for
     * @return true iff there is a logged in user who has been granted the
     * given role
     */
    public static boolean userHasRole(String role)
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication != null)
        {
            Object principle = authentication.getPrincipal();
            if(principle instanceof UserDetails)
            {
                UserDetails details = (UserDetails)principle;
                for(GrantedAuthority authority: details.getAuthorities())
                {
                    if(authority.getAuthority().equals(role))
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
