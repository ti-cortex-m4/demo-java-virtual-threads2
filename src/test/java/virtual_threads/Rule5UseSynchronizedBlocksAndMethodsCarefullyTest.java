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

    public synchronized String accessResource1() {
        return access();
    }


    private final ReentrantLock reentrantLock = new ReentrantLock();

    public String accessResource2() {
        reentrantLock.lock();
        try {
            return access();
        } finally {
            reentrantLock.unlock();
        }
    }

    private String access () {
        return "";
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
