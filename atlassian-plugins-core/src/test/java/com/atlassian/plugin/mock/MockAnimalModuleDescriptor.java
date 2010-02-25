package com.atlassian.plugin.mock;

import com.atlassian.plugin.module.ModuleClassFactory;
import junit.framework.Assert;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.StateAware;
import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;

import org.dom4j.Element;

public class MockAnimalModuleDescriptor extends AbstractModuleDescriptor<MockAnimal> implements StateAware, ModuleDescriptor<MockAnimal>
{
    MockAnimal module;
    public boolean disabled;
    public boolean enabled;
    
    private final String type;
    private final String name;
    

    public MockAnimalModuleDescriptor()
    {
      this(null, null);
    }
    
    public MockAnimalModuleDescriptor(String type, String name)
    {
      super(ModuleClassFactory.NOOP_MODULE_CREATOR);
      this.type = type;
      this.name = name;
    }

    @Override
    public void init(final Plugin plugin, final Element element) throws PluginParseException
    {
        super.init(plugin, element);
        if (type != null && name != null) {
          Assert.assertNotNull(plugin.getResourceDescriptor(type, name));
        }
    }

    @Override
    public MockAnimal getModule()
    {
        if (module == null)
        {
            try
            {
                module = getModuleClass().newInstance();
            }
            catch (final InstantiationException e)
            {
                throw new PluginParseException(e);
            }
            catch (final IllegalAccessException e)
            {
                throw new PluginParseException(e);
            }
        }
        return module;
    }

    @Override
    public void enabled()
    {
        super.enabled();
        enabled = true;
    }

    @Override
    public void disabled()
    {
        disabled = true;
        super.disabled();
    }

    public boolean isEnabled()
    {
        return enabled && !disabled;
    }
}
