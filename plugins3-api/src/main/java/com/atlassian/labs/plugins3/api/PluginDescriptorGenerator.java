package com.atlassian.labs.plugins3.api;

import com.atlassian.labs.plugins3.api.module.ServletContextParamModuleGenerator;
import com.atlassian.labs.plugins3.api.module.ServletFilterModuleGenerator;
import com.atlassian.labs.plugins3.api.module.ServletModuleGenerator;
import com.atlassian.labs.plugins3.api.module.WebItemModuleGenerator;
import com.atlassian.labs.plugins3.api.module.WebSectionModuleGenerator;
import org.dom4j.Element;

/**
 *
 */
public interface PluginDescriptorGenerator<INFO extends InfoGenerator>
{
    <M extends PluginDescriptorGenerator> M convertTo(Class<M> generatorClass);
    
    INFO info();

    <M extends ModuleGenerator> M add(String key, Class<M> moduleBuilderClass);

    void add(String key, Element moduleElement);

    ServletModuleGenerator addServlet(String key);

    ServletFilterModuleGenerator addServletFilter(String key);

    ServletContextParamModuleGenerator addServletContextParam(String key);

    WebSectionModuleGenerator addWebSection(String key);

    WebItemModuleGenerator addWebItem(String key);

    void scanForModules(Package rootPackage);
}
