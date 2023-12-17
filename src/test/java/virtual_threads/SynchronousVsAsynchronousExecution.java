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

public class SynchronousVsAsynchronousExecution {

    protected static final Logger logger = LoggerFactory.getLogger(SynchronousVsAsynchronousExecution.class);

    @Test
    public void testSynchronous() {
        logger.info("this task started");

        int netAmountInUsd = getPriceInEur() * getExchangeRateEurToUsd(); // blocking
        float tax = getTax(netAmountInUsd); // blocking
        float grossAmountInUsd = netAmountInUsd * (1 + tax);

        logger.info("this task finished: {}", grossAmountInUsd);

        logger.info("another task started");
    }

    @Test
    public void testAsynchronousWithFuture() throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newCachedThreadPool();

        logger.info("this task started");

        Future<Integer> priceInEur = executorService.submit(this::getPriceInEur);
        Future<Integer> exchangeRateEurToUsd = executorService.submit(this::getExchangeRateEurToUsd);

        while (!priceInEur.isDone() || !exchangeRateEurToUsd.isDone()) { // non-blocking
            Thread.sleep(100);
            logger.info("another task is running");
        }

        int netAmountInUsd = priceInEur.get() * exchangeRateEurToUsd.get(); // actually non-blocking
        Future<Float> tax = executorService.submit(() -> getTax(netAmountInUsd));

        while (!tax.isDone()) { // non-blocking
            Thread.sleep(100);
            logger.info("another task is running");
        }

        float grossAmountInUsd = netAmountInUsd * (1 + tax.get()); // actually non-blocking

        logger.info("this task finished: {}", grossAmountInUsd);

        executorService.shutdown();
        executorService.awaitTermination(60, TimeUnit.SECONDS);

        logger.info("another task is running");
    }

    @Test
    public void testAsynchronousWithCompletableFuture() throws InterruptedException {
        CompletableFuture<Integer> priceInEur = CompletableFuture.supplyAsync(this::getPriceInEur);
        CompletableFuture<Integer> exchangeRateEurToUsd = CompletableFuture.supplyAsync(this::getExchangeRateEurToUsd);

        CompletableFuture<Integer> netAmountInUsd = priceInEur
                .thenCombine(exchangeRateEurToUsd, (price, exchangeRate) -> price * exchangeRate);

        logger.info("this task started");

        netAmountInUsd
                .thenCompose(amount -> CompletableFuture.supplyAsync(() -> amount * (1 + getTax(amount))))
                .whenComplete((grossAmountInUsd, throwable) -> {
                    if (throwable == null) {
                        logger.info("this task finished: {}", grossAmountInUsd);
                    } else {
                        logger.warn("this task failed: {}", throwable.getMessage());
                    }
                }); // non-blocking

        logger.info("another task started");
        Thread.sleep(10000);
    }

    private int getPriceInEur() {
        return sleepAndGet(2);
    }

    private int getExchangeRateEurToUsd() {
        return sleepAndGet(4);
    }

    private float getTax(int amount) {
        return sleepAndGet(50) / 100f;
    }


    protected static void sleep(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected static <T> T sleepAndGet(int seconds, T message) {
        logger.info(message + " started");
        sleep(seconds);
        logger.info(message + " finished");
        return message;
    }

    protected static <T> T sleepAndGet(T message) {
        return sleepAndGet(1, message);
    }
}
