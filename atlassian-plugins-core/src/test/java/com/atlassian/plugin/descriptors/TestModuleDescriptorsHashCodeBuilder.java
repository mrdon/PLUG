package com.atlassian.plugin.descriptors;

import com.atlassian.plugin.ModuleDescriptor;
import junit.framework.TestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Responsible for testing that the <tt>hashCode</tt> contract defined by
 * {@link com.atlassian.plugin.ModuleDescriptor#hashCode()} is fulfilled by the
 * {@link ModuleDescriptors.HashCodeBuilder} class.
 *
 * @since 2.8.0
 */
public class TestModuleDescriptorsHashCodeBuilder extends TestCase
{
    public void testTheHashCodeOfADescriptorWithANullCompleteKeyShouldBeZero()
    {
        final ModuleDescriptor descriptor = mock(ModuleDescriptor.class);

        when(descriptor.getCompleteKey()).thenReturn(null);

        assertEquals(new ModuleDescriptors.HashCodeBuilder().descriptor(descriptor).toHashCode(), 0);
    }

    public void testTheHashCodeOfADescriptorWithANonNullCompleteKeyIsEqualToTheHashCodeOfTheCompleteKey()
    {
        final ModuleDescriptor descriptor = mock(ModuleDescriptor.class);

        when(descriptor.getCompleteKey()).thenReturn("test-plugin:test-key");

        assertEquals
                (
                        new ModuleDescriptors.HashCodeBuilder().descriptor(descriptor).toHashCode(),
                        descriptor.getCompleteKey().hashCode()
                );
    }
}

