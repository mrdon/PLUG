package com.atlassian.plugin.osgi.container.felix;

import org.apache.commons.logging.Log;
import org.apache.felix.framework.Logger;
import org.apache.felix.framework.util.FelixConstants;
import org.osgi.framework.BundleException;

import java.util.List;
import java.util.Arrays;

/**
 * Bridges Felix logging messages with the Commons Logging
 */
public class FelixLoggerBridge extends Logger {
    private final Log log;

    private static final List<String> messagesToIgnore = Arrays.asList(
            "BeanInfo'. Please",
            "BeanInfo' was not found",
            "sun.beans.editors.",
            "add an import for 'org.springframework.osgi.service.",
            "Class 'org.springframework.util.Assert'",
            "Class '[Lorg.springframework.osgi.service",
            "Class 'org.springframework.core.InfrastructureProxy'",
            "Class 'org.springframework.aop.SpringProxy'",
            "Class 'org.springframework.aop.IntroductionInfo'",
            "Class 'org.apache.commons.logging.impl.Log4JLogger'"
    );

    public FelixLoggerBridge(Log log) {
        this.log = log;
        setLogLevel(
                log.isDebugEnabled() ? Logger.LOG_DEBUG :
                log.isInfoEnabled() ? Logger.LOG_WARNING :
                Logger.LOG_ERROR);
    }

    protected void doLog(org.osgi.framework.ServiceReference serviceReference, int level, java.lang.String message, java.lang.Throwable throwable) {
        if (serviceReference != null)
            message = "Service " + serviceReference + ": " + message;

        switch (level) {
            case LOG_DEBUG:
                log.debug(message);
                break;
            case LOG_ERROR:
                if (throwable != null) {
                    if ((throwable instanceof BundleException) &&
                            (((BundleException) throwable).getNestedException() != null)) {
                        throwable = ((BundleException) throwable).getNestedException();
                    }
                    log.error(message, throwable);
                } else
                    log.error(message);
                break;
            case LOG_INFO:
                logInfoUnlessLame(message);
                break;
            case LOG_WARNING:
                logInfoUnlessLame(message);
                break;
            default:
                log.debug("UNKNOWN[" + level + "]: " + message);
        }
    }

    protected void logInfoUnlessLame(String message)
    {
        if (message != null)
        {
            // I'm really, really sick of these stupid messages
            for (String dumbBit : messagesToIgnore)
                if (message.contains(dumbBit))
                    return;
        }
        log.info(message);
    }
}
