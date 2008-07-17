package com.atlassian.plugin.osgi.util;

import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.osgi.container.PackageScannerConfiguration;
import com.atlassian.plugin.util.ClassLoaderUtils;

import java.util.*;
import java.util.jar.Manifest;
import java.io.IOException;
import java.io.InputStream;

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
import aQute.lib.osgi.Clazz;
import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Jar;

/**
 * Utilities to help create OSGi headers
 */
public class OsgiHeaderUtil
{
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
                for (String inf : reg.getMainInterfaces())
                {
                    String clsName = inf.replace('.','/')+".class";
                    crawlReferenceTree(clsName, referredClasses, referredPackages);
                }
            }
            for (String pkg : referredPackages)
            {
                sb.append(pkg).append(",");
            }
        }
        return sb.toString();
    }

    static void crawlReferenceTree(String className, Set<String> scannedClasses, Set<String> packageImports) throws IOException
    {
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
            crawlReferenceTree(ref, scannedClasses, packageImports);

    }

    /**
     * Determines framework exports taking into account host components and package scanner configuration.
     *
     * @param regs The list of host component registrations
     * @param packageScannerConfig The configuration for the package scanning
     * @return A list of exports, in a format compatible with OSGi headers
     */
    public static String determineExports(List<HostComponentRegistration> regs, PackageScannerConfiguration packageScannerConfig){
        Collection<ExportPackage> exports = generateExports(packageScannerConfig);

        String baseExports = "org.osgi.framework; version=1.3.0," +
            "org.osgi.service.packageadmin; version=1.2.0," +
            "org.osgi.service.startlevel; version=1.0.0," +
            "org.osgi.service.url; version=1.0.0," +
            "org.osgi.util; version=1.3.0," +
            "org.osgi.util.tracker; version=1.3.0," +
            "host.service.command; version=1.0.0," +
            "javax.swing.tree,javax.swing,org.xml.sax,org.xml.sax.helpers," +
            "javax.xml,javax.xml.parsers,javax.xml.transform,javax.xml.transform.sax," +
            "javax.xml.transform.stream,javax.xml.transform.dom,org.w3c.dom,javax.naming.spi," +
            "javax.swing.border,javax.swing.event,javax.swing.text," +
            constructAutoExports(exports);

        try
        {
            String referredPackages = findReferredPackages(regs);

            Analyzer analyzer = new Analyzer();
            analyzer.setJar(new Jar("somename.jar"));
            
            // we pretend the exports are imports for the sake of the bnd tool, which would otherwise cut out
            // exports that weren't actually in the jar
            analyzer.setProperty(Constants.IMPORT_PACKAGE, baseExports + "," + referredPackages);
            Manifest mf = analyzer.calcManifest();
            return mf.getMainAttributes().getValue(Constants.IMPORT_PACKAGE);
        } catch (IOException ex)
        {
            log.error("Unable to calculate necessary exports based on host components", ex);
            return baseExports;
        }
    }

    static String constructAutoExports(Collection<ExportPackage> packageExports) {

        StringBuilder sb = new StringBuilder();
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
            if (i.hasNext()) {
                sb.append(",");
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Exports:\n"+sb.toString().replaceAll(",", "\r\n"));
        }

        return sb.toString();
    }

    static Collection<ExportPackage> generateExports(PackageScannerConfiguration packageScannerConfig)
    {
        String[] arrType = new String[0];
        Collection<ExportPackage> exports = new PackageScanner()
           .select(
               jars(
                       include(packageScannerConfig.getJarIncludes().toArray(arrType)),
                       exclude(packageScannerConfig.getJarExcludes().toArray(arrType))),
               packages(
                       include(packageScannerConfig.getPackageIncludes().toArray(arrType)),
                       exclude(packageScannerConfig.getPackageExcludes().toArray(arrType)))
           )
           .withMappings(packageScannerConfig.getPackageVersions())
           .scan();
        return exports;
    }
}
