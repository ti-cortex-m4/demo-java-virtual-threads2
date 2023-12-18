
package virtual_threads;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Example4CreateStartedAndUnstartedVirtualThreadsTest {

    @Test
    public void createStartedThreadTest() throws InterruptedException, ExecutionException {
        Thread.Builder builder = Thread.ofVirtual();
        Thread thread = builder.start(() -> System.out.println("run!"));
        thread.join();
    }

    @Test
    public void createUnstartedThreadTest() throws InterruptedException, ExecutionException {
        Thread.Builder builder = Thread.ofVirtual();
        Thread thread = builder.unstarted(() -> System.out.println("run!"));
        assertEquals(Thread.State.NEW, thread.getState());
        thread.start();
        thread.join();
    }
}
