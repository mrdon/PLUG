package com.atlassian.plugin.osgi.container.felix;

import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.osgi.container.PackageScannerConfiguration;
import com.atlassian.plugin.osgi.util.OsgiHeaderUtil;
import com.atlassian.plugin.util.ClassLoaderUtils;

import java.util.List;
import java.util.Collection;
import java.util.Iterator;
import java.util.jar.Manifest;
import java.io.*;
import java.net.MalformedURLException;

import org.twdata.pkgscanner.ExportPackage;
import org.twdata.pkgscanner.PackageScanner;
import static org.twdata.pkgscanner.PackageScanner.jars;
import static org.twdata.pkgscanner.PackageScanner.include;
import static org.twdata.pkgscanner.PackageScanner.exclude;
import static org.twdata.pkgscanner.PackageScanner.packages;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Jar;

import javax.servlet.ServletContext;

/**
 * Builds the OSGi package exports string.  Uses a file to cache the scanned results, keyed by the application version.
 */
class ExportsBuilder
{

    static final String JDK_PACKAGES_PATH = "jdk-packages.txt";
    static final String JDK6_PACKAGES_PATH = "jdk6-packages.txt";
    private static Log log = LogFactory.getLog(ExportsBuilder.class);
    private static final String EXPORTS_TXT = "exports.txt";

    /**
     * Determines framework exports taking into account host components and package scanner configuration.
     *
     * @param regs The list of host component registrations
     * @param packageScannerConfig The configuration for the package scanning
     * @return A list of exports, in a format compatible with OSGi headers
     */
    public String determineExports(List<HostComponentRegistration> regs, PackageScannerConfiguration packageScannerConfig, File cacheDir){

        String exports = loadExportsFromCache(cacheDir, packageScannerConfig.getCurrentHostVersion());

        if (exports == null)
        {

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
                origExports.append(OsgiHeaderUtil.findReferredPackages(regs));

                Analyzer analyzer = new Analyzer();
                analyzer.setJar(new Jar("somename.jar"));

                // we pretend the exports are imports for the sake of the bnd tool, which would otherwise cut out
                // exports that weren't actually in the jar
                analyzer.setProperty(Constants.IMPORT_PACKAGE, origExports.toString());
                Manifest mf = analyzer.calcManifest();

                exports = mf.getMainAttributes().getValue(Constants.IMPORT_PACKAGE);
                saveExportsToCache(cacheDir, packageScannerConfig.getCurrentHostVersion(), exports);
            } catch (IOException ex)
            {
                log.error("Unable to calculate necessary exports based on host components", ex);
                exports = origExports.toString();
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Exports:\n"+exports.replaceAll(",", "\r\n"));
        }
        return exports;
    }

    private void saveExportsToCache(File cacheDir, String currentHostVersion, String exports)
    {
        // Don't bother saving exports if a version isn't supplied
        if (currentHostVersion == null)
            return;

        File cache = new File(cacheDir, EXPORTS_TXT);
        FileWriter fout = null;
        try
        {
            fout = new FileWriter(cache);
            fout.write(currentHostVersion);
            fout.write("\n");
            fout.write(exports);
        }
        catch (FileNotFoundException e)
        {
            log.warn("Cache directory not available: "+cacheDir.getAbsolutePath(), e);
        }
        catch (IOException e)
        {
            log.warn("Unable to save exports cache", e);
        }
        finally
        {
            IOUtils.closeQuietly(fout);
        }

    }

    private String loadExportsFromCache(File cacheDir, String currentHostVersion)
    {
        // Don't bother loading exports if a version isn't supplied
        if (currentHostVersion == null)
            return null;

        File cache = new File(cacheDir, EXPORTS_TXT);
        if (cache.exists())
        {
            FileReader reader = null;
            try
            {
                reader = new FileReader(cache);
                String contents = IOUtils.toString(reader);
                int pos = contents.indexOf('\n');
                if (pos == -1)
                {
                    throw new IOException("Invalid cache file format");
                }
                String cacheVersion = contents.substring(0, pos);
                if (currentHostVersion.equals(cacheVersion))
                {
                    return contents.substring(pos + 1);
                }
            }
            catch (FileNotFoundException e)
            {
                // Should never happen
                throw new RuntimeException(e);
            }
            catch (IOException e)
            {
                log.warn("Unable to write exports cache", e);
            }
            finally
            {
                IOUtils.closeQuietly(reader);
            }
        }
        return null;
    }

    void constructAutoExports(StringBuilder sb, Collection<ExportPackage> packageExports) {
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

    Collection<ExportPackage> generateExports(PackageScannerConfiguration packageScannerConfig)
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

    void constructJdkExports(StringBuilder sb, String packageListPath)
    {
        InputStream in = null;
        try
        {
            in = ClassLoaderUtils.getResourceAsStream(packageListPath, ExportsBuilder.class);
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
