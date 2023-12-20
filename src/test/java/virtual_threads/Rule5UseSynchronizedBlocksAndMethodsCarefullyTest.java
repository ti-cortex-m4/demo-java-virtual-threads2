package virtual_threads;

import org.junit.jupiter.api.Test;

import java.util.concurrent.locks.ReentrantLock;

public class Rule5UseSynchronizedBlocksAndMethodsCarefullyTest {

    private final ReentrantLock reentrantLock = new ReentrantLock();

    @Test
    public void useReentrantLock() {
        reentrantLock.lock();
        try {
            exclusiveResource();
        } finally {
            reentrantLock.unlock();
        }
    }


    private final Object lockObject = new Object();

    public void useSynchronizedBlock() {
        synchronized (lockObject) {
            exclusiveResource();
        }
    }


    private void exclusiveResource() {
    }
}
