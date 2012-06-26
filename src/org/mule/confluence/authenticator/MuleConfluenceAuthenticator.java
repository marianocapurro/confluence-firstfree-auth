package org.mule.confluence.authenticator;

import java.security.Principal;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;


import com.atlassian.crowd.integration.seraph.v22.ConfluenceAuthenticator;
import com.atlassian.seraph.auth.DefaultAuthenticator;

public class MuleConfluenceAuthenticator extends ConfluenceAuthenticator
{
    private static final Logger LOG = Logger.getLogger(MuleConfluenceAuthenticator.class);

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * 
     * @see com.atlassian.confluence.user.ConfluenceAuthenticator#getUser(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public Principal getUser(HttpServletRequest request, HttpServletResponse response)
    {
        if(LOG.isDebugEnabled())
        {
            LOG.debug(dumpRequest(request));
        }
        
        Principal user = super.getUser(request, response);
        
        if(user != null && !isAnonymousUser(user))
        {
            // Don't wait a second. A user is logged in!
            return user;
        }
        else
        {
            boolean isFirstClickAnonymousUser = isFirstClickAnonymousUser(user);
            boolean isFirstClick = isFirstClick(request, response);
            if(user == null && isFirstClick)
            {
                //Authenticate the user as the first click anonymous user
                user = getFirstClickAnonymousUser(request, response);
                request.getSession().setAttribute(DefaultAuthenticator.LOGGED_IN_KEY, user);
                request.getSession().setAttribute(DefaultAuthenticator.LOGGED_OUT_KEY, null);
                
                if(LOG.isDebugEnabled()) 
                {
                    LOG.debug("First time click from [" + request.getRemoteAddr() + "] logged in as [" + user + "]");
                }
            }
            else if(isInternalAjaxRequest(request, response)) {
                //Authenticate the user as the first click anonymous user
                if(user == null)
                {
                    user = getFirstClickAnonymousUser(request, response);
                    request.getSession().setAttribute(DefaultAuthenticator.LOGGED_IN_KEY, user);
                    request.getSession().setAttribute(DefaultAuthenticator.LOGGED_OUT_KEY, null);    
                    if(LOG.isDebugEnabled()) 
                    {
                        LOG.debug("First time click internal AJAX request from [" + request.getRemoteAddr() + "] logged in as [" + user + "]");
                    }                    
                }
                if(LOG.isDebugEnabled()) 
                {
                    LOG.debug("Confluence is loading internal content for first click anonymous user via AJAX. Allow this content from [" + request.getRemoteAddr() + "]");
                }                
                return user;
            }
            else if(user != null && !isFirstClick && isFirstClickAnonymousUser)
            {
                //Force the logout of the anonymous first click user
                request.getSession().setAttribute(DefaultAuthenticator.LOGGED_IN_KEY, null);
                request.getSession().setAttribute(DefaultAuthenticator.LOGGED_OUT_KEY, new Boolean(true));
                user = null;
                if(LOG.isInfoEnabled()) 
                {
                    LOG.info("First time click user from [" + request.getRemoteAddr() + "] logged out");
                }
            } else if(user == null && isGoogleBot(request, response)) {
                //Authenticate the user as the google bot anonymous user
                user = getGoogleBotAnonymousUser(request, response);
                request.getSession().setAttribute(DefaultAuthenticator.LOGGED_IN_KEY, user);
                request.getSession().setAttribute(DefaultAuthenticator.LOGGED_OUT_KEY, null);
                
                if(LOG.isInfoEnabled()) 
                {
                    LOG.info("Google bot from [" + request.getRemoteAddr() + "] logged in as [" + user + "]");
                }
            }
            else if(isGoogleBotAnonymousUser(user) && !isGoogleBot(request, response))
            {
                //Force the logout of the anonymous first click user
                request.getSession().setAttribute(DefaultAuthenticator.LOGGED_IN_KEY, null);
                request.getSession().setAttribute(DefaultAuthenticator.LOGGED_OUT_KEY, new Boolean(true));
                user = null;
                if(LOG.isDebugEnabled()) 
                {
                    LOG.debug("Google bot user from [" + request.getRemoteAddr() + "] logged out");
                }                
            }
            // User is null (User requires login) or first click user is logged on,
            // but the user is coming again from Google
            return user;
        }
    }

    /*
     * Retrieves the anonymous user from Confluence
     */
    private Principal getFirstClickAnonymousUser(HttpServletRequest request, HttpServletResponse response)
    {
        String username = getConfigParam("fcf.username", "googlefcf");
        
        if(LOG.isDebugEnabled())
        {
            LOG.debug("First click about to retrieve user [" + username + "]");
        }        
        Principal user = getUser(username);

        if(LOG.isDebugEnabled())
        {
            LOG.debug("First click anonymous user is [" + user + "]");
        }
        
        if(user == null)
        {
            LOG.warn("First click anonymous user [" + username + "] not found.");
        }
        
        return user;
    }
    
