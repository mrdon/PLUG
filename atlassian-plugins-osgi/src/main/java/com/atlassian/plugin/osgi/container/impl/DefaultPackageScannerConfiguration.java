package com.atlassian.plugin.osgi.container.impl;

import com.atlassian.plugin.osgi.container.PackageScannerConfiguration;

import javax.servlet.ServletContext;
import java.util.*;

/**
 * Default package scanner configuration.  Probably mostly useful for Spring XML configuration.
 */
public class DefaultPackageScannerConfiguration implements PackageScannerConfiguration
{

    private List<String> jarIncludes = Arrays.asList("*.jar");
    private List<String> jarExcludes = Collections.emptyList();
    private List<String> packageIncludes = Arrays.asList("com.atlassian.*", "javax.swing.tree", "org.quartz", "org.quartz.*", "bucket.*", "net.sf.cglib", "net.sf.cglib.*", "net.sf.hibernate", "net.sf.hibernate.*", "com.octo.captcha.*", "com.opensymphony.*", "org.apache.*", "org.xml.*", "javax.*", "org.w3c.*");
    private List<String> packageExcludes = Arrays.asList("com.springframework*", "org.apache.commons.logging*");
    private Map<String, String> packageVersions;
    private String hostVersion;
    private ServletContext servletContext;

    public DefaultPackageScannerConfiguration()
    {
        this(null);
    }

    /**
     * @since 2.2
     * @param hostVersion The current host application version
     */
    public DefaultPackageScannerConfiguration(String hostVersion)
    {
        this.hostVersion = hostVersion;
        jarIncludes = new ArrayList<String>(jarIncludes);
        jarExcludes = new ArrayList<String>(jarExcludes);
        packageIncludes = new ArrayList<String>(packageIncludes);
        packageExcludes = new ArrayList<String>(packageExcludes);
    }


    public void setJarIncludes(List<String> jarIncludes)
    {
        this.jarIncludes = jarIncludes;
    }

    public void setJarExcludes(List<String> jarExcludes)
    {
        this.jarExcludes = jarExcludes;
    }

    public void setPackageIncludes(List<String> packageIncludes)
    {
        this.packageIncludes = packageIncludes;
    }

    public void setPackageExcludes(List<String> packageExcludes)
    {
        this.packageExcludes = packageExcludes;
    }

    /**
     * Sets the jars to include and exclude from scanning
     * @param includes A list of jar patterns to include
     * @param excludes A list of jar patterns to exclude
     */
    public void setJarPatterns(List<String> includes, List<String> excludes) {
        this.jarIncludes = includes;
        this.jarExcludes = excludes;
    }

    /**
     * Sets the packages to include and exclude
     * @param includes A list of patterns to include
     * @param excludes A list of patterns to exclude
     */
    public void setPackagePatterns(List<String> includes, List<String> excludes) {
        this.packageIncludes = includes;
        this.packageExcludes = excludes;
    }

    /**
     * Maps discovered packages to specific versions by overriding autodiscovered versions
     * @param packageToVersions A map of package patterns to version strings
     */
    public void setPackageVersions(Map<String,String> packageToVersions)
    {
        this.packageVersions = packageToVersions;
    }

    public List<String> getJarIncludes()
    {
        return jarIncludes;
    }

    public List<String> getJarExcludes()
    {
        return jarExcludes;
    }

    public List<String> getPackageIncludes()
    {
        return packageIncludes;
    }

    public List<String> getPackageExcludes()
    {
        return packageExcludes;
    }

    public Map<String, String> getPackageVersions()
    {
        return packageVersions;
    }

    public String getCurrentHostVersion()
    {
        return hostVersion;
    }

    public ServletContext getServletContext()
    {
        return servletContext;
    }

    public void setServletContext(ServletContext servletContext)
    {
        this.servletContext = servletContext;
    }
}
