package virtual_threads;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

public class Rule3UseSemaphoresInsteadOfFixedThreadPoolsToLimitConcurrencyTest {

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public Object useFixedExecutorServiceToLimitConcurrency() throws ExecutionException, InterruptedException {
        Future<Object> future = executorService.submit(() -> sharedResource());
        return future.get();
    }

    private final Semaphore semaphore = new Semaphore(10);

    public Object useSemaphoreToLimitConcurrency() throws InterruptedException {
        semaphore.acquire();
        try {
            return sharedResource();
        } finally {
            semaphore.release();
        }
    }


    private String sharedResource() {
        return "";
    }
}
