package com.atlassian.plugin.osgi.spring;

import org.springframework.core.task.AsyncTaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Executes spring tasks using a cached thread pool that expands as necessary.  Overrides the default Spring executor
 * that spawns a new thread for every application context creation.
 *
 * @since 2.5.0
 */
public class ThreadPoolAsyncTaskExecutor implements AsyncTaskExecutor
{
    private final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory());

    /**
     * Executes the runnable
     * @param task The runnable task
     * @param startTimeout The start timeout (ignored)
     */
    public void execute(Runnable task, long startTimeout)
    {
        // yes, we ignore the start timeout
        executor.execute(task);
    }

    /**
     * Executes the runnable
     * @param task The runnable task
     */
    public void execute(Runnable task)
    {
        this.execute(task, -1);
    }

    /**
     * Thread factory that names the threads for the executor
     */
    private static class NamedThreadFactory implements ThreadFactory
    {
        private final AtomicInteger counter = new AtomicInteger();
        public Thread newThread(Runnable r)
        {
            Thread thread = new Thread(r);
            thread.setDaemon(false);
            thread.setName("Spring executor " + counter.incrementAndGet());
            return thread;
        }
    }
}
