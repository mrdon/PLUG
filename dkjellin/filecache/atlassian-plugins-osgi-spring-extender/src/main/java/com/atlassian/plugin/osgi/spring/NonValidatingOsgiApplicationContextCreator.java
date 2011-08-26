package com.atlassian.plugin.osgi.spring;

import com.atlassian.plugin.osgi.spring.external.ApplicationContextPreProcessor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.osgi.context.DelegatedExecutionOsgiBundleApplicationContext;
import org.springframework.osgi.extender.OsgiApplicationContextCreator;
import org.springframework.osgi.extender.support.ApplicationContextConfiguration;
import org.springframework.osgi.extender.support.DefaultOsgiApplicationContextCreator;
import org.springframework.osgi.extender.support.scanning.ConfigurationScanner;
import org.springframework.osgi.extender.support.scanning.DefaultConfigurationScanner;
import org.springframework.osgi.util.OsgiStringUtils;
import org.springframework.osgi.atlassian.NonValidatingOsgiBundleXmlApplicationContext;
import org.springframework.util.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Application context creator that will use a special application context that disables XML Schema validation
 *
 * @since 2.5.0
 */
public class NonValidatingOsgiApplicationContextCreator implements OsgiApplicationContextCreator
{
    private static final Logger log = LoggerFactory.getLogger(DefaultOsgiApplicationContextCreator.class);
    private final List<ApplicationContextPreProcessor> applicationContextPreProcessors;

	private ConfigurationScanner configurationScanner = new DefaultConfigurationScanner();

    public NonValidatingOsgiApplicationContextCreator(List<ApplicationContextPreProcessor> applicationContextPreProcessors)
    {
        this.applicationContextPreProcessors = applicationContextPreProcessors;
    }

    /**
     * Creates an application context that disables validation.  Most of this code is copy/pasted from
     * {@link DefaultOsgiApplicationContextCreator}
     * @param bundleContext The bundle context for the application context
     * @return The new application context
     * @throws Exception If anything goes wrong
     */
    public DelegatedExecutionOsgiBundleApplicationContext createApplicationContext(BundleContext bundleContext) throws Exception
    {
        Bundle bundle = bundleContext.getBundle();
		ApplicationContextConfiguration config = new ApplicationContextConfiguration(bundle, configurationScanner);
		if (log.isTraceEnabled())
			log.trace("Created configuration " + config + " for bundle "
					+ OsgiStringUtils.nullSafeNameAndSymName(bundle));

		// it's not a spring bundle, ignore it
        if (!isSpringPoweredBundle(bundle, config))
        {
            return null;
        }

        log.info("Discovered configurations " + ObjectUtils.nullSafeToString(config.getConfigurationLocations())
				+ " in bundle [" + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "]");

        // This is the one new line, which uses our application context and not the other one
		DelegatedExecutionOsgiBundleApplicationContext sdoac = new NonValidatingOsgiBundleXmlApplicationContext(
			config.getConfigurationLocations());

		sdoac.setBundleContext(bundleContext);
		sdoac.setPublishContextAsService(config.isPublishContextAsService());

        for (ApplicationContextPreProcessor processor : applicationContextPreProcessors)
        {
            processor.process(bundle, sdoac);
        }

		return sdoac;
    }

    boolean isSpringPoweredBundle(Bundle bundle, ApplicationContextConfiguration config)
    {
        // Check for the normal configuration xml files
        if (config.isSpringPoweredBundle())
        {
            return true;
        }

        // Check any preprocessors, as they may solely use annotations
        else
        {
            for (ApplicationContextPreProcessor processor : applicationContextPreProcessors)
            {
                if (processor.isSpringPoweredBundle(bundle))
                {
                    return true;
                }
            }
        }

        // Return false as the default
        return false;
    }
}
