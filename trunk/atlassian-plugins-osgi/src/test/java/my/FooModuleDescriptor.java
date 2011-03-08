package my;

import com.atlassian.plugin.descriptors.AbstractModuleDescriptor;
import com.atlassian.plugin.module.ModuleFactory;

public class FooModuleDescriptor extends AbstractModuleDescriptor<FooModule> {

   public FooModuleDescriptor(ModuleFactory moduleFactory) {
       super(moduleFactory);
   }

   public FooModule getModule() {
       return moduleFactory.createModule(moduleClassName, this);
   }

}