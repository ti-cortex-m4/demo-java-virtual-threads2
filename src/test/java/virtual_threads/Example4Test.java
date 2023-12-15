package virtual_threads;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Example4Test {

    @Test
    public void doTest() throws InterruptedException, ExecutionException {
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            System.out.println("Executor service: " + executorService);
        }
    }

    @Test
    public void doNotTest() throws InterruptedException, ExecutionException {
        // don't do that
        try (ExecutorService executorService = Executors.newCachedThreadPool(Thread.ofVirtual().factory())) {
            System.out.println("Executor service: " + executorService);
        }
    }
}
