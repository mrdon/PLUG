package com.atlassian.plugin.scripting;

import org.mozilla.javascript.Script;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.apache.commons.io.IOUtils;

import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

import com.atlassian.plugin.util.concurrent.CopyOnWriteMap;
import com.atlassian.plugin.scripting.variables.JsScript;
import com.atlassian.plugin.PluginException;

/**
 * Created by IntelliJ IDEA.
 * User: mrdon
 * Date: Dec 14, 2008
 * Time: 8:52:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class ScriptManager
{
    private final Scriptable sharedScope;
    private final Map<String,Script> scripts = CopyOnWriteMap.newHashMap();

    public ScriptManager()
    {
        this.sharedScope = buildSharedScope();
    }

    void ensureCompiled(String path, InputStream in) throws IOException
    {
        if (!scripts.containsKey(path))
        {
            Context cx = Context.enter();
            try
            {
                InputStreamReader reader = new InputStreamReader(in);
                Script script = cx.compileReader(reader, path, 1, null);
                scripts.put(path, script);
            }
            finally
            {
                Context.exit();
            }
        }
    }

    Scriptable buildSharedScope()
    {
        Context cx = Context.enter();
        try
        {
            return cx.initStandardObjects();
        }
        finally
        {
            Context.exit();
        }
    }

    public void runInSharedScope(String path, InputStream in)
    {
        Context cx = Context.enter();
        try
        {
            cx.evaluateReader(sharedScope, new InputStreamReader(in), path, 1, null);
        }
        catch (IOException e)
        {
            throw new PluginException("Unable to run shared script "+path, e);
        }
        finally
        {
            Context.exit();
        }
    }

    public JsScript run(ClassLoader classLoader, String path, Map<String,Object> variables) throws IOException
    {

        InputStream in = null;
        try
        {
            in = classLoader.getResourceAsStream(path);
            return run(path, classLoader.getResourceAsStream(path), variables);
        }
        finally
        {
            IOUtils.closeQuietly(in);
        }
    }

    public JsScript run(String path, InputStream in,Map<String,Object> variables) throws IOException
    {
        Context cx = Context.enter();
        try
        {
            ensureCompiled(path, in);
            Script script = scripts.get(path);

            // We can share the scope.
            Scriptable threadScope = cx.newObject(sharedScope);
            threadScope.setPrototype(sharedScope);

            // We want "threadScope" to be a new top-level
            // scope, so set its parent scope to null. This
            // means that any variables created by assignments
            // will be properties of "threadScope".
            threadScope.setParentScope(null);


            JsScript jsScript = new JsScript(path);
            Map<String,Object> vars = new HashMap<String,Object>(variables);
            vars.put("script", jsScript);

            for (Map.Entry<String,Object> entry : vars.entrySet())
            {
                Object wrapped = Context.javaToJS(entry.getValue(), threadScope);
                ScriptableObject.putProperty(threadScope, entry.getKey(), wrapped);
            }

            Object obj = script.exec(cx, threadScope);
            if (jsScript.getResult() == null)
            {
                jsScript.setResult(Context.jsToJava(obj, Object.class));
            }
            return jsScript;
        }
        finally
        {
            Context.exit();
        }
    }
}
