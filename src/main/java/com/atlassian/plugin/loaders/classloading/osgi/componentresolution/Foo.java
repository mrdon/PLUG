package com.atlassian.plugin.loaders.classloading.osgi.componentresolution;

import java.util.jar.JarFile;
import java.util.zip.ZipFile;
import java.io.IOException;

public class Foo
{
    public static void main(String[] args) throws IOException
    {
        ZipFile file = new ZipFile("/Users/dbrown/dev/confluence/conf-webapp/src/main/webapp/WEB-INF/framework-bundles/spring-context-connector-1.0-SNAPSHOT.jar");
        file.getEntry("META-INF/MANIFEST.MF");
    }
}
