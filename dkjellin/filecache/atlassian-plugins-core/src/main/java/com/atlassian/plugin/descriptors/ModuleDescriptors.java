package com.atlassian.plugin.descriptors;

import com.atlassian.plugin.ModuleDescriptor;

import javax.annotation.concurrent.NotThreadSafe;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since v2.8
 */
public final class ModuleDescriptors
{
    /**
     * <p>Assists in implementing a consistent implementation of {@link ModuleDescriptor#equals(Object)} methods for
     * module descriptors based on the complete key of the descriptor.</p>
     *
     * <p>The full specification of the <tt>equals(Object obj)</tt> contract is defined by
     * {@link ModuleDescriptor#equals(Object)}</p>
     *
     * <p>Usage:
     * <ol>
     *     <li>If you are using this builder to implement the <tt>equals(Object obj)</tt> method in a module descriptor
     *     implementation:
     *     <p>
     *          <code>
     *              new ModuleDescriptors.EqualsBuilder().descriptor(this).isEqualTo(obj);
     *          </code>
     *     </p>
     *     </li>
     *     <li>If you are using this builder to compare descriptors from outside a module descriptor implementation;
     *     given two descriptor instances, <tt>descriptor1</tt> and <tt>descriptor2</tt>:
     *     <p>
     *          <code>
     *              new ModuleDescriptors.EqualsBuilder().descriptor(descriptor1).isEqualTo(descriptor2);
     *          </code>
     *     </p>
     *     </li>
     * </ol>
     * </p>
     *
     * @since 2.8.0
     */
    @NotThreadSafe
    public static class EqualsBuilder
    {
        private ModuleDescriptor descriptor;

        /**
         * Sets the module descriptor to create an <code>equals</code> implementation for.
         *
         * @param descriptor the module descriptor.
         * @return this builder instance.
         */
        public EqualsBuilder descriptor(final ModuleDescriptor descriptor)
        {
            checkNotNull(descriptor, "Tried to build an equals implementation for a null module descriptor. " +
                    "This is not allowed.");
            this.descriptor = descriptor;
            return this;
        }

        /**
         * <p>Returns <tt>true</tt> if the given object is also a module descriptor and the two descriptors have the same
         * &quot;complete key&quot; as determined by {@link com.atlassian.plugin.ModuleDescriptor#getCompleteKey()}.</p>
         *
         * @param obj object to be compared for equality with this module descriptor.
         * @return <tt>true</tt> if the specified object is equal to this module descriptor.
         */
        public boolean isEqualTo(final Object obj)
        {
            checkNotNull(descriptor, "Tried to build an equals implementation for a null module descriptor. " +
                    "This is not allowed.");

            if (descriptor == obj) { return true; }

            if (!(obj instanceof ModuleDescriptor)) { return false; }

            ModuleDescriptor rhs = (ModuleDescriptor) obj;

            return new org.apache.commons.lang.builder.EqualsBuilder().
                    append(descriptor.getCompleteKey(), rhs.getCompleteKey()).
                    isEquals();
        }
    }

    /**
     * <p>Assists in implementing {@link Object#hashCode()} methods for module descriptors based on the <code>hashCode</code>
     * of their complete key.</p>
     *
     * <p>The full specification of the <tt>hashCode()</tt> contract is defined by
     * {@link ModuleDescriptor#hashCode()}</p>
     *
     * <p>Usage:
     * <ol>
     *     <li>If you are using this builder to implement the <tt>hashCode()</tt> method in a module descriptor
     *     implementation:
     *     <p>
     *          <code>
     *              new ModuleDescriptors.HashCodeBuilder().descriptor(this).toHashCode();
     *          </code>
     *     </p>
     *     </li>
     *     <li>If you are using this builder to calculate the hashCode of a descriptor from outside a module descriptor
     *     implementation; given a descriptor instance <tt>desc</tt>:
     *     <p>
     *          <code>
     *              new ModuleDescriptors.EqualsBuilder().descriptor(desc).toHashCode();
     *          </code>
     *     </p>
     *     </li>
     * </ol>
     * </p>
     *
     * @since 2.8.0
     */
    @NotThreadSafe
    public static class HashCodeBuilder
    {
        private ModuleDescriptor descriptor;

        /**
         * Sets the module descriptor to create a <code>hashCode</code> implementation for.
         *
         * @param descriptor the descriptor. Must not be null.
         * @return this builder.
         */
        public HashCodeBuilder descriptor(final ModuleDescriptor descriptor)
        {
            checkNotNull(descriptor, "Tried to calculate the hash code of a null module descriptor.");
            this.descriptor = descriptor;
            return this;
        }

        /**
         * Return the computed <code>hashCode</code> for this module descriptor.
         *
         * @return <code>hashCode</code> based on the hashCode of the complete key of the module descriptor.
         */
        public int toHashCode()
        {
            checkNotNull(descriptor, "Tried to calculate the hash code of a null module descriptor.");
            return descriptor.getCompleteKey() == null ? 0 : descriptor.getCompleteKey().hashCode();
        }

        /**
         * The computed <code>hashCode</code> from toHashCode() is returned due to the likelihood
         * of bugs in mis-calling toHashCode() and the unlikeliness of it mattering what the hashCode for
         * HashCodeBuilder itself is.
         *
         * @return <code>hashCode</code> based on the complete key of the module descriptor.
         */
        public int hashCode()
        {
            return toHashCode();
        }
    }
}
