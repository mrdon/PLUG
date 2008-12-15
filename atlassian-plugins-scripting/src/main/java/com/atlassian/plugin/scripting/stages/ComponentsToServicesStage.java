package com.atlassian.plugin.scripting.stages;

import com.atlassian.plugin.osgi.factory.transform.TransformStage;
import com.atlassian.plugin.osgi.factory.transform.TransformContext;
import com.atlassian.plugin.osgi.factory.transform.PluginTransformationException;
import com.atlassian.plugin.scripting.ScriptingTransformContext;
import com.atlassian.plugin.scripting.ScriptManager;
import com.atlassian.plugin.scripting.variables.JsScript;
import org.dom4j.Element;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.apache.commons.logging.impl.LogKitLogger;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: mrdon
 * Date: Dec 15, 2008
 * Time: 11:21:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class ComponentsToServicesStage implements TransformStage
{
    private static final Logger log = Logger.getLogger(ComponentsToServicesStage.class);

    public void execute(final TransformContext context) throws PluginTransformationException
    {
        ScriptingTransformContext ctx = (ScriptingTransformContext) context;
        final ScriptManager scriptManager = ctx.getScriptManager();

        List<Element> elements = context.getDescriptorDocument().getRootElement().elements("component");
        for (final Element component : elements)
        {
            final String cls = component.attributeValue("class");
            if ("true".equalsIgnoreCase(component.attributeValue("public")))
            {
                ctx.getBundleActivators().add(new BundleActivator()
                {
                    ServiceRegistration reg;
                    public void start(BundleContext bundleContext) throws Exception
                    {
                        JsScript script = scriptManager.run(bundleContext.getBundle().getResource(cls),
                            Collections.<String, Object>emptyMap());
                        Dictionary<String,String> dict = new Hashtable<String,String>();
                        dict.put("key", component.attributeValue("key"));
                        if (component.attribute("alias") != null)
                            dict.put("alias", component.attributeValue("alias"));
                        reg = bundleContext.registerService(findInterfaces(component, context), script.getResult(), dict);
                    }

                    public void stop(BundleContext bundleContext) throws Exception
                    {
                        //reg.unregister();
                    }
                });
            }
            else
            {
                log.warn("Non-public components not supported");
            }
        }
    }

    private String[] findInterfaces(Element component, TransformContext ctx)
    {
        List<Element> compInterfaces = component.elements("interface");
        String[] infNames = new String[compInterfaces.size()];
        int x=0;
        for (Element inf : compInterfaces)
        {
            String txt = inf.getTextTrim();
            ctx.getExtraImports().add(txt.substring(0, txt.lastIndexOf(".")));
            infNames[x++] = txt;
        }
        return infNames;
    }
}
