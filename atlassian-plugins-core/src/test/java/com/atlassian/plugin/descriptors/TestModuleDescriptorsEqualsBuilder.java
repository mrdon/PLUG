package com.atlassian.plugin.descriptors;

import com.atlassian.plugin.ModuleDescriptor;
import junit.framework.TestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Responsible for testing that the <tt>equals</tt> contract defined by
 * {@link com.atlassian.plugin.ModuleDescriptor#equals(Object)} is fulfilled by the
 * {@link ModuleDescriptors.EqualsBuilder} class.
 *
 * @since 2.8.0
 */
public class TestModuleDescriptorsEqualsBuilder extends TestCase
{
    public void testAModuleDescriptorMustNotBeEqualToNull()
    {
        final ModuleDescriptor descriptor = mock(ModuleDescriptor.class);

        assertFalse(new ModuleDescriptors.EqualsBuilder().descriptor(descriptor).isEqualTo(null));
    }

    public void testModuleDescriptorsShouldBeEqualIfTheyHaveTheSameCompleteKey()
    {
        final ModuleDescriptor descriptor1 = mock(ModuleDescriptor.class);
        final ModuleDescriptor descriptor2 = mock(ModuleDescriptor.class);

        when(descriptor1.getCompleteKey()).thenReturn("abc:xyz");
        when(descriptor2.getCompleteKey()).thenReturn("abc:xyz");

        assertTrue(new ModuleDescriptors.EqualsBuilder().descriptor(descriptor1).isEqualTo(descriptor2));
    }

    public void testModuleDescriptorsShouldBeUnEqualIfTheyDoNotHaveTheSameCompleteKey()
    {
        final ModuleDescriptor descriptor1 = mock(ModuleDescriptor.class);
        final ModuleDescriptor descriptor2 = mock(ModuleDescriptor.class);

        when(descriptor1.getCompleteKey()).thenReturn("abc:xyz");
        when(descriptor2.getCompleteKey()).thenReturn("def:xyz");

        assertFalse(new ModuleDescriptors.EqualsBuilder().descriptor(descriptor1).isEqualTo(descriptor2));
    }

    public void testAModuleDescriptorMustNotBeEqualToAnObjectThatIsNotAModuleDescriptor()
    {
        final ModuleDescriptor descriptor = mock(ModuleDescriptor.class);
        final Object anObjectThatIsNotAModuleDescriptor = new Object();

        assertFalse(new ModuleDescriptors.EqualsBuilder().descriptor(descriptor).isEqualTo(anObjectThatIsNotAModuleDescriptor));
    }

    public void testAModuleDescriptorShouldBeEqualToItSelf()
    {
        final ModuleDescriptor descriptor = mock(ModuleDescriptor.class);

        assertTrue(new ModuleDescriptors.EqualsBuilder().descriptor(descriptor).isEqualTo(descriptor));
    }

    public void testEqualsImplementationIsReflexiveForTwoModuleDescriptorsWithTheSameCompleteKey()
    {
        final ModuleDescriptor descriptor1 = mock(ModuleDescriptor.class);
        final ModuleDescriptor descriptor2 = mock(ModuleDescriptor.class);

        when(descriptor1.getCompleteKey()).thenReturn("abc:xyz");
        when(descriptor2.getCompleteKey()).thenReturn("abc:xyz");

        assertTrue(new ModuleDescriptors.EqualsBuilder().descriptor(descriptor1).isEqualTo(descriptor2));
        assertTrue(new ModuleDescriptors.EqualsBuilder().descriptor(descriptor2).isEqualTo(descriptor1));
    }

    public void testEqualsImplementationIsTransitiveForThreeDescriptorsWithTheSameCompleteKey()
    {
        final ModuleDescriptor descriptor1 = mock(ModuleDescriptor.class);
        final ModuleDescriptor descriptor2 = mock(ModuleDescriptor.class);
        final ModuleDescriptor descriptor3 = mock(ModuleDescriptor.class);


        when(descriptor1.getCompleteKey()).thenReturn("abc:xyz");
        when(descriptor2.getCompleteKey()).thenReturn("abc:xyz");
        when(descriptor3.getCompleteKey()).thenReturn("abc:xyz");

        assertTrue(new ModuleDescriptors.EqualsBuilder().descriptor(descriptor1).isEqualTo(descriptor2));
        assertTrue(new ModuleDescriptors.EqualsBuilder().descriptor(descriptor2).isEqualTo(descriptor3));
        assertTrue(new ModuleDescriptors.EqualsBuilder().descriptor(descriptor1).isEqualTo(descriptor3));
    }
}
