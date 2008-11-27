package com.atlassian.plugin.osgi.factory.transform;

/**
 * Performes an stage in the transformation from a JAR to an OSGi bundle
 *
 * @since 2.2.0
 */
public interface TransformStage<T extends TransformContext>
{
    /**
     * Transforms the jar by operating on the context
     * @param context The transform context to operate on
     * @throws PluginTransformationException If the stage cannot be performed and the whole operation should be aborted
     */
    void execute(T context)
        throws PluginTransformationException;

}
