package virtual_threads.part1;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Example3ThreadBuildersTest {

    @Test
    public void platformThreadBuilderTest() {
        Thread.Builder builder = Thread.ofPlatform()
            .group(Thread.currentThread().getThreadGroup())
            .daemon(false)
            .priority(10)
            .stackSize(1024)
            .name("a platform thread")
            .inheritInheritableThreadLocals(false)
            .uncaughtExceptionHandler((t, e) -> System.out.printf("Thread %s failed with exception %s", t, e));
        Thread thread = builder.unstarted(() -> System.out.println("run platform"));

        assertEquals("a platform thread", thread.getName());
        assertEquals("main", thread.getThreadGroup().getName());
        assertFalse(thread.isDaemon());
        assertEquals(10, thread.getPriority());
    }

    @Test
    public void virtualThreadBuilderTest() {
        Thread.Builder builder = Thread.ofVirtual()
            .name("a virtual thread")
            .inheritInheritableThreadLocals(false)
            .uncaughtExceptionHandler((t, e) -> System.out.printf("Thread %s failed with exception %s", t, e));
        Thread thread = builder.unstarted(() -> System.out.println("run virtual thread"));

        assertEquals("a virtual thread", thread.getName());
        assertEquals("VirtualThreads", thread.getThreadGroup().getName());
        assertTrue(thread.isDaemon());
        assertEquals(5, thread.getPriority());
    }

    @Test
    public void virtualThreadBuilderTest2() {
        Thread thread = Thread.startVirtualThread(() -> System.out.println("run"));
    }
}