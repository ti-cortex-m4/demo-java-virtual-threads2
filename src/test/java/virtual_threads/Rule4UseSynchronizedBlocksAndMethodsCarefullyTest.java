package virtual_threads;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

public class Rule4UseSynchronizedBlocksAndMethodsCarefullyTest {

    @Test
    public void doTest() throws InterruptedException, ExecutionException {
    }

    @Test
    public void doNotTest() throws InterruptedException, ExecutionException {
    }



    public synchronized String accessResource1() {
        return access();
    }

    private static final ReentrantLock LOCK = new ReentrantLock();

    public String accessResource2() {
        LOCK.lock();
        try {
            return access();
        } finally {
            LOCK.unlock();
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
