package com.atlassian.plugin.osgi;

import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginInformation;
import com.atlassian.plugin.Resourced;
import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.elements.ResourceDescriptor;

import java.util.Collection;
import java.util.List;
import java.util.Date;
import java.net.URL;
import java.io.InputStream;

import org.osgi.framework.Bundle;

/**
 * Created by IntelliJ IDEA.
 * User: tomd
 * Date: May 21, 2008
 * Time: 4:02:18 PM
 * This isn't the template you are looking for. It can go about its business.
 */
public class OsgiPlugin implements Plugin {
    public OsgiPlugin(Bundle bundle) {
        //To change body of created methods use File | Settings | File Templates.
    }

    public String getName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setName(String name) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getI18nNameKey() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setI18nNameKey(String i18nNameKey) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getKey() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setKey(String aPackage) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void addModuleDescriptor(ModuleDescriptor moduleDescriptor) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Collection getModuleDescriptors() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ModuleDescriptor getModuleDescriptor(String key) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List getModuleDescriptorsByModuleClass(Class aClass) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isEnabledByDefault() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setEnabledByDefault(boolean enabledByDefault) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public PluginInformation getPluginInformation() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setPluginInformation(PluginInformation pluginInformation) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setResources(Resourced resources) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isEnabled() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setEnabled(boolean enabled) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isSystemPlugin() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean containsSystemModule() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setSystemPlugin(boolean system) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isBundledPlugin() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Date getDateLoaded() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isUninstallable() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isDeleteable() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isDynamicallyLoaded() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Class loadClass(String clazz, Class callingClass) throws ClassNotFoundException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ClassLoader getClassLoader() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public URL getResource(String path) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public InputStream getResourceAsStream(String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void close() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public List getResourceDescriptors() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List getResourceDescriptors(String type) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ResourceLocation getResourceLocation(String type, String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ResourceDescriptor getResourceDescriptor(String type, String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int compareTo(Object o) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
