package virtual_threads;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Example1 {

    @Test
    public void test1() throws InterruptedException {
        Thread platformThread = new Thread(() -> System.out.println("run"));
        platformThread.start();
        platformThread.join();
        assertFalse(platformThread.isVirtual());
    }

    @Test
    public void test2() throws InterruptedException {
        Thread virtualThread = Thread.startVirtualThread(() -> System.out.println("run"));
        virtualThread.join();
        assertTrue(virtualThread.isVirtual());
    }
}