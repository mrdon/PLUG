package com.atlassian.plugin.osgi.container.felix;

import org.apache.commons.logging.Log;
import org.apache.felix.framework.Logger;
import org.apache.felix.framework.util.FelixConstants;
import org.osgi.framework.BundleException;

/**
 * Bridges Felix logging messages with the Commons Logging
 */
public class FelixLoggerBridge extends Logger {
    private final Log log;

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
                log.info(message);
                break;
            case LOG_WARNING:
                log.info(message);
                break;
            default:
                log.debug("UNKNOWN[" + level + "]: " + message);
        }
    }
}
