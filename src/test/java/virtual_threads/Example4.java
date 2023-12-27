package virtual_threads;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Example4 {

    @Test
    public void test1() throws InterruptedException {
        Thread.Builder builder = Thread.ofVirtual()
            .name("a virtual thread")
            .inheritInheritableThreadLocals(false)
            .uncaughtExceptionHandler((t, e) -> System.out.printf("Thread %s failed with exception %s", t, e));
        System.out.println(builder.getClass().getName()); // java.lang.ThreadBuilders$VirtualThreadBuilder

        Thread thread = builder.start(() -> System.out.println("run"));
        System.out.println(thread.getClass().getName()); // java.lang.VirtualThread

        thread.join();
    }

    @Test
    public void test2() throws InterruptedException {
        Thread thread = Thread.startVirtualThread(() -> System.out.println("run"));
        System.out.println(thread.getClass().getName()); // java.lang.VirtualThread

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