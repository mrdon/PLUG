package com.atlassian.plugin.osgi.factory.transform;

import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.osgi.hostcomponents.PropertyBuilder;
import com.atlassian.plugin.osgi.hostcomponents.ComponentRegistrar;

import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;

/**
 * Transforms host components into Spring configuration
 * @since 2.1
 */
public class HostComponentSpringTransformer implements SpringTransformer
{
    public void transform(List<HostComponentRegistration> regs, Document pluginDoc, Document springDoc)
    {
        Element root = springDoc.getRootElement();
        if (regs != null)
        {
            for (int x=0; x<regs.size(); x++)
            {
                HostComponentRegistration reg = regs.get(x);
                String beanName = reg.getProperties().get(PropertyBuilder.BEAN_NAME);

                Element osgiService = root.addElement("osgi:reference");
                osgiService.addAttribute("id", determineId(pluginDoc, beanName, x));

                // Disabling this for now due to some strange Spring DM bug where it will occasionally generate an invalid
                // filter, see http://jira.atlassian.com/browse/CONF-13292
                if (beanName != null)
                    osgiService.addAttribute("filter", "(&(bean-name="+beanName+")("+ ComponentRegistrar.HOST_COMPONENT_FLAG+"=true))");

                Element interfaces = osgiService.addElement("osgi:interfaces");
                for (String name : reg.getMainInterfaces())
                {
                    Element e = interfaces.addElement("beans:value");
                    e.setText(name);
                }
            }
        }
    }

    private String determineId(Document pluginDoc, String beanName, int iteration)
    {
        String id = beanName;
        if (id == null)
            id = "bean"+iteration;

        id = id.replaceAll("#", "LB");

        for (Object element : pluginDoc.getRootElement().elements("component-import"))
        {
            String key = ((Element)element).attributeValue("key");
            if (id.equals(key))
            {
                id+=iteration;
                break;
            }
        }
        return id;
    }
}