    /*
     * Checks if this is the first user click
     */
    private boolean isGoogleBot(HttpServletRequest request, HttpServletResponse response)
    {
        String name = getConfigParam("gb.header", "From");
        String value = request.getHeader(name);
        String compareTo = getConfigParam("gb.header_value", "googlebot");

        if(LOG.isDebugEnabled())
        {
            LOG.debug("GoogleBot value for HTTP header [" + name + "] is [" + value + "]. Comparing to [" + compareTo + "]");
        }
        
        return value != null && value.startsWith(compareTo);
    }

    /*
     * Retrieves the anonymous user from Confluence
     */
    private Principal getGoogleBotAnonymousUser(HttpServletRequest request, HttpServletResponse response)
    {
        String username = getConfigParam("gb.username", "googlebot");
        if(LOG.isDebugEnabled())
        {
            LOG.debug("Google Bot about to retrieve user [" + username + "]");
        }
        
        Principal user = getUser(username);

        if(LOG.isDebugEnabled())
        {
            LOG.debug("Google Bot anonymous user is [" + user + "]");
        }
        
        if(user == null)
        {
            LOG.warn("Google Bot anonymous user [" + username + "] not found.");
        }
        
        return user;
    }
    
    /*
     * Confluence loads internal content (for examples menus) with an AJAX request. This content shouldn't be checked.
     */
    private boolean isInternalAjaxRequest(HttpServletRequest request, HttpServletResponse response)
    {
        String srcName = getConfigParam("fcf.ajax.src.header", "Referer");
        String srcValue = request.getHeader(srcName);
        String srcCompareTo = getConfigParam("fcf.ajax.src.header_value", "http://www.mulesoft.org");

        String reqWithName = getConfigParam("fcf.ajax.reqWith.header", "X-Requested-With");
        String reqWithValue = request.getHeader(reqWithName);
        String reqWithCompareTo = getConfigParam("fcf.ajax.reqWith.header_value", "XMLHttpRequest");
        
        if(LOG.isDebugEnabled())
        {
            LOG.debug("First Free Click value for HTTP header [" + srcName + "] is [" + srcValue + "]. Comparing to [" + srcCompareTo + "]");
            LOG.debug("First Free Click value for HTTP header [" + reqWithName + "] is [" + reqWithValue + "]. Comparing to [" + reqWithCompareTo + "]");
        }
        
        return srcValue != null && srcValue.startsWith(srcCompareTo) && reqWithValue != null && reqWithValue.equalsIgnoreCase(reqWithCompareTo);
    }
    
    /*
     * Checks if this is the first user click
     */
    private boolean isFirstClick(HttpServletRequest request, HttpServletResponse response)
    {
        String name = getConfigParam("fcf.header", "Referer");
        String value = request.getHeader(name);
        String compareTo = getConfigParam("fcf.header_value", "http://www.google.com");

        if(LOG.isDebugEnabled())
        {
            LOG.debug("First Free Click value for HTTP header [" + name + "] is [" + value + "]. Comparing to [" + compareTo + "]");
        }
        
        return value != null && value.startsWith(compareTo);
    }
    
    /*
     * 
     */
    private boolean isFirstClickAnonymousUser(Principal user)
    {
        String username = getConfigParam("fcf.username", "googlefcf");
        if(LOG.isDebugEnabled())
        {
            LOG.debug("First click about to check session user [" + user + "] against [" + username + "]");
        }
        return user != null && username.equalsIgnoreCase(user.getName());
    }
    
    private boolean isGoogleBotAnonymousUser(Principal user)
    {
        String username = getConfigParam("gb.username", "googlebot");
        if(LOG.isDebugEnabled())
        {
            LOG.debug("Google bot about to check session user [" + user + "] against [" + username + "]");
        }
        return user != null && username.equalsIgnoreCase(user.getName());
    }    
    
    private boolean isAnonymousUser(Principal user) 
    {
        return isFirstClickAnonymousUser(user) || isGoogleBotAnonymousUser(user);
    }
    
    /*
     * For debugging purposes, dump the request
     */
    @SuppressWarnings("rawtypes")
    private String dumpRequest(HttpServletRequest request) {
        Object paramName;
        StringBuilder sb = new StringBuilder(request.getRequestURL() + " -> HTTP Headers - [");
        for(Enumeration e = request.getHeaderNames(); e.hasMoreElements(); ) {
            paramName = e.nextElement();
            sb.append(paramName + "=" + request.getHeader(paramName.toString()));
            if(e.hasMoreElements()) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
    
    private String getConfigParam(String key, String defaultValue)
    {
        return MuleConfluenceAuthenticatorConfig.instance().getConfigutationParameter(key, defaultValue);
    }
}
