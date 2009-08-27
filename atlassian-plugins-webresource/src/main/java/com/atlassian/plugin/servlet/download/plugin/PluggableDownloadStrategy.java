package com.atlassian.plugin.servlet.download.plugin;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.event.PluginEventListener;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.event.events.PluginModuleDisabledEvent;
import com.atlassian.plugin.event.events.PluginModuleEnabledEvent;
import com.atlassian.plugin.servlet.DownloadException;
import com.atlassian.plugin.servlet.DownloadStrategy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A download strategy which maintains a list of {@link DownloadStrategyModuleDescriptor}s
 * and delegates to them in order.
 *
 * @see DownloadStrategyModuleDescriptor
 * @see DownloadStrategy
 * @since 2.2.0
 */
public class PluggableDownloadStrategy implements DownloadStrategy
{
    private static final Log log = LogFactory.getLog(PluggableDownloadStrategy.class);
    private final Map<String, DownloadStrategy> strategies = new ConcurrentHashMap<String, DownloadStrategy>();

    public PluggableDownloadStrategy(final PluginEventManager pluginEventManager)
    {
        pluginEventManager.register(this);
    }

    public boolean matches(final String urlPath)
    {
        for (final DownloadStrategy strategy : strategies.values())
        {
            if (strategy.matches(urlPath))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Matched plugin download strategy: " + strategy.getClass().getName());
                }
                return true;
            }
        }
        return false;
    }

    public void serveFile(final HttpServletRequest request, final HttpServletResponse response) throws DownloadException
    {
        for (final DownloadStrategy strategy : strategies.values())
        {
            if (strategy.matches(request.getRequestURI().toLowerCase()))
            {
                strategy.serveFile(request, response);
                return;
            }
        }
        throw new DownloadException(
            "Found plugin download strategy during matching but not when trying to serve. Enable debug logging for more information.");
    }

    public void register(final String key, final DownloadStrategy strategy)
    {
        if (strategies.containsKey(key))
        {
            log.warn("Replacing existing download strategy with module key: " + key);
        }
        strategies.put(key, strategy);
    }

    public void unregister(final String key)
    {
        strategies.remove(key);
    }

    @PluginEventListener
    public void pluginModuleEnabled(final PluginModuleEnabledEvent event)
    {
        final ModuleDescriptor<?> module = event.getModule();
        if (!(module instanceof DownloadStrategyModuleDescriptor))
        {
            return;
        }

        register(module.getCompleteKey(), (DownloadStrategy) module.getModule());
    }

    @PluginEventListener
    public void pluginModuleDisabled(final PluginModuleDisabledEvent event)
    {
        final ModuleDescriptor<?> module = event.getModule();
        if (!(module instanceof DownloadStrategyModuleDescriptor))
        {
            return;
        }

        unregister(module.getCompleteKey());
    }
}
