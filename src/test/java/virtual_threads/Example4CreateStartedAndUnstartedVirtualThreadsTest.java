package virtual_threads;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Example4CreateStartedAndUnstartedVirtualThreadsTest {

    @Test
    public void createStartedThreadTest() throws InterruptedException, ExecutionException {
        Thread.Builder builder = Thread.ofVirtual();
        Thread thread = builder.start(() -> {
            sleep(1);
            System.out.println("run!");
        });
        assertEquals(Thread.State.RUNNABLE, thread.getState());
        thread.join();
    }

    @Test
    public void createUnstartedThreadTest() throws InterruptedException, ExecutionException {
        Thread.Builder builder = Thread.ofVirtual();
        Thread thread = builder.unstarted(() -> {
            sleep(1);
            System.out.println("run!");
        });
        assertEquals(Thread.State.NEW, thread.getState());
        thread.start();
        thread.join();
    }

    private void sleep(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
