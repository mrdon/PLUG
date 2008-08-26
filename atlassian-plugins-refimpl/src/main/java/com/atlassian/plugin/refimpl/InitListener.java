package com.atlassian.plugin.refimpl;

import com.atlassian.plugin.PluginManager;
import com.atlassian.plugin.DefaultPluginManager;
import com.atlassian.plugin.osgi.loader.OsgiPluginLoader;
import com.atlassian.plugin.store.MemoryPluginStateStore;

import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;
import java.util.Arrays;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: mrdon
 * Date: 06/07/2008
 * Time: 12:24:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class InitListener implements ServletContextListener {

    public InitListener() {
    }

    public void contextInitialized(ServletContextEvent sce) {
        initializeLogger();

        Logger.getLogger(InitListener.class).info("Logging initialized.");
        ContainerManager.setInstance(new ContainerManager(sce.getServletContext()));
        ContainerManager mgr = ContainerManager.getInstance();
        mgr.getPluginManager().getPlugins();
    }

    public void contextDestroyed(ServletContextEvent sce) {
        ContainerManager.setInstance(null);
    }

    private void initializeLogger()
     {
       Properties logProperties = new Properties();

       try
       {
         logProperties.load(getClass().getResourceAsStream("/log4j.properties"));
         PropertyConfigurator.configure(logProperties);

       }
       catch(IOException e)
       {
         throw new RuntimeException("Unable to load logging property", e);
       }
     }

}
