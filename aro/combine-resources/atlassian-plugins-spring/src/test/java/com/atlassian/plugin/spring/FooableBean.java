package com.atlassian.plugin.spring;

import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.BeansException;

import java.io.Serializable;

@AvailableToPlugins
public class FooableBean implements Fooable, BeanFactoryAware, Serializable
{
    public void sayHi() {
        System.out.println("hi");
    }

    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
