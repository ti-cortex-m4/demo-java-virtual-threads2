package virtual_threads;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Example4CreateStartedAndUnstartedVirtualThreadsTest {

    @Test
    public void createStartedThreadTest() throws InterruptedException {
        Thread.Builder builder = Thread.ofVirtual();
        Thread thread = builder.start(() -> { sleep(1000); System.out.println("run"); });
        assertEquals(Thread.State.RUNNABLE, thread.getState());
        thread.join();
    }

    @Test
    public void createUnstartedThreadTest() throws InterruptedException {
        Thread.Builder builder = Thread.ofVirtual();
        Thread thread = builder.unstarted(() -> System.out.println("run"));
        assertEquals(Thread.State.NEW, thread.getState());
        thread.start();
        assertEquals(Thread.State.RUNNABLE, thread.getState());
        thread.join();
    }

    private void sleep(int milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
