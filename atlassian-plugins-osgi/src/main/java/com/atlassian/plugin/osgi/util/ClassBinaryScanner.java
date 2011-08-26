package com.atlassian.plugin.osgi.util;

import aQute.lib.osgi.ClassDataCollector;
import aQute.lib.osgi.Clazz;
import aQute.lib.osgi.Resource;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

/**
 * Contains all the utility methods and classes for scanning class file binary.
 *
 * @since 2.10
 */
public class ClassBinaryScanner
{
    /**
     * Scans class binary to extract 1) referred classes 2) imported packages 3) the class's superclass.
     *
     * @param clazz the input class binary
     * @return result, never null
     * @throws java.io.IOException if the scan dies along the way
     *
     * @since 2.10
     */
    public static ScanResult scanClassBinary(Clazz clazz) throws IOException
    {
        final ImmutableSet.Builder<String> allReferredClasses = new ImmutableSet.Builder<String>();
        final String[] superClassName = new String[] { null };
        try
        {
            // TODO: Perhaps, we should start scanning for annotations as well. This ClassDataCollector in bndlib 1.4.3 is quite powerful.
            clazz.parseClassFileWithCollector(new ClassDataCollector() {

                @Override
                public void extendsClass(String name)
                {
                    superClassName[0] = name;
                    allReferredClasses.add(name);
                }

                @Override
                public void implementsInterfaces(String[] names)
                {
                    for(String name:names)
                    {
                        allReferredClasses.add(name);
                    }
                }

                @Override
                public void addReference(String name)
                {
                    // the class name is in the form "abc.def.ghi" instead of "abc/def/ghi" which is different from other methods for unknown reason.
                    allReferredClasses.add(StringUtils.replace(name, ".", "/"));
                }
            });
        }
        catch (Exception e)
        {
            throw new IOException("Error parsing class file", e);
        }

        return new ScanResult(allReferredClasses.build(), ImmutableSet.copyOf(clazz.getReferred()), superClassName[0]);
    }

    /**
     * Contains the result of class binary scanning.
     *
     * @since 2.10
     */
    public static class ScanResult
    {
        // this classes are referred to as strings since we don't want to load them all at this stage.
        private Set<String> referredClasses;
        private Set<String> referredPackages;
        private String superClass;

        public ScanResult(Set<String> referredClasses,  Set<String> referredPackages, String superClass)
        {
            this.referredClasses = referredClasses;
            this.referredPackages = referredPackages;
            this.superClass = superClass;
        }

        public Set<String> getReferredClasses()
        {
            return referredClasses;
        }

        public Set<String> getReferredPackages()
        {
            return referredPackages;
        }

        public String getSuperClass()
        {
            return superClass;
        }
    }

    /**
     * InputStream-based resource for class scanning purpose (in the format required by bndlib).
     *
     * @since 2.10
     */
    public static class InputStreamResource implements Resource
    {
        private String extra;
        private InputStream inputStream;

        public InputStreamResource(InputStream inputStream)
        {
            this.inputStream = inputStream;
        }

        public InputStream openInputStream() throws Exception
        {
            return inputStream;
        }

        public void write(OutputStream out) throws Exception
        {
            throw new UnsupportedOperationException("Not for write");
        }

        public long lastModified()
        {
            return -1;
        }

        public void setExtra(String extra)
        {
            this.extra = extra;
        }

        public String getExtra()
        {
            return extra;
        }

        public void close()
        {
            IOUtils.closeQuietly(inputStream);
        }
    }
}
