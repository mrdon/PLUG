package com.atlassian.plugin.webresource;

import junit.framework.TestCase;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.atlassian.plugin.elements.ResourceDescriptor;
import com.atlassian.plugin.webresource.batch.BatchResource;
import com.atlassian.plugin.webresource.PluginResource;
import org.dom4j.DocumentHelper;
import org.dom4j.DocumentException;

public class TestWebResourceModuleDescriptor extends TestCase
{
    private static final String COMPLETE_KEY = "test.plugin:webresources";
    private WebResourceModuleDescriptor descriptor;

    protected void setUp() throws Exception
    {
        super.setUp();

        final List<ResourceDescriptor> resourceDescriptors = new ArrayList<ResourceDescriptor>();

        resourceDescriptors.add(createResourceDescriptor("plugin.css"));
        resourceDescriptors.add(createResourceDescriptor("master.css"));
        resourceDescriptors.add(createResourceDescriptor("master-ie.css"));
        resourceDescriptors.add(createResourceDescriptor("master.js"));

        descriptor = new WebResourceModuleDescriptor() {
            public String getCompleteKey()
            {
                return COMPLETE_KEY;
            }

            public List getResourceDescriptors()
            {
                return resourceDescriptors;
            }

            public List getResourceDescriptors(String type)
            {
                return resourceDescriptors;
            }
        };
    }

    protected void tearDown() throws Exception
    {
        descriptor = null;
        super.tearDown();
    }

    public void testGetPluginResources() throws Exception
    {
        // css batch no params
        BatchResource cssBatch = new BatchResource("css", COMPLETE_KEY, Collections.EMPTY_MAP);
        List<PluginResource> resources = descriptor.getPluginResources(cssBatch);
        assertEquals(3, resources.size());
        // order matters
        PluginResource resource = resources.get(0);
        assertEquals("plugin.css", resource.getResourceName());
        resource = resources.get(1);
        assertEquals("master.css", resource.getResourceName());
        resource = resources.get(2);
        assertEquals("master-ie.css", resource.getResourceName());

        // css batch with ieonly param
        Map<String, String> params = new TreeMap<String, String>();
        params.put("ieonly", "true");
        BatchResource ieOnlyBatch = new BatchResource("css", COMPLETE_KEY, params);
        resources = descriptor.getPluginResources(ieOnlyBatch);
        assertEquals(1, resources.size());
        resource = resources.get(0);
        assertEquals("master-ie.css", resource.getResourceName());
        assertEquals(COMPLETE_KEY, resource.getModuleCompleteKey());

        // js batch
        BatchResource jsBatch = new BatchResource("js", COMPLETE_KEY, Collections.EMPTY_MAP);
        resources = descriptor.getPluginResources(jsBatch);
        assertEquals(1, resources.size());
        resource = resources.get(0);
        assertEquals("master.js", resource.getResourceName());

        // unknown batch
        BatchResource unknownBatch = new BatchResource("txt", COMPLETE_KEY, Collections.EMPTY_MAP);
        resources = descriptor.getPluginResources(unknownBatch);
        assertEquals(0, resources.size());
    }

    public void testGetBatchResources()
    {
        List<BatchResource> batches = descriptor.getBatchResources();
        assertEquals(3, batches.size());

        BatchResource batch1 = batches.get(0);
        BatchResource cssBatch = new BatchResource("css", COMPLETE_KEY, Collections.EMPTY_MAP);
        assertEquals(cssBatch, batch1);        

        BatchResource batch2 = batches.get(1);
        Map<String, String> params = new TreeMap<String, String>();
        params.put("ieonly", "true");
        BatchResource ieOnlyBatch = new BatchResource("css", COMPLETE_KEY, params);
        assertEquals(ieOnlyBatch, batch2);

        BatchResource batch3 = batches.get(2);
        BatchResource jsBatch = new BatchResource("js", COMPLETE_KEY, Collections.EMPTY_MAP);
        assertEquals(jsBatch, batch3);
    }

    private ResourceDescriptor createResourceDescriptor(String resourceName) throws DocumentException
    {
        String xml = "<resource type=\"download\" name=\"" + resourceName + "\" location=\"/includes/css/" + resourceName + "\">\n" +
                            "<param name=\"source\" value=\"webContext\"/>\n";

        if(resourceName.indexOf("ie") != -1)
           xml += "<param name=\"ieonly\" value=\"true\"/>\n";

        xml += "</resource>";
        return new ResourceDescriptor(DocumentHelper.parseText(xml).getRootElement());
    }
}
