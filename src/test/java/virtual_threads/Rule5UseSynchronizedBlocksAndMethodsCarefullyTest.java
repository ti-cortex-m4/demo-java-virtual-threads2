package virtual_threads;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;

public class Rule5UseSynchronizedBlocksAndMethodsCarefullyTest {

    private final ReentrantLock reentrantLock = new ReentrantLock();

    @Test
    public void doTest() throws InterruptedException, ExecutionException {
        reentrantLock.lock();
        try {
            exclusiveResource();
        } finally {
            reentrantLock.unlock();
        }
    }

    private final Object lockObject = new Object();

    @Test
    public void doNotTest() throws InterruptedException, ExecutionException {
        synchronized (lockObject) {
            exclusiveResource();
        }
    }

    private void exclusiveResource() {
    }
}
