package virtual_threads.part1;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Example4VirtualThreadFactoryTest {

    @Test
    public void virtualThreadFactory() {
        Thread.Builder builder = Thread.ofVirtual()
            .name("virtual thread");

        ThreadFactory factory = builder.factory();
        System.out.println(factory.getClass().getName()); // java.lang.ThreadBuilders$VirtualThreadFactory
        Thread thread = factory.newThread(() -> System.out.println("run"));

        assertEquals("virtual thread", thread.getName());
        assertEquals(Thread.State.NEW, thread.getState());
    }
}
