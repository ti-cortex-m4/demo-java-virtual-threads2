package virtual_threads;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class _SynchronousVsAsynchronousExecution {

    protected static final Logger logger = LoggerFactory.getLogger(_SynchronousVsAsynchronousExecution.class);

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

    @Test
    public void blockingSynchronousStyleTest() throws ExecutionException, InterruptedException {
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            long start = new Date().getTime();

            Future<Float> future = executorService.submit(() -> {
                logger.info("task started");

                int netAmountInUsd = getPriceInEur() * getExchangeRateEurToUsd(); // blocking
                float tax = getTax(netAmountInUsd); // blocking
                float grossAmountInUsd = netAmountInUsd * (1 + tax);

                return grossAmountInUsd;
            });

            float grossAmountInUsd = future.get();
            assertEquals(300, grossAmountInUsd);

            logger.info("task finished in {}", new Date().getTime() - start);
        }
    }

    @Test
    public void blockingAsynchronousStyleTest() throws InterruptedException, ExecutionException {
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            long start = new Date().getTime();

            Future<Integer> priceInEur = executorService.submit(this::getPriceInEur);
            Future<Integer> exchangeRateEurToUsd = executorService.submit(this::getExchangeRateEurToUsd);
            int netAmountInUsd = priceInEur.get() * exchangeRateEurToUsd.get();

            Future<Float> tax = executorService.submit(() -> getTax(netAmountInUsd));
            float grossAmountInUsd = netAmountInUsd * (1 + tax.get());

            assertEquals(300, grossAmountInUsd);
            logger.info("task finished in {}", new Date().getTime() - start);
        }
    }

    @Test
    public void nonBlockingAsynchronousStyleTest() throws InterruptedException, ExecutionException {
        long start = new Date().getTime();
        logger.info("task started");

        CompletableFuture.supplyAsync(this::getPriceInEur)
            .thenCombine(CompletableFuture.supplyAsync(this::getExchangeRateEurToUsd), (price, exchangeRate) -> price * exchangeRate)
            .thenCompose(amount -> CompletableFuture.supplyAsync(() -> amount * (1 + getTax(amount))))
            .whenComplete((grossAmountInUsd, throwable) -> {
                if (throwable == null) {
                    logger.info("task finished in {}", new Date().getTime() - start);
                    assertEquals(300, grossAmountInUsd);
                } else {
                    fail(throwable);
                }
            })
            .get();

        logger.info("task finished");
    }

    @Test
    public void testSynchronousVirtualThreads() throws InterruptedException {
        logger.info("this task started");
        Date start = new Date();

        var thread = Thread.ofVirtual().start(() ->
            {
                int netAmountInUsd = getPriceInEur() * getExchangeRateEurToUsd(); // blocking
                float tax = getTax(netAmountInUsd); // blocking
                float grossAmountInUsd = netAmountInUsd * (1 + tax);

                logger.info("this task finished: {} in {}", grossAmountInUsd, (new Date().getTime() - start.getTime()));
            }
        );
        thread.join();

        logger.info("this task finished: in {}", (new Date().getTime() - start.getTime()));
    }

    private int getPriceInEur() {
        return sleepAndGet(2, 100);
    }

    private int getExchangeRateEurToUsd() {
        return sleepAndGet(3, 2);
    }

    private float getTax(int amount) {
        return sleepAndGet(5, 50) / 100f;
    }

//    protected static <T> T sleepAndGet(T message) {
//        return sleepAndGet(1, message);
//    }
}
