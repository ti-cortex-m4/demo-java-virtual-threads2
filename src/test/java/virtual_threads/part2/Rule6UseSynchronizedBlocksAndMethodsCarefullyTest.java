package virtual_threads.part2;

import java.util.concurrent.locks.ReentrantLock;

public class /*TODO*/ Rule6UseSynchronizedBlocksAndMethodsCarefullyTest {

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
        return "result";
    }
}
