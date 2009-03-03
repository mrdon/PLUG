package com.atlassian.plugin.osgi.util;

import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.osgi.container.PackageScannerConfiguration;
import com.atlassian.plugin.util.ClassLoaderUtils;
import com.atlassian.plugin.util.ClassUtils;

import java.util.*;
import java.util.jar.Manifest;
import java.io.*;
import java.net.MalformedURLException;

import org.twdata.pkgscanner.ExportPackage;
import org.twdata.pkgscanner.PackageScanner;
import static org.twdata.pkgscanner.PackageScanner.jars;
import static org.twdata.pkgscanner.PackageScanner.include;
import static org.twdata.pkgscanner.PackageScanner.exclude;
import static org.twdata.pkgscanner.PackageScanner.packages;
import org.osgi.framework.Version;
import org.osgi.framework.Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.io.IOUtils;
import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Jar;
import aQute.lib.header.OSGiHeader;

import javax.servlet.ServletContext;

/**
 * Utilities to help create OSGi headers
 */
public class OsgiHeaderUtil
{
    static final String JDK_PACKAGES_PATH = "jdk-packages.txt";
    static final String JDK6_PACKAGES_PATH = "jdk6-packages.txt";
    static Log log = LogFactory.getLog(OsgiHeaderUtil.class);

    /**
     * Finds all referred packages for host component registrations by scanning their declared interfaces' bytecode.
     *
     * @param registrations A list of host component registrations
     * @return The referred packages in a format compatible with an OSGi header
     * @throws IOException If there are any problems scanning bytecode
     */
    public static String findReferredPackages(List<HostComponentRegistration> registrations) throws IOException
    {
        StringBuffer sb = new StringBuffer();
        Set<String> referredPackages = new HashSet<String>();
        Set<String> referredClasses = new HashSet<String>();
        if (registrations == null)
        {
            sb.append(",");
        }
        else
        {
            for (HostComponentRegistration reg : registrations)
            {
                Set<Class> classesToScan = new HashSet<Class>();

                // Make sure we scan all extended interfaces as well
                for (Class inf : reg.getMainInterfaceClasses())
                    ClassUtils.findAllTypes(inf, classesToScan);

                for (Class inf : classesToScan)
                {
                    String clsName = inf.getName().replace('.','/')+".class";
                    crawlReferenceTree(clsName, referredClasses, referredPackages, 1);
                }
            }
            for (String pkg : referredPackages)
            {
                sb.append(pkg).append(",");
            }
        }
        return sb.toString();
    }

    /**
     * This will crawl the class interfaces to the desired level.
     *
     * @param className name of the class.
     * @param scannedClasses set of classes that have been scanned.
     * @param packageImports set of imports that have been found.
     * @param level depth of scan (recursion).
     * @throws IOException error loading a class.
     */
    static void crawlReferenceTree(String className, Set<String> scannedClasses, Set<String> packageImports, int level) throws IOException
    {
        if (level <= 0)
        {
            return;
        }

        if (className.startsWith("java/"))
            return;

        if (scannedClasses.contains(className))
            return;
        else
            scannedClasses.add(className);

        if (log.isDebugEnabled())
            log.debug("Crawling "+className);

        InputStream in = ClassLoaderUtils.getResourceAsStream(className, OsgiHeaderUtil.class);
        if (in == null)
        {
            log.error("Cannot find interface "+className);
            return;
        }
        Clazz clz = new Clazz(className, in);
        packageImports.addAll(clz.getReferred().keySet());

        Set<String> referredClasses = clz.getReferredClasses();
        for (String ref : referredClasses)
            crawlReferenceTree(ref, scannedClasses, packageImports, level-1);

    }

    /**
     * Parses an OSGi header line into a map structure
     *
     * @param header The header line
     * @return A map with the key the entry value and the value a map of attributes
     * @since 2.2.0
     */
    public static Map<String,Map<String,String>> parseHeader(String header)
    {
        return OSGiHeader.parseHeader(header);
    }

    /**
     * Builds the header string from a map
     * @param key The header value
     * @param attrs The map of attributes
     * @return A string, suitable for inclusion into an OSGI header string
     * @since 2.2.0
     */
    public static String buildHeader(String key, Map<String,String> attrs)
    {
        StringBuilder fullPkg = new StringBuilder(key);
        if (attrs != null && !attrs.isEmpty())
        {
            for (Map.Entry<String,String> entry : attrs.entrySet())
            {
                fullPkg.append(";");
                fullPkg.append(entry.getKey());
                fullPkg.append("=\"");
                fullPkg.append(entry.getValue());
                fullPkg.append("\"");
            }
        }
        return fullPkg.toString();
    }
}
