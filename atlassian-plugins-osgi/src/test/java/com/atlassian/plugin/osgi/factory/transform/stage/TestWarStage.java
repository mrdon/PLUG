package com.atlassian.plugin.osgi.factory.transform.stage;

import junit.framework.TestCase;

import java.io.File;
import java.util.Collections;

import com.atlassian.plugin.test.PluginJarBuilder;
import com.atlassian.plugin.osgi.factory.transform.TransformContext;
import com.atlassian.plugin.osgi.hostcomponents.HostComponentRegistration;
import com.atlassian.plugin.PluginAccessor;

public class TestWarStage  extends TestCase
{
    public void testTransform() throws Exception
    {
        final File plugin = new PluginJarBuilder("mywar-")
            .addFormattedResource("WEB-INF/web.xml",
                "<web-app id='starter' version='2.4'",
                "         xmlns='http://java.sun.com/xml/ns/j2ee'",
                "         xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'",
                "         xsi:schemaLocation='http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd'>",
                "   <servlet>",
                "     <servlet-name>test</servlet-name>",
                "     <servlet-class>my.TestServlet</servlet-class>",
                "   </servlet>",
                "   <servlet-mapping>",
                "     <servlet-name>test</servlet-name>",
                "     <url-pattern>/test</url-pattern>",
                "   </servlet-mapping>",
                "</web-app>")
            .addFormattedJava("my.TestServlet",
                "package my;",
                "public class TestServlet extends javax.servlet.http.HttpServlet {}")
            .addResource("WEB-INF/lib/foo.jar", "nothing really")
            .build();
        WarStage stage = new WarStage();

        final TransformContext context = new TransformContext(Collections.<HostComponentRegistration> emptyList(), plugin,
            "WEB-INF/web.xml");
        stage.execute(context);
        byte[] apxml = context.getFileOverrides().get("atlassian-plugin.xml");
        assertNotNull(apxml);
        String xml = new String(apxml);
        System.out.println("xml:\n\n"+xml);
        assertTrue(xml.contains("<servlet key=\"test\""));
        assertTrue(xml.contains("<atlassian-plugin key=\"starter\""));
        assertEquals("WEB-INF/classes,WEB-INF/lib/foo.jar", context.getBndInstructions().get("Bundle-ClassPath"));
    }
}
