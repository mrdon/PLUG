package com.atlassian.labs.plugins3.spring;

import java.util.Map;

import javax.inject.Provider;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

public class DefaultProvider<T> implements Provider<T>, BeanFactoryAware,
        InitializingBean {

    private BeanFactory beanFactory;
    final Class<T> type;

    public DefaultProvider(Class<T> type) {
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    public T get() {
        ListableBeanFactory listableBeanFactory = (ListableBeanFactory) this.beanFactory;
        Map beans = listableBeanFactory.getBeansOfType(type);

        if (beans.values().size() != 1) {
            throw new NoSuchBeanDefinitionException(
                    type.getName(),
                    "No unique bean of type ["
                            + type.getName()
                            + "] is defined: expected single matching bean but found "
                            + beans.values().size() + ": "
                            + beans.keySet().toString());
        }

        return (T) beans.values().iterator().next();
    }

    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    public void afterPropertiesSet() throws Exception {
        if (!(this.beanFactory instanceof ListableBeanFactory)) {
            throw new IllegalArgumentException(
                    "bean factory must be a ListableBeanFactory");
        }
    }

}