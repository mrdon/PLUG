package com.atlassian.plugin.util;

import java.util.Iterator;

/**
 * Read-only wrapper for an iterator
 *
 * @since 2.6.0
 */
public class ReadOnlyIterator<T> implements Iterator<T>, Iterable<T>
{
    private final Iterator<T> delegate;

    public ReadOnlyIterator(Iterator<T> delegate)
    {
        this.delegate = delegate;
    }

    public boolean hasNext()
    {
        return delegate.hasNext();
    }

    public T next()
    {
        return delegate.next();
    }

    /**
     * @throws UnsupportedOperationException Always
     */
    public void remove() throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException("Remove not allowed");
    }

    public Iterator<T> iterator()
    {
        return this;
    }
}
