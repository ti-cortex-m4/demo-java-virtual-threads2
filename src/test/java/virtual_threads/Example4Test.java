package virtual_threads;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Example4Test {

    @Test
    public void doTest() throws InterruptedException, ExecutionException {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<String> future1 = executor.submit(() -> "alpha");
            Future<String> future2 = executor.submit(() -> "omega");

            System.out.println(future1.get() + future2.get());
        }
    }

    @Test
    public void doNotTest() throws InterruptedException, ExecutionException {
        try (var executor = Executors.newCachedThreadPool(Thread.ofVirtual().factory())) {
            Future<String> future1 = executor.submit(() -> "alpha");
            Future<String> future2 = executor.submit(() -> "omega");

            System.out.println(future1.get() + future2.get());
        }
    }
}
