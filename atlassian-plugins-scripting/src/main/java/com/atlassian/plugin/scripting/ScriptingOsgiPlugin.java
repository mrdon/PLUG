package com.atlassian.plugin.scripting;

import com.atlassian.plugin.osgi.factory.OsgiPlugin;
import com.atlassian.plugin.osgi.container.OsgiContainerException;
import com.atlassian.plugin.scripting.variables.JsScript;
import com.atlassian.plugin.util.concurrent.CopyOnWriteMap;
import org.objectweb.asm.ClassWriter;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.V1_5;
import org.osgi.framework.Bundle;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: mrdon
 * Date: Dec 13, 2008
 * Time: 8:25:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class ScriptingOsgiPlugin extends OsgiPlugin
{
    private final ScriptingClassLoader scriptingClassLoader;

    private final Map<String,Class<?>> scriptClasses;
    private final Map<Class<?>, Object> scriptObjects;

    public ScriptingOsgiPlugin(final Bundle bundle, ScriptManager scriptManager)
    {
        super(bundle);

        scriptingClassLoader = new ScriptingClassLoader(super.getClassLoader(), scriptManager);
        scriptObjects = CopyOnWriteMap.newHashMap();
        scriptClasses = CopyOnWriteMap.newHashMap();
    }

    public ClassLoader getClassLoader()
    {
        return scriptingClassLoader;
    }

    public <T> Class<T> loadClass(String clazz, Class<?> callingClass) throws ClassNotFoundException
    {
        return (Class<T>) scriptingClassLoader.loadClass(clazz);
    }

    public synchronized <T> T autowire(Class<T> clazz, AutowireStrategy autowireStrategy)
    {
        if (scriptObjects.containsKey(clazz))
        {
            return (T) scriptObjects.get(clazz);
        }
        else
        {
            return super.autowire(clazz, autowireStrategy);    //To change body of overridden methods use File | Settings | File Templates.
        }
    }

    private class ScriptingClassLoader extends ClassLoader
    {
        private final ScriptManager scriptManager;
        protected ScriptingClassLoader(ClassLoader classLoader, ScriptManager scriptManager)
        {
            super(classLoader);
            this.scriptManager = scriptManager;
        }

        protected Class<?> findClass(String className) throws ClassNotFoundException
        {
            if (className.endsWith(".js"))
            {
                if (!scriptClasses.containsKey(className))
                {
                    JsScript script;
                    try
                    {
                        URL url = getResource(className);
                        if (url == null)
                            return super.findClass(className);
                        script = scriptManager.run(url, Collections.<String, Object>emptyMap());
                    }
                    catch (IOException e)
                    {
                        throw new ClassNotFoundException("Unable to compile and run "+className, e);
                    }
                    Object obj = script.getResult();
                    String genClassName = "tmp/"+className.substring(0, className.length() - 3);
                    ClassWriter cw = new ClassWriter(0);
                    cw.visit(V1_5, ACC_PUBLIC, genClassName, null, "java/lang/Object", null);
                    cw.visitEnd();
                    byte[] b = cw.toByteArray();
                    Class objClass = defineClass(genClassName.replace("/", "."), b, 0, b.length);
                    scriptObjects.put(objClass, obj);
                    scriptClasses.put(className, objClass);
                }
                return scriptClasses.get(className);
            }
            return super.findClass(className);
        }
    }
}
