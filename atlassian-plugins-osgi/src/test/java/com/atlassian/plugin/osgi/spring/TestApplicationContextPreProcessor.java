package com.atlassian.plugin.osgi.spring;

import com.atlassian.plugin.JarPluginArtifact;
import com.atlassian.plugin.osgi.PluginInContainerTestBase;
import com.atlassian.plugin.test.PluginJarBuilder;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

/**
 *
 */
public class TestApplicationContextPreProcessor extends PluginInContainerTestBase
{
    public void testCustomPreProcessor() throws Exception
    {
        if (System.getProperty("java.version").startsWith("1.6"))
        {

            new PluginJarBuilder("testPreProcessor")
                    .addFormattedResource("atlassian-plugin.xml",
                            "<atlassian-plugin name='Test' key='test.pre.plugin' pluginsVersion='2'>",
                            "    <plugin-info>",
                            "        <version>1.0</version>",
                            "    </plugin-info>",
                            "    <component key='obj' class='my.CustomProcessor' public='true' interface='com.atlassian.plugin.osgi.spring.external.ApplicationContextPreProcessor'/>",
                            "</atlassian-plugin>")
                    .addFormattedJava("my.StaticFieldBeanFactoryPostProcessor",
                            "package my;",
                            "import org.springframework.beans.BeansException;",
                            "import org.springframework.beans.factory.config.BeanDefinition;",
                            "import org.springframework.beans.factory.config.BeanFactoryPostProcessor;",
                            "import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;",
                            "public class StaticFieldBeanFactoryPostProcessor implements BeanFactoryPostProcessor {",
                            "    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {",
                            "        String[] names = beanFactory.getBeanDefinitionNames();" +
                                    "        for (int x=0; x<names.length; x++) {",
                            "            BeanDefinition def = beanFactory.getBeanDefinition(names[x]);",
                            "            try {",
                            "                Class clazz = beanFactory.getBeanClassLoader().loadClass(def.getBeanClassName());",
                            "                if (clazz.getField('name') != null) {",
                            "                    clazz.getField('name').set(clazz, 'changed');",
                            "                }",
                            "            }",
                            "            catch (Exception e) {",
                            "                throw new RuntimeException(e);",
                            "            }",
                            "        }",
                            "    }",
                            "}")
                    .addFormattedJava("my.CustomProcessor",
                            "package my;",
                            "import com.atlassian.plugin.osgi.spring.external.ApplicationContextPreProcessor;",
                            "import org.osgi.framework.Bundle;",
                            "import org.springframework.context.ConfigurableApplicationContext;",
                            "public class CustomProcessor implements ApplicationContextPreProcessor {",
                            "  public boolean isSpringPoweredBundle(Bundle bundle) { return true; }",
                            "  public void process(Bundle bundle, ConfigurableApplicationContext applicationContext) {",
                            "    applicationContext.addBeanFactoryPostProcessor(new StaticFieldBeanFactoryPostProcessor());",
                            "  }",
                            "}")
                    .build(pluginsDir);
            initPluginManager();

            File plugin = new PluginJarBuilder("testPreProcessor")
                    .addFormattedResource("atlassian-plugin.xml",
                            "<atlassian-plugin name='Test' key='test.pre.client' pluginsVersion='2'>",
                            "    <plugin-info>",
                            "        <version>1.0</version>",
                            "    </plugin-info>",
                            "    <component key='obj' class='my.Foo' />",
                            "</atlassian-plugin>")
                    .addFormattedJava("my.Foo",
                            "package my;",
                            "public class Foo {",
                            "  public volatile static String name = 'original';",
                            "}")
                    .build();
            pluginManager.installPlugin(new JarPluginArtifact(plugin));

            Class<Object> fooClass = pluginManager.getPlugin("test.pre.client").loadClass("my.Foo", null);
            assertEquals("changed", fooClass.getField("name").get(fooClass));
        }
    }
}
