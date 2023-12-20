package virtual_threads;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class _SynchronousVsAsynchronousExecution {

    private static final Logger logger = LoggerFactory.getLogger(_SynchronousVsAsynchronousExecution.class);

    @Test
    public void blockingSynchronousStyleTest() throws ExecutionException, InterruptedException {
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            long startMillis = System.currentTimeMillis();

            Future<Float> future = executorService.submit(() -> { // non-blocking
                int netAmountInUsd = getPriceInEur() * getExchangeRateEurToUsd(); // blocking
                float tax = getTax(netAmountInUsd); // blocking
                return netAmountInUsd * (1 + tax);
            });

            float grossAmountInUsd = future.get(); // blocking
            assertEquals(300, grossAmountInUsd);

            logger.info("blocking synchronous code finished in {} millis", System.currentTimeMillis() - startMillis); // ~ 10000 millis
        }
    }

    @Test
    public void blockingAsynchronousStyleTest() throws InterruptedException, ExecutionException {
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            long startMillis = System.currentTimeMillis();

            Future<Integer> priceInEur = executorService.submit(this::getPriceInEur); // non-blocking
            Future<Integer> exchangeRateEurToUsd = executorService.submit(this::getExchangeRateEurToUsd); // non-blocking
            int netAmountInUsd = priceInEur.get() * exchangeRateEurToUsd.get(); // blocking

            Future<Float> tax = executorService.submit(() -> getTax(netAmountInUsd)); // non-blocking
            float grossAmountInUsd = netAmountInUsd * (1 + tax.get()); // blocking

            assertEquals(300, grossAmountInUsd);
            logger.info("blocking asynchronous code finished in {} millis", System.currentTimeMillis() - startMillis); // ~ 8000 millis
        }
    }

    @Test
    public void nonBlockingAsynchronousStyleTest() throws InterruptedException, ExecutionException {
        long startMillis = System.currentTimeMillis();

        CompletableFuture.supplyAsync(this::getPriceInEur) // non-blocking
            .thenCombine(CompletableFuture.supplyAsync(this::getExchangeRateEurToUsd), (price, exchangeRate) -> price * exchangeRate) // non-blocking
            .thenCompose(amount -> CompletableFuture.supplyAsync(() -> amount * (1 + getTax(amount)))) // non-blocking
            .whenComplete((grossAmountInUsd, throwable) -> { // non-blocking
                if (throwable == null) {
                    assertEquals(300, grossAmountInUsd);
                } else {
                    fail(throwable);
                }
            })
            .get(); // blocking

        logger.info("non-blocking asynchronous code finished in {} millis", System.currentTimeMillis() - startMillis); // ~ 8000 millis
    }

    private int getPriceInEur() {
        return sleepAndGet(2000, 100);
    }

    private int getExchangeRateEurToUsd() {
        return sleepAndGet(3000, 2);
    }

    private float getTax(int amount) {
        return sleepAndGet(5000, 50) / 100f;
    }

    private <T> T sleepAndGet(int millis, T value) {
        logger.info(value + " started");
        sleep(millis);
        logger.info(value + " finished");
        return value;
    }

    private void sleep(int millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
