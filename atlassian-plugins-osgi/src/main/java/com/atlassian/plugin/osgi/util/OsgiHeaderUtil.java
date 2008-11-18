package com.atlassian.plugin.osgi.util;

import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.osgi.container.PackageScannerConfiguration;
import com.atlassian.plugin.util.ClassLoaderUtils;
import com.atlassian.plugin.util.ClassUtils;

import java.util.*;
import java.util.jar.Manifest;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
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
     * Determines framework exports taking into account host components and package scanner configuration.
     *
     * @param regs The list of host component registrations
     * @param packageScannerConfig The configuration for the package scanning
     * @return A list of exports, in a format compatible with OSGi headers
     */
    public static String determineExports(List<HostComponentRegistration> regs, PackageScannerConfiguration packageScannerConfig){
        String exports = null;

        StringBuilder origExports = new StringBuilder();
        origExports.append("org.osgi.framework; version=1.3.0,");
        origExports.append("org.osgi.service.packageadmin; version=1.2.0," );
        origExports.append("org.osgi.service.startlevel; version=1.0.0,");
        origExports.append("org.osgi.service.url; version=1.0.0,");
        origExports.append("org.osgi.util; version=1.3.0,");
        origExports.append("org.osgi.util.tracker; version=1.3.0,");
        origExports.append("host.service.command; version=1.0.0,");

        constructJdkExports(origExports, JDK_PACKAGES_PATH);
        origExports.append(",");

        if (System.getProperty("java.specification.version").equals("1.6")) {
            constructJdkExports(origExports, JDK6_PACKAGES_PATH);
            origExports.append(",");
        }

        Collection<ExportPackage> exportList = generateExports(packageScannerConfig);
        constructAutoExports(origExports, exportList);


        try
        {
            origExports.append(findReferredPackages(regs));

            Analyzer analyzer = new Analyzer();
            analyzer.setJar(new Jar("somename.jar"));
            
            // we pretend the exports are imports for the sake of the bnd tool, which would otherwise cut out
            // exports that weren't actually in the jar
            analyzer.setProperty(Constants.IMPORT_PACKAGE, origExports.toString());
            Manifest mf = analyzer.calcManifest();

            exports = mf.getMainAttributes().getValue(Constants.IMPORT_PACKAGE);
        } catch (IOException ex)
        {
            log.error("Unable to calculate necessary exports based on host components", ex);
            exports = origExports.toString();
        }

        if (log.isDebugEnabled()) {
            log.debug("Exports:\n"+exports.replaceAll(",", "\r\n"));
        }
        return exports;
    }

    static void constructAutoExports(StringBuilder sb, Collection<ExportPackage> packageExports) {
        for (Iterator<ExportPackage> i = packageExports.iterator(); i.hasNext(); ) {
            ExportPackage pkg = i.next();
            sb.append(pkg.getPackageName());
            if (pkg.getVersion() != null) {
                try {
                    Version.parseVersion(pkg.getVersion());
                    sb.append(";version=").append(pkg.getVersion());
                } catch (IllegalArgumentException ex) {
                    log.info("Unable to parse version: "+pkg.getVersion());
                }
            }
            sb.append(",");
        }
    }

    static Collection<ExportPackage> generateExports(PackageScannerConfiguration packageScannerConfig)
    {
        String[] arrType = new String[0];

        PackageScanner scanner = new PackageScanner()
           .select(
               jars(
                       include(packageScannerConfig.getJarIncludes().toArray(arrType)),
                       exclude(packageScannerConfig.getJarExcludes().toArray(arrType))),
               packages(
                       include(packageScannerConfig.getPackageIncludes().toArray(arrType)),
                       exclude(packageScannerConfig.getPackageExcludes().toArray(arrType)))
           )
           .withMappings(packageScannerConfig.getPackageVersions());

        Collection<ExportPackage> exports = scanner.scan();

        if (!isPackageScanSuccessful(exports) && packageScannerConfig.getServletContext() != null)
        {
            log.warn("Unable to find expected packages via classloader scanning.  Trying ServletContext scanning...");
            ServletContext ctx = packageScannerConfig.getServletContext();
            try
            {
                exports = scanner.scan(ctx.getResource("/WEB-INF/lib"), ctx.getResource("/WEB-INF/classes"));
            }
            catch (MalformedURLException e)
            {
                log.warn(e);
            }
        }
        
        if (!isPackageScanSuccessful(exports))
        {
            throw new IllegalStateException("Unable to find required packages via classloader or servlet context"
                    + " scanning, most likely due to an application server bug.");
        }
        return exports;
    }

    /**
     * Tests to see if a scan of packages to export was successful, using the presence of log4j as the criteria.
     *
     * @param exports The exports found so far
     * @return True if log4j is present, false otherwise
     */
    private static boolean isPackageScanSuccessful(Collection<ExportPackage> exports)
    {
        boolean log4jFound = false;
        for (ExportPackage export : exports)
        {
            if (export.getPackageName().equals("org.apache.log4j"))
            {
                log4jFound = true;
                break;
            }
        }
        return log4jFound;
    }

    static void constructJdkExports(StringBuilder sb, String packageListPath)
    {
        InputStream in = null;
        try
        {
            in = ClassLoaderUtils.getResourceAsStream(packageListPath, OsgiHeaderUtil.class);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null)
            {
                line = line.trim();
                if (line.length() > 0)
                {
                    if (line.charAt(0) != '#')
                    {
                        if (sb.length() > 0)
                            sb.append(',');
                        sb.append(line);
                    }
                }
            }
        } catch (IOException e)
        {
            IOUtils.closeQuietly(in);
        }
    }

}
