package com.atlassian.plugin.webresource;

import com.atlassian.plugin.FileCacheService;
import com.atlassian.plugin.FileCacheServiceImpl;
import com.atlassian.plugin.util.PluginUtils;
import junit.framework.TestCase;

import java.io.File;

import static org.mockito.Mockito.mock;

public class TestAbstractFileCacheResource extends TestCase
{

     private WebResourceIntegration mockWebResourceIntegration = mock(WebResourceIntegration.class);
    public void testNullServiceDisablesFileCache() throws Exception
    {
        AbstractFileCacheResource afcr = new NonAbstractFileCacheResource(null);
        assertFalse(afcr.isFileCacheEnabled());
    }

    public void testFileCacheEnabled() throws Exception
    {
        AbstractFileCacheResource afcr = new NonAbstractFileCacheResource(new FileCacheServiceImpl(mockWebResourceIntegration,10));
        assertTrue(afcr.isFileCacheEnabled());
    }

    public void testDevmodeDisableFileCache() throws Exception
    {
        AbstractFileCacheResource afcr = new NonAbstractFileCacheResource(new FileCacheServiceImpl(mockWebResourceIntegration,10));
        String value = System.getProperty(PluginUtils.ATLASSIAN_DEV_MODE);
        System.setProperty(PluginUtils.ATLASSIAN_DEV_MODE,"true");
        try
        {
            assertFalse(afcr.isFileCacheEnabled());
        }
        finally
        {
            if(value==null)
            {
                System.getProperties().remove(PluginUtils.ATLASSIAN_DEV_MODE);
            }
            else
            {
                System.setProperty(PluginUtils.ATLASSIAN_DEV_MODE,value);
            }
        }
    }

    private static class NonAbstractFileCacheResource extends AbstractFileCacheResource
    {
        private NonAbstractFileCacheResource(FileCacheService fileCacheService)
        {
            super(fileCacheService);
        }
    }
}
