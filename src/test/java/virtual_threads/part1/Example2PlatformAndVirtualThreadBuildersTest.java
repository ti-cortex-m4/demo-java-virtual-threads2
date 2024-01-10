package virtual_threads.part1;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Example2PlatformAndVirtualThreadBuildersTest {

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
        System.out.println(builder.getClass().getName()); // java.lang.ThreadBuilders$PlatformThreadBuilder

        Thread thread = builder.unstarted(() -> System.out.println("run platform thread"));

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
        System.out.println(builder.getClass().getName()); // java.lang.ThreadBuilders$VirtualThreadBuilder

        Thread thread = builder.unstarted(() -> System.out.println("run virtual thread"));

        assertEquals("a virtual thread", thread.getName());
        assertEquals("VirtualThreads", thread.getThreadGroup().getName());
        assertTrue(thread.isDaemon());
        assertEquals(5, thread.getPriority());
    }
}
