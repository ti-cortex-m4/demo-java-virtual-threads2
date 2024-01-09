package virtual_threads.part1;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Example1PlatformAndVirtualThreadsTest {

    @Test
    public void test1() throws InterruptedException {
        Thread platformThread = new Thread(() -> System.out.println("run platform thread"));
        platformThread.start();
        platformThread.join();
        assertFalse(platformThread.isVirtual());
        assertEquals("java.lang.Thread", platformThread.getClass().getName());
    }

    @Test
    public void test2() throws InterruptedException {
        Thread virtualThread = Thread.startVirtualThread(() -> System.out.println("run virtual thread"));
        virtualThread.join();
        assertTrue(virtualThread.isVirtual());
        assertEquals("java.lang.VirtualThread", virtualThread.getClass().getName());
    }
}