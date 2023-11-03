package virtual_threads;

import java.util.concurrent.*;

public class ExecutorsNewVirtualThreadPerTaskExecutor {

    public static void main(String[] args) throws InterruptedException {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        executor.submit(() -> {
            System.out.println("runnable");
        });

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }
}
