package virtual_threads.part1;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Example4CreateStartedAndUnstartedVirtualThreadsTest {

    @Test
    public void createStartedThreadTest() throws InterruptedException {
        Thread.Builder builder = Thread.ofVirtual();

        Thread startedThread = builder.start(() -> { sleep(1000); System.out.println("run"); });
        assertEquals(Thread.State.RUNNABLE, startedThread.getState());
        startedThread.join();

        Thread unstartedThread = builder.unstarted(() -> System.out.println("run"));
        assertEquals(Thread.State.NEW, unstartedThread.getState());
        unstartedThread.start();
        assertEquals(Thread.State.RUNNABLE, unstartedThread.getState());
        unstartedThread.join();
    }

    @Test
    public void createUnstartedThreadTest() throws InterruptedException {
        Thread.Builder builder = Thread.ofVirtual();
        Thread unstartedThread = builder.unstarted(() -> System.out.println("run"));
        assertEquals(Thread.State.NEW, unstartedThread.getState());
        unstartedThread.start();
        assertEquals(Thread.State.RUNNABLE, unstartedThread.getState());
        unstartedThread.join();
    }

    private void sleep(int milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
