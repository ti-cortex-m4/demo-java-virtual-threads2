package virtual_threads.part1;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Example1PlatformAndVirtualThreadsTest {

    @Test
    public void createPlatformThreadByConstructor() throws InterruptedException {
        Thread thread = new Thread(() -> System.out.println("run platform thread"));
        thread.start();
        thread.join();

        assertFalse(thread.isVirtual());
        assertEquals("java.lang.Thread", thread.getClass().getName());
    }

    @Test
    public void createPlatformThreadByBuilder() throws InterruptedException {
        Thread thread = Thread.ofPlatform().start(() -> System.out.println("run platform thread"));
        thread.join();

        assertFalse(thread.isVirtual());
        assertEquals("java.lang.Thread", thread.getClass().getName());
    }

    @Test
    public void createVirtualThreadByStaticFactoryMethod() throws InterruptedException {
        Thread thread = Thread.startVirtualThread(() -> System.out.println("run virtual thread"));
        thread.join();

        assertTrue(thread.isVirtual());
        assertEquals("java.lang.VirtualThread", thread.getClass().getName());
        assertEquals("", thread.getName());
    }

    @Test
    public void createVirtualThreadByBuilder() throws InterruptedException {
        Thread thread = Thread.ofVirtual().start(() -> System.out.println("run virtual thread"));
        thread.join();

        assertTrue(thread.isVirtual());
        assertEquals("java.lang.VirtualThread", thread.getClass().getName());
    }
}