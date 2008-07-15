package com.atlassian.plugin.web.descriptors;

import java.util.Comparator;

/**
 * A simple comparator for any weighted descriptor - lowest weights first.
 */
public class WeightedDescriptorComparator implements Comparator
{
    public int compare(Object o1, Object o2)
    {
        WeightedDescriptor w1 = (WeightedDescriptor)o1;
        WeightedDescriptor w2 = (WeightedDescriptor)o2;
        if (w1.getWeight() < w2.getWeight())
            return -1;
        else if (w1.getWeight() > w2.getWeight())
            return 1;
        else
            return 0;
    }
}
