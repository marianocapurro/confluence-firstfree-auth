/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.confluence.authenticator;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * @author mariano
 *
 */
public class MuleConfluenceAuthenticatorConfig
{

    private Properties configuration = new Properties();
    private static MuleConfluenceAuthenticatorConfig instance = null;
    private static final String CONFIG_FILENAME = "muleauth.properties";
    private static final Logger LOG = Logger.getLogger(MuleConfluenceAuthenticatorConfig.class);  
    private static final String REFRESH_CONFIG_KEY = "refresh";
    private static final long DEFAULT_REFRESH_TIMEOUT = 86400L; // One day in seconds
    
    private long nextConfigurationRefresh = -1L;
        
    /**
     * 
     */
    private MuleConfluenceAuthenticatorConfig()
    {
    }

    /**
     * 
     * @return
     */
    public static synchronized MuleConfluenceAuthenticatorConfig instance()
    {
        if(instance == null || System.currentTimeMillis() >= instance.getNextConfigurationRefresh())
        {
            if(LOG.isDebugEnabled())
            {
                LOG.debug("Configuration is null or expired. Reloading it.");
            }
            instance = new MuleConfluenceAuthenticatorConfig();
            instance.load();
            
            instance.setNextConfigurationRefresh(System.currentTimeMillis() + instance.getRefreshTimeout() * 1000);
            
            LOG.info("Configuration reloaded. Next update will be in [" + instance.getRefreshTimeout() + " seconds]. Current configuration is: [" + instance.configuration  + "]");
        }
        return instance;
    }
    
    /**
     * 
     *
     */
    private void load()
    {
        InputStream in = null;
        
        try
        {
            String configPath = getClass().getResource("/" + CONFIG_FILENAME).getPath();
            if(LOG.isDebugEnabled())
            {
                LOG.debug("About to load configuration from [" + configPath + "]");
            }
            
            in = new FileInputStream(configPath);
            
            if(in != null)
            {
                this.configuration.load(in);
                if(LOG.isDebugEnabled()) 
                {
                    LOG.debug("Configuration loaded. New configuration is [" + this.configuration + "]");
                }
            }
        }
        catch(Exception ex)
        {
            LOG.error("Could not load configuration from properties file [" + CONFIG_FILENAME + "]: " + ex.getMessage(), ex);
        }
        finally
        {
            try
            {
                if(in != null)
                {
                    in.close();
                }
            }
            catch(Exception ex)
            {
                LOG.error("Could not close configuration file [" + CONFIG_FILENAME + "] input stream: " + ex.getMessage(), ex); 
            }
        }
    }

    /**
     * 
     * @param key
     * @param defaultValue
     * @return
     */
    public String getConfigutationParameter(String key, String defaultValue)
    {
        String value = this.configuration.getProperty(key, defaultValue);
        if(LOG.isDebugEnabled())
        {
            LOG.debug("Configuration parameter [" + key + "] value is [" + value + "]. Default value is [" + defaultValue + "]");
        }
        return value;
    }
    
    /**
     * 
     * @return
     */
    private long getRefreshTimeout()
    {
        try
        {
            return Long.parseLong(getConfigutationParameter(REFRESH_CONFIG_KEY, null));
        }
        catch(Exception ex)
        {
            LOG.warn("Cannot get refresh timeout from configuration [" + ex.getMessage() + "]. Using default value [" + DEFAULT_REFRESH_TIMEOUT + " seconds]", ex);
            return DEFAULT_REFRESH_TIMEOUT;
        }
    }

    private long getNextConfigurationRefresh()
    {
        return nextConfigurationRefresh;
    }

    private void setNextConfigurationRefresh(long nextConfigurationRefresh)
    {
        this.nextConfigurationRefresh = nextConfigurationRefresh;
    }
}
