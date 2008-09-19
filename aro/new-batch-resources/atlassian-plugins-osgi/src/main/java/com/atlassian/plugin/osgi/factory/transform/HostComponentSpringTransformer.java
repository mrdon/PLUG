package com.atlassian.plugin.osgi.factory.transform;

import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.osgi.hostcomponents.PropertyBuilder;

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
                String id = beanName;
                if (id == null)
                    id = "bean"+x;

                id = id.replaceAll("#", "LB");
                Element osgiService = root.addElement("osgi:reference");
                osgiService.addAttribute("id", id);

                if (beanName != null)
                    osgiService.addAttribute("filter", "(bean-name="+beanName+")");

                Element interfaces = osgiService.addElement("osgi:interfaces");
                for (String name : reg.getMainInterfaces())
                {
                    Element e = interfaces.addElement("beans:value");
                    e.setText(name);
                }
            }
        }
    }
}
