package com.atlassian.labs.plugins3.extender;

import com.atlassian.labs.plugins3.api.annotation.Component;
import com.atlassian.labs.plugins3.api.annotation.PluginModule;
import com.atlassian.labs.plugins3.spring.HostComponentBeanFactoryPostProcessor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.osgi.spring.external.ApplicationContextPreProcessor;
import com.atlassian.plugin.osgi.util.OsgiHeaderUtil;
import com.google.common.collect.Sets;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.CustomAutowireConfigurer;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionDefaults;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.w3c.dom.Element;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 *
 */
public class Plugins3ApplicationContextPreProcessor implements ApplicationContextPreProcessor
{
    private final PluginAccessor pluginAccessor;
    private static final Logger log = LoggerFactory.getLogger(Plugins3ApplicationContextPreProcessor.class);

    public Plugins3ApplicationContextPreProcessor(PluginAccessor pluginAccessor)
    {
        this.pluginAccessor = pluginAccessor;
    }

    public boolean isSpringPoweredBundle(Bundle bundle)
    {
        try
        {
            bundle.loadClass("AtlassianPlugin");
            return true;
        }
        catch (ClassNotFoundException e)
        {
            return false;
        }
    }

    public void process(Bundle bundle, final ConfigurableApplicationContext configurableApplicationContext)
    {
        final AutowiredAnnotationBeanPostProcessor autowireProcessor = new AutowiredAnnotationBeanPostProcessor();
        autowireProcessor.setAutowiredAnnotationType(Inject.class);
        final Plugin plugin = pluginAccessor.getPlugin(OsgiHeaderUtil.getPluginKey(bundle));

        configurableApplicationContext.addBeanFactoryPostProcessor(new BeanFactoryPostProcessor()
        {
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException
            {
                beanFactory.addBeanPostProcessor(autowireProcessor);
                //beanFactory.addBeanPostProcessor(new CommonAnnotationBeanPostProcessor());
                String scanPackage = plugin.getPluginInformation().getParameters().get("scanPackage");
                if (scanPackage != null)
                {
                    scanForModules(beanFactory, configurableApplicationContext, scanPackage);
                }
            }
        });
        CustomAutowireConfigurer configurer = new CustomAutowireConfigurer();
        configurer.setCustomQualifierTypes(Sets.newHashSet(Named.class));
        configurableApplicationContext.addBeanFactoryPostProcessor(configurer);
        configurableApplicationContext.addBeanFactoryPostProcessor(new HostComponentBeanFactoryPostProcessor());
    }

    private void scanForModules(ConfigurableListableBeanFactory beanFactory, ConfigurableApplicationContext configurableApplicationContext, String... basePackages)
    {
        long start = System.currentTimeMillis();
        ClassPathBeanDefinitionScanner scanner = configureScanner(configurableApplicationContext, beanFactory);
        scanner.setIncludeAnnotationConfig(false);
		int beansFound = scanner.scan(basePackages);
        log.info("Found {} modules for in package bases {} in {} ms", new Object[]{beansFound, asList(basePackages), (System.currentTimeMillis() - start)});
    }

    protected ClassPathBeanDefinitionScanner createScanner(BeanFactory beanFactory)
    {
        return new ClassPathBeanDefinitionScanner((BeanDefinitionRegistry)beanFactory, false);
    }

    protected ClassPathBeanDefinitionScanner configureScanner(ApplicationContext applicationContext, BeanFactory beanFactory)
    {
        //boolean useDefaultFilters = true;
        //if (element.hasAttribute(USE_DEFAULT_FILTERS_ATTRIBUTE)) {
        //	useDefaultFilters = Boolean.valueOf(element.getAttribute(USE_DEFAULT_FILTERS_ATTRIBUTE));
        //}

        // Delegate bean definition registration to scanner class.
        ClassPathBeanDefinitionScanner scanner = createScanner(beanFactory);
        scanner.addIncludeFilter(new AnnotationTypeFilter(PluginModule.class));
        scanner.setResourceLoader(applicationContext);
        scanner.setBeanDefinitionDefaults(new BeanDefinitionDefaults());
        //scanner.setAutowireCandidatePatterns(parserContext.getDelegate().getAutowireCandidatePatterns());

        /*
		try {
			parseBeanNameGenerator(element, scanner);
		}
		catch (Exception ex) {
			readerContext.error(ex.getMessage(), readerContext.extractSource(element), ex.getCause());
		}

		try {
			parseScope(element, scanner);
		}
		catch (Exception ex) {
			readerContext.error(ex.getMessage(), readerContext.extractSource(element), ex.getCause());
		}

		parseTypeFilters(element, scanner, readerContext);
		*/

        return scanner;
    }
}
