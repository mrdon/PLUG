package com.atlassian.plugin.modulefactory;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.util.ClassLoaderUtils;
import org.apache.bsf.BSFManager;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.logging.Logger;

/**
 * A ModuleFactory implementation that creates module instances that are proxies that pass through method calls
 * defined on the configured module interface to a configured dynamic scripting engine.
 * <p>
 * Configuration for ModuleDescriptors that employ this ModuleFactory should include either:
 * <ul>
 * <li>a script resource</li>
 * <li>a literal script</li>
 * </ul>
 *
 * TODO: let loaded resource files with registered file extensions be used instead of declaring a language
 */
public class BsfModuleFactory implements ModuleFactory
{
    private static final Logger LOG = Logger.getLogger(BsfModuleFactory.class.getName());

    /**
     * Required configuration, wet the ModuleDescriptor using this key.
     */
    public static final String CONFIG_KEY_MODULE_DESCRIPTOR = "moduleDescriptor";

    /**
     * Required configuration, set the language using this key (e.g. "groovy", "javascript").
     */
    public static final String CONFIG_KEY_LANGUAGE = "language";

    private static final String RESOURCE_TYPE_SCRIPT = "script";

    private ModuleDescriptor moduleDescriptor;

    /**
     * The configured module descriptor must be fully initialised before this call is made as the scripting
     * environment will get a reference to it.
     *
     * @return a Proxy that passes method calls to the scripting engine.
     */
    public Object getModule()
    {

        List scripts = moduleDescriptor.getResourceDescriptors(RESOURCE_TYPE_SCRIPT);
        if (scripts.size() != 1)
        {
            throw new IllegalStateException("I need exactly 1 script in the module descriptor at the moment. " + scripts.size() + " found.");
        }
        ResourceDescriptor resourceDescriptor = (ResourceDescriptor) scripts.get(0); // TODO multiple scripts
        final String language = resourceDescriptor.getName();
        final String content = resourceDescriptor.getContent();
        final String scriptLocation = resourceDescriptor.getLocation();
        String scriptBody = null;
        if (StringUtils.isNotBlank(content))
        {
            scriptBody = content;
        } else
        {
            // load from file
            BufferedReader in = new BufferedReader(new InputStreamReader(ClassLoaderUtils.getResourceAsStream(scriptLocation, this.getClass())));
            StringBuffer sb = new StringBuffer();
            String line;
            try
            {
                while ((line = in.readLine()) != null)
                {
                    sb.append(line + "\n");
                }
            } catch (IOException e)
            {
                e.printStackTrace();
            }
            scriptBody = sb.toString();
        }

        final Class moduleInterface = moduleDescriptor.getModuleBaseType();

        return getModuleImplementation(language, scriptBody, moduleInterface);
    }

    Object getModuleImplementation(final String language, final String scriptBody, final Class moduleInterface)
    {
        Object moduleImplementation = null;
        try
        {
            BSFManager manager = new BSFManager();

            // declare the moduleDescriptor in the scripting execution environment so it can retrieve whatever
            // necessary config
            manager.declareBean(CONFIG_KEY_MODULE_DESCRIPTOR, moduleDescriptor, ModuleDescriptor.class);

            // executing the script should result in the creation of an object that implements the interface

            moduleImplementation = manager.eval(language, "", 0, 0, scriptBody);

        }
        catch (Throwable e)
        {
            System.out.println("Unable to execute script");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        if (moduleImplementation != null && !moduleInterface.isAssignableFrom(moduleImplementation.getClass()))
        {
            LOG.warning("script does not appear to return an implementation of the module class: " + moduleInterface.getName());
        }

        return moduleImplementation;
    }


    public void setModuleDescriptor(ModuleDescriptor moduleDescriptor)
    {
        if (moduleDescriptor == null)
        {
            throw new NullPointerException("moduleDescriptor cannot be null");
        }


        this.moduleDescriptor = moduleDescriptor;
    }

}
