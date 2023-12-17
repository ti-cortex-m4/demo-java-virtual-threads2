package virtual_threads;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

public class Rule3UseSemaphoresInsteadOfFixedThreadPoolsToLimitConcurrencyTest {

    ExecutorService executorService = Executors.newFixedThreadPool(10);

    String foo1() throws ExecutionException, InterruptedException {
       Future<String> future = executorService.submit(() -> limitedService());
       return future.get();
    }


    Semaphore semaphore = new Semaphore(10);

    String foo2() throws InterruptedException {
        semaphore.acquire();
        try {
            return limitedService();
        } finally {
            semaphore.release();
        }
    }


    private String limitedService() {
        return "";
    }
}
