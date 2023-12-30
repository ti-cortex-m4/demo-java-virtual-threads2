package virtual_threads;

import java.util.concurrent.locks.ReentrantLock;

public class Rule5UseSynchronizedBlocksAndMethodsCarefullyTest {

    private final Object lockObject = new Object();

    public String useSynchronizedBlockForExclusiveAccess() {
        synchronized (lockObject) {
            return exclusiveResource();
        }
    }


    private final ReentrantLock reentrantLock = new ReentrantLock();

    public String useReentrantLockForExclusiveAccess() {
        reentrantLock.lock();
        try {
            return exclusiveResource();
        } finally {
            reentrantLock.unlock();
        }
    }


    private String exclusiveResource() {
        return "";
    }
}
