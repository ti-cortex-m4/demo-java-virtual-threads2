package virtual_threads;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;

public class Rule5UseSynchronizedBlocksAndMethodsCarefullyTest {

    @Test
    public void doTest() throws InterruptedException, ExecutionException {
    }

    @Test
    public void doNotTest() throws InterruptedException, ExecutionException {
    }


    private final Object lockObject = new Object();

    public void accessResource1() {
        synchronized(lockObject) {
            exclusiveResource();
        }
    }


    private final ReentrantLock reentrantLock = new ReentrantLock();

    public void accessResource2() {
        reentrantLock.lock();
        try {
            exclusiveResource();
        } finally {
            reentrantLock.unlock();
        }
    }

    private void exclusiveResource() {
    }


/*
    synchronized(lockObj) {
        frequentIO();
    }

    lock.lock();
    try {
        frequentIO();
    } finally {
        lock.unlock();
    }
*/
}
