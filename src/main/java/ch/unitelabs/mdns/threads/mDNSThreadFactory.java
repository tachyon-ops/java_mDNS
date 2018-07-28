package ch.unitelabs.mdns.threads;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class mDNSThreadFactory implements ThreadFactory {

    /** pool number. */
    private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);

    /** thread group. */
    private final ThreadGroup group;

    /** thread number. */
    private final AtomicInteger threadNumber;

    /** thread prefix. */
    private final String namePrefix;

    /**
     * Constructor.
     * <p>
     * Threads will be named 'mDNS-[suffix]-[unique pool number]-thread-[unique thread number for pool]
     *
     * @param suffix thread suffix
     */
    public mDNSThreadFactory(final String suffix) {
        final SecurityManager s = System.getSecurityManager();
        threadNumber = new AtomicInteger(1);
        group = s != null ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        namePrefix = "mDNS-" + suffix + "-" + POOL_NUMBER.getAndIncrement() + "-thread-";
    }

    @Override
    public final Thread newThread(final Runnable r) {
        final Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY);
        return t;
    }
}
