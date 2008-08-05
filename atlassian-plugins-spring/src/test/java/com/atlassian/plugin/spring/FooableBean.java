package com.atlassian.plugin.spring;

import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.BeansException;

import java.io.Serializable;
import java.util.HashMap;

@AvailableToPlugins
public class FooableBean extends HashMap implements Fooable, BeanFactoryAware, Serializable
{
    public void sayHi() {
        System.out.println("hi");
    }

    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    }
}
