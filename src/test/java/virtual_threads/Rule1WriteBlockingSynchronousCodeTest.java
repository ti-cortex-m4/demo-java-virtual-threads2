package virtual_threads;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Rule1WriteBlockingSynchronousCodeTest {

    @Test
    public void doTest() throws InterruptedException, ExecutionException {
        try {
            String page = getBody(info.getUrl(), HttpResponse.BodyHandlers.ofString());
            String imageUrl = info.findImage(page);
            byte[] data = getBody(imageUrl, HttpResponse.BodyHandlers.ofByteArray());
            info.setImageData(data);
            process(info);
        } catch (Exception ex) {
            t.printStackTrace();
        }

    }

    @Test
    public void doNotTest() throws InterruptedException, ExecutionException {
        CompletableFuture.supplyAsync(info::getUrl, pool)
            .thenCompose(url -> getBodyAsync(url, HttpResponse.BodyHandlers.ofString()))
            .thenApply(info::findImage)
            .thenCompose(url -> getBodyAsync(url, HttpResponse.BodyHandlers.ofByteArray()))
            .thenApply(info::setImageData)
            .thenAccept(this::process)
            .exceptionally(t -> { t.printStackTrace(); return null; });
    }
}
