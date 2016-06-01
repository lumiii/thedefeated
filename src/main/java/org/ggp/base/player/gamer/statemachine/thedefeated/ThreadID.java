package org.ggp.base.player.gamer.statemachine.thedefeated;

import java.util.concurrent.atomic.AtomicInteger;

public class ThreadID
{
	private static final AtomicInteger nextId = new AtomicInteger(0);

    // Thread local variable containing each thread's ID
    private static final ThreadLocal<Integer> threadId = new ThreadLocal<>();

    private static final ThreadLocal<Boolean> initialized = new ThreadLocal<Boolean>() {
        @Override protected Boolean initialValue() {
            return false;
            }
        };

    public static void initializeThreadID()
    {
    	if (!initialized.get())
    	{
	    	threadId.set(nextId.getAndIncrement());
	    	System.out.println("Adding new thread ID: " + threadId.get());
	    	System.out.println("From " + Thread.currentThread().getName());
	    	initialized.set(true);
    	}
    }

    // Returns the current thread's unique ID, assigning it if necessary
    public static int get() {
    	if (!initialized.get())
    	{
    		initializeThreadID();
    	}

        return threadId.get();
    }
}
