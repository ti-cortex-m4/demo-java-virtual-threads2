package virtual_threads.part1;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
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

    @Test
    public void test3() throws InterruptedException {
        Thread.Builder builder = Thread.ofVirtual();

        ThreadFactory threadFactory = builder.factory();
        System.out.println(threadFactory.getClass().getName()); // java.lang.ThreadBuilders$VirtualThreadFactory

        Thread thread = threadFactory.newThread(() -> System.out.println("run"));
        System.out.println(thread.getClass().getName()); // java.lang.VirtualThread
        thread.join();
    }

    @Test
    public void test4() throws InterruptedException, ExecutionException {
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            System.out.println(executorService.getClass().getName()); // java.util.concurrent.ThreadPerTaskExecutor

            Future<?> future = executorService.submit(() -> System.out.println("run"));
            future.get();
        }
    }
    private void sleep(int milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}