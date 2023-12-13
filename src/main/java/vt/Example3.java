package vt;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Example3 {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            System.out.println("executor service: " + executorService);
            Future<?> future = executorService.submit(() -> System.out.println("Running thread"));
            future.get();
            System.out.println("Task completed");
        }
    }
}
