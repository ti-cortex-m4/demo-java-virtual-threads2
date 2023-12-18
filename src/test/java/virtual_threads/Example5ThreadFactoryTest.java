package virtual_threads;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Example5ThreadFactoryTest {

    @Test
    public void virtualThreadFactoryTest() throws InterruptedException, ExecutionException {
        Thread.Builder builder = Thread.ofVirtual()
            .name("virtual thread");
        ThreadFactory threadFactory = builder.factory();
        Thread thread = threadFactory.newThread(() -> System.out.println("run"));

        assertEquals("virtual thread", thread.getName());
        assertEquals(Thread.State.NEW, thread.getState());
    }
}
