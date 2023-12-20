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

public class Rule1WriteBlockingSynchronousCodeTest {

    private static final Logger logger = LoggerFactory.getLogger(Rule1WriteBlockingSynchronousCodeTest.class);

    @Test
    public void blockingSynchronousCodeTest() throws ExecutionException, InterruptedException {
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            long startMillis = System.currentTimeMillis();

            Future<Float> future = executorService.submit(() -> { // non-blocking
                int priceInEur = getPriceInEur(); // blocking
                float netAmountInUsd = priceInEur * getExchangeRateEurToUsd(); // blocking
                float tax = getTax(netAmountInUsd); // blocking
                return netAmountInUsd * (1 + tax);
            });

            float grossAmountInUsd = future.get(); // blocking, ~10000 millis
            assertEquals(165, grossAmountInUsd);

            long durationMillis = System.currentTimeMillis() - startMillis;
            logger.info("finished in {} millis", durationMillis);
            assertEquals(durationMillis, 10000, 100);
        }
    }

    @Test
    public void blockingAsynchronousCodeTest() throws InterruptedException, ExecutionException {
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            long startMillis = System.currentTimeMillis();

            Future<Integer> priceInEur = executorService.submit(this::getPriceInEur); // non-blocking
            Future<Float> exchangeRateEurToUsd = executorService.submit(this::getExchangeRateEurToUsd); // non-blocking
            float netAmountInUsd = priceInEur.get() * exchangeRateEurToUsd.get(); // blocking

            Future<Float> tax = executorService.submit(() -> getTax(netAmountInUsd)); // non-blocking
            float grossAmountInUsd = netAmountInUsd * (1 + tax.get()); // blocking, ~8000 millis

            assertEquals(165, grossAmountInUsd);

            long durationMillis = System.currentTimeMillis() - startMillis;
            logger.info("finished in {} millis", durationMillis);
            assertEquals(durationMillis, 8000, 100);
        }
    }

    @Test
    public void nonBlockingAsynchronousCodeTest() throws InterruptedException, ExecutionException {
        long startMillis = System.currentTimeMillis();

        CompletableFuture.supplyAsync(this::getPriceInEur) // non-blocking
            .thenCombine(CompletableFuture.supplyAsync(this::getExchangeRateEurToUsd), (price, exchangeRate) -> price * exchangeRate) // non-blocking
            .thenCompose(amount -> CompletableFuture.supplyAsync(() -> amount * (1 + getTax(amount)))) // non-blocking
            .whenComplete((grossAmountInUsd, throwable) -> { // non-blocking
                if (throwable == null) {
                    assertEquals(165, grossAmountInUsd);
                } else {
                    fail(throwable);
                }
            })
            .get(); // blocking, ~8000 millis

        long durationMillis = System.currentTimeMillis() - startMillis;
        logger.info("finished in {} millis", durationMillis);
        assertEquals(durationMillis, 8000, 100);
    }

    private int getPriceInEur() {
        return sleepAndGet(2000, 100);
    }

    private float getExchangeRateEurToUsd() {
        return sleepAndGet(3000, 1.1f);
    }

    private float getTax(float amount) {
        return sleepAndGet(5000, 0.5f);
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
