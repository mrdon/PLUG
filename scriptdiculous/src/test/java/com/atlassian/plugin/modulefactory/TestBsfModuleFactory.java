package com.atlassian.plugin.modulefactory;

import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.util.ClassLoaderUtils;
import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.mock.MockMineral;
import junit.framework.TestCase;
import org.easymock.MockControl;
import org.apache.bsf.BSFManager;
import org.apache.bsf.BSFException;
import org.apache.bsf.util.IOUtils;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 * Unit test for {@link com.atlassian.plugin.modulefactory.BsfModuleFactory}.
 */
public class TestBsfModuleFactory extends TestCase
{
    static String GROOVY_WEIGHT_SCRIPT = "import com.atlassian.plugin.mock.MockMineral\n" +
                "class MyGroovyClass implements MockMineral {\n" +
                "\n" +
                "    public int getWeight() {\n" +
                "            return 1972\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "return new MyGroovyClass()\n";

    public void testGetInterfaceModuleWithGroovy() throws PluginParseException
    {
        ResourceLocation groovyResourceLocation = new ResourceLocation("location", "groovy", "script", "text/groovy", GROOVY_WEIGHT_SCRIPT, null);

        // set up the mock module descriptor
        MockControl moduleDescriptorMockCtrl = MockControl.createNiceControl(ModuleDescriptor.class);
        ModuleDescriptor mockModuleDescriptor = (ModuleDescriptor) moduleDescriptorMockCtrl.getMock();
        mockModuleDescriptor.getModuleClass();
        moduleDescriptorMockCtrl.setReturnValue(MockMineral.class);
        mockModuleDescriptor.getResourceDescriptors("script");
        final List resourceDescriptors = makeMockResourceDescriptorList(GROOVY_WEIGHT_SCRIPT);

        moduleDescriptorMockCtrl.setReturnValue(resourceDescriptors);
        moduleDescriptorMockCtrl.setReturnValue(groovyResourceLocation);
        moduleDescriptorMockCtrl.replay();

        // test our BsfModuleFactory such that the returned module executes our groovy script
        BsfModuleFactory factory = new BsfModuleFactory();
        HashMap config = new HashMap();
        config.put("language", "groovy");
        config.put("moduleDescriptor", mockModuleDescriptor);
        //factory.configure(config);
        MockMineral module = (MockMineral) factory.getModule();
        assertEquals(1972, module.getWeight());

        moduleDescriptorMockCtrl.verify();
    }

    private List makeMockResourceDescriptorList(final String groovyWeightScript)
    {
        List resourceDescriptors = new ArrayList();
        MockControl resourceDescriptorMockCtrl = MockControl.createNiceControl(ResourceDescriptor.class);
        ResourceDescriptor mockResourceDescriptor = (ResourceDescriptor) resourceDescriptorMockCtrl.getMock();
        mockResourceDescriptor.getName();
        resourceDescriptorMockCtrl.setReturnValue("groovy");
        mockResourceDescriptor.getContent();
        resourceDescriptorMockCtrl.setReturnValue(groovyWeightScript);
        resourceDescriptorMockCtrl.replay();
        resourceDescriptors.add(mockResourceDescriptor);
        return resourceDescriptors;
    }

    public void testBsfInGeneral() throws BSFException, IOException
    {
        BSFManager manager = new BSFManager();
        String scriptSource = getResourceAsString("GroovySubclass.gy");
        final Object subclass = manager.eval("groovy", "GroovySubclass.gy", 0, 0, scriptSource);
        TestSuperclass superClass = (TestSuperclass) subclass;
        final String result = superClass.someMethod();
        System.out.println(result);
    }

    public void testGetModuleImplementation() {
        BsfModuleFactory mf = new BsfModuleFactory();
        MockMineral module = (MockMineral) mf.getModuleImplementation("groovy", GROOVY_WEIGHT_SCRIPT, MockMineral.class);
        assertEquals(1972, module.getWeight());
    }

    public void testJavascript() throws IOException, BSFException
    {
        BSFManager manager = new BSFManager();
        String scriptSource = getResourceAsString("JavascriptSubclass.js");
        final Object subclass = manager.eval("javascript", "JavascriptSubclass.js", 0, 0, scriptSource);
        TestSuperclass superClass = (TestSuperclass) subclass;
        final String result = superClass.someMethod();
        System.out.println(result);
    }

    public static String getResourceAsString(String filename) throws IOException
    {
        return IOUtils.getStringFromReader(new InputStreamReader(ClassLoaderUtils.getResourceAsStream(filename, TestBsfModuleFactory.class)));
    }

}
