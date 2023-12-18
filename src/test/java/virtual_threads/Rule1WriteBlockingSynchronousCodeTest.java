package virtual_threads;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Rule1WriteBlockingSynchronousCodeTest {

    protected static final Logger logger = LoggerFactory.getLogger(Rule1WriteBlockingSynchronousCodeTest.class);
    
    @Test
    public void doTest() throws InterruptedException, ExecutionException {
        try {
            long start = new Date().getTime();

            Info info = new Info();
            String page = getBody1(info.getUrl(), HttpResponse.BodyHandlers.ofString());
            String imageUrl = info.findImage(page);
            String data = getBody(imageUrl, HttpResponse.BodyHandlers.ofByteArray());
            info.setImageData(data);
            process(info);

            logger.info("finished in {}", (new Date().getTime()-start));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void doNotTest() throws InterruptedException, ExecutionException {
        Info info = new Info();
        ExecutorService executor = Executors.newCachedThreadPool();

        long start = new Date().getTime();

        CompletableFuture.supplyAsync(info::getUrl, executor)
            .thenCompose(url -> getBodyAsync1(url, HttpResponse.BodyHandlers.ofString()))
            .thenApply(info::findImage)
            .thenCompose(url -> getBodyAsync(url, HttpResponse.BodyHandlers.ofByteArray()))
            .thenApply(info::setImageData)
            .thenAccept(this::process)
            .exceptionally(t -> {
                t.printStackTrace();
                return null;
            })
            .join();

        logger.info("finished in {}", (new Date().getTime()-start));
    }

    private String getBody1(String url, HttpResponse.BodyHandler<String> response) {
        logger.info("receive1 " + url);
        delay(2);
        return "step1";
    }

    private String getBody(String url, HttpResponse.BodyHandler<byte[]> response) {
        logger.info("receive3 " + url);
        delay(3);
        return "step3";
    }

    private CompletableFuture<String> getBodyAsync1(String url, HttpResponse.BodyHandler<String> response) {
        logger.info("receive1 " + url);
        delay(2);
        return CompletableFuture.supplyAsync(() -> "step1");
    }

    private CompletableFuture<String> getBodyAsync(String url, HttpResponse.BodyHandler<byte[]> response) {
        logger.info("receive3 " + url);
        delay(3);
        return CompletableFuture.supplyAsync(() -> "step3");
    }

    private void process(Info info) {
    }

    private void delay() {
        try {
           Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void delay(int i) {
        try {
            Thread.sleep(i*1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    class Info {

        private String url;

        public Info() {
            this.url ="step0";
        }

        public String getUrl() {
            return url;
        }


        public String findImage(String page) {
            logger.info("receive2 " + page);
            return "step2";
        }

        public Info setImageData(String data) {
            logger.info("receive4 " + data);
            return this;
        }

    }
}
