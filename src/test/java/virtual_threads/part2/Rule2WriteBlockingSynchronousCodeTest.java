package virtual_threads.part2;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import virtual_threads.AbstractTest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class Rule2WriteBlockingSynchronousCodeTest extends AbstractTest {

    @Test
    public void blockingSynchronousCodeTest() throws ExecutionException, InterruptedException {
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            long startMillis = System.currentTimeMillis();

            Future<Float> future = executorService.submit(() -> {
                int priceInEur = readPriceInEur();
                float netAmountInUsd = priceInEur * readExchangeRateEurToUsd();
                float tax = readTax(netAmountInUsd);
                return netAmountInUsd * (1 + tax);
            });

            float grossAmountInUsd = future.get();
            assertEquals(165, grossAmountInUsd);

            long durationMillis = System.currentTimeMillis() - startMillis;
            assertEquals(durationMillis, 10000, 100);
        }
    }

    @Test
    public void blockingAsynchronousCodeTest() throws InterruptedException, ExecutionException {
        try (var executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            long startMillis = System.currentTimeMillis();

            Future<Integer> priceInEur = executorService.submit(this::readPriceInEur);
            Future<Float> exchangeRateEurToUsd = executorService.submit(this::readExchangeRateEurToUsd);
            float netAmountInUsd = priceInEur.get() * exchangeRateEurToUsd.get();

            Future<Float> tax = executorService.submit(() -> readTax(netAmountInUsd));
            float grossAmountInUsd = netAmountInUsd * (1 + tax.get());
            assertEquals(165, grossAmountInUsd);

            long durationMillis = System.currentTimeMillis() - startMillis;
            assertEquals(durationMillis, 8000, 100);
        }
    }

    @Test
    public void nonBlockingAsynchronousCodeTest() throws InterruptedException, ExecutionException {
        long startMillis = System.currentTimeMillis();

        CompletableFuture.supplyAsync(this::readPriceInEur)
            .thenCombine(CompletableFuture.supplyAsync(this::readExchangeRateEurToUsd), (price, exchangeRate) -> price * exchangeRate)
            .thenCompose(amount -> CompletableFuture.supplyAsync(() -> amount * (1 + readTax(amount))))
            .whenComplete((grossAmountInUsd, t) -> {
                if (t == null) {
                    assertEquals(165, grossAmountInUsd);
                } else {
                    fail(t);
                }
            })
            .get();

        long durationMillis = System.currentTimeMillis() - startMillis;
        assertEquals(durationMillis, 8000, 100);
    }

    private int readPriceInEur() {
        return sleepAndGet(2000, 100);
    }

    private float readExchangeRateEurToUsd() {
        return sleepAndGet(3000, 1.1f);
    }

    private float readTax(float amount) {
        return sleepAndGet(5000, 0.5f);
    }

    private <T> T sleepAndGet(int millis, T value) {
        logger.info(value + " started");
        sleep(millis);
        logger.info(value + " finished");
        return value;
    }
}
