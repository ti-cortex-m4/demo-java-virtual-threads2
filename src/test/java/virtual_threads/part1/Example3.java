package virtual_threads.part1;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Example3 {

    @Test
    public void test2() throws InterruptedException {
        Thread thread = Thread.startVirtualThread(() -> { sleep(1000); System.out.println("run virtual thread"); });

        assertTrue(thread.isVirtual());
        assertEquals("", thread.getName());
        assertEquals("VirtualThreads", thread.getThreadGroup().getName());
        assertTrue(thread.isDaemon());
        assertEquals(5, thread.getPriority());

        thread.join();
    }

    private void sleep(int milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}