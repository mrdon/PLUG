package com.atlassian.plugin;

import com.atlassian.util.concurrent.NotNull;
import org.dom4j.Element;

import java.util.Map;

public interface ModuleDescriptor<T> extends Resourced
{
    /**
     * The complete key for this module, including the plugin key.
     * <p>
     * Format is plugin.key:module.key
     * </p>
     * 
     * @return The complete key for this module.
     * @see #getKey()
     * @see #getPluginKey()
     */
    String getCompleteKey();

    /**
     * The plugin key for this module, derived from the complete key.
     * 
     * @return The plugin key for this module.
     * @see #getKey()
     * @see #getCompleteKey()
     */
    String getPluginKey();

    /**
     * The key for this module, unique within the plugin.
     * 
     * @return The key for this module.
     * @see #getCompleteKey()
     * @see #getPluginKey()
     */
    String getKey();

    /**
     * A simple string name for this descriptor.
     * 
     * @return The name for this ModuleDescriptor.
     */
    String getName();

    /**
     * A simple description of this descriptor.
     * 
     * @return The description for this ModuleDescriptor.
     */
    String getDescription();

    /**
     * The class of the module this descriptor creates.
     * 
     * @return The class of the module this descriptor creates.
     * @see #getModule()
     */
    Class<T> getModuleClass();

    /**
     * The particular module object created by this plugin.
     * 
     * @return The module object created by this plugin.
     * @see #getModuleClass()
     */
    T getModule();

    /**
     * Initialise a module given it's parent plugin and the XML element
     * representing the module.
     * <p>
     * Since atlassian-plugins v2.2, you can no longer load classes from the
     * plugin in this method, because the OSGi bundle that they will live in is
     * not built yet. Load classes in the
     * {@link com.atlassian.plugin.descriptors.AbstractModuleDescriptor#enabled()}
     * method instead.
     * 
     * @param plugin The plugin that the module belongs to. Must not be null.
     * @param element XML element representing the module. Must not be null.
     * @throws PluginParseException Can be thrown if an error occurs while
     *             parsing the XML element.
     */
    void init(@NotNull Plugin plugin, @NotNull Element element) throws PluginParseException;

    /**
     * Whether or not this plugin module is enabled by default.
     * 
     * @return {@code true} if this plugin module is enabled by default.
     */
    boolean isEnabledByDefault();

    /**
     * Whether or not this plugin module is a "system" plugin that shouldn't be
     * made visible/disableable to the user.
     * 
     * @return {@code true} if this plugin module is a "system" plugin that
     *         shouldn't be made visible/disableable to the user.
     */
    boolean isSystemModule();

    /**
     * Override this if your plugin needs to clean up when it's been removed.
     * 
     * @param plugin TODO: The plugin parameter is redundant. The
     *            ModuleDescriptor must know its parent plugin in order to
     *            implement getPlugin()
     */
    void destroy(Plugin plugin);

    Float getMinJavaVersion();

    /**
     * If a min java version has been specified this will return true if the
     * running jvm is >= to the specified version. If this is not set then it is
     * treated as not having a preference.
     * 
     * @return true if satisfied, false otherwise.
     */
    boolean satisfiesMinJavaVersion();

    Map<String, String> getParams();

    /**
     * Key used to override {@link #getName()} when using internationalisation.
     * 
     * @return the i18n key. May be null.
     */
    String getI18nNameKey();

    /**
     * Key used to override {@link #getDescription()} when using
     * internationalisation.
     * 
     * @return the i18n key. May be null.
     */
    String getDescriptionKey();

    /**
     * @return The plugin this module descriptor is associated with
     */
    Plugin getPlugin();

    /**
	 * <p>Compares the specified object with this module descriptor for equality.</p>
	 *
     * <p>Returns <tt>true</tt> if the given object is also a module descriptor and the two descriptors have the same
     * &quot;complete key&quot; as determined by {@link #getCompleteKey()}.</p>
     *
     * This ensures that the <tt>equals</tt> method works properly across
	 * different implementations of the <tt>ModuleDescriptor</tt> interface.
	 *
	 * @param obj object to be compared for equality with this module descriptor.
	 * @return <tt>true</tt> if the specified object is equal to this module descriptor.
     * @since 2.8.0
     */
    boolean equals(Object obj);

    /**
     * Returns the hash code value for this module descriptor.  The hash code
	 * of a module descriptor <tt>d</tt> is defined to be: <pre>
	 *     getCompleteKey() == null ? 0 : getCompleteKey().hashCode()
         * </pre>
	 * This ensures that <tt>d1.equals(d2)</tt> implies that
	 * <tt>d1.hashCode()==d2.hashCode()</tt> for any two Module Descriptors
	 * <tt>d1</tt> and <tt>d2</tt>, as required by the general
	 * contract of <tt>Object.hashCode</tt>.
     *
     * @return the hash code value for this module descriptor.
     * @see Object#hashCode()
	 * @see Object#equals(Object)
	 * @see #equals(Object)
     * @since 2.8.0
     */
    int hashCode();
}
