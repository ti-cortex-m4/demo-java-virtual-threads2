package virtual_threads;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Example3Test {

    @Test
    public void builderTest() throws InterruptedException, ExecutionException {
        Thread.Builder builder = Thread.ofVirtual()
            .name("MyThread")
            .inheritInheritableThreadLocals(false)
            .uncaughtExceptionHandler((t, e) -> System.out.printf("Thread %s failed with exception %s", t, e));
        Thread thread = builder.start(() -> System.out.println("run"));

        assertEquals("VirtualThreads", thread.getThreadGroup().getName());
        assertTrue(thread.isDaemon());
        assertEquals(5, thread.getPriority());

        thread.join();
    }

    @Test
    public void builder2Test() throws InterruptedException, ExecutionException {
        Thread.Builder builder = Thread.ofVirtual()
            .name("MyThread")
            .inheritInheritableThreadLocals(false)
            .uncaughtExceptionHandler((t, e) -> System.out.printf("Thread %s failed with exception %s", t, e));
        Thread thread = builder.unstarted(() -> System.out.println("run"));
        thread.start();

        assertEquals("VirtualThreads", thread.getThreadGroup().getName());
        assertTrue(thread.isDaemon());
        assertEquals(5, thread.getPriority());

        thread.join();
    }
}
