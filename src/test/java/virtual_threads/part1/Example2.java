package virtual_threads.part1;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Example2 {

    @Test
    public void test1() throws InterruptedException {
        Thread platformThread = Thread.ofPlatform().start(() -> System.out.println("run platform thread"));
        platformThread.join();
        assertFalse(platformThread.isVirtual());
    }

    @Test
    public void test2() throws InterruptedException {
        Thread virtualThread = Thread.ofVirtual().start(() -> System.out.println("run virtual thread"));
        virtualThread.join();
        assertTrue(virtualThread.isVirtual());
    }
}