package virtual_threads;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Example4Test {

    @Test
    public void doTest() throws InterruptedException, ExecutionException {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            System.out.println(executor);
            Future<String> future1 = executor.submit(() -> "alpha");
            System.out.println(future1.get());
        }
    }

    @Test
    public void doNotTest() throws InterruptedException, ExecutionException {
        try (var executor = Executors.newCachedThreadPool(Thread.ofVirtual().factory())) {
            System.out.println(executor);
            Future<String> future1 = executor.submit(() -> "alpha");
            System.out.println(future1.get());
        }
    }
}
