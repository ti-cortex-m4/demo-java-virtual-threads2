package virtual_threads;

import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Rule1WriteBlockingSynchronousCodeTest {

    @Test
    public void doTest() throws InterruptedException, ExecutionException {
        try {
            Info info = new Info();
            String page = getBody1(info.getUrl(), HttpResponse.BodyHandlers.ofString());
            String imageUrl = info.findImage(page);
            byte[] data = getBody(imageUrl, HttpResponse.BodyHandlers.ofByteArray());
            info.setImageData(data);
            process(info);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void doNotTest() throws InterruptedException, ExecutionException {
        Info info = new Info();
        Executor executor = Executors.newCachedThreadPool(Thread.ofVirtual().factory());
        CompletableFuture.supplyAsync(info::getUrl, executor)
            .thenCompose(url -> getBodyAsync1(url, HttpResponse.BodyHandlers.ofString()))
            .thenApply(info::findImage)
            .thenCompose(url -> getBodyAsync(url, HttpResponse.BodyHandlers.ofByteArray()))
            .thenApply(info::setImageData)
            .thenAccept(this::process)
            .exceptionally(t -> {
                t.printStackTrace();
                return null;
            });
    }

    private String getBody1(String url, HttpResponse.BodyHandler<String> response) {
        return " ";
    }

    private byte[] getBody(String url, HttpResponse.BodyHandler<byte[]> response) {
        return new byte[]{};
    }

    private CompletableFuture<String> getBodyAsync1(String url, HttpResponse.BodyHandler<String> response) {
        return CompletableFuture.supplyAsync(() -> " ");
    }

    private CompletableFuture<byte[]> getBodyAsync(String url, HttpResponse.BodyHandler<byte[]> response) {
        return CompletableFuture.supplyAsync(() -> new byte[]{});
    }

    private void process(Info info) {
    }
}

class Info {

    private String url;

    public String getUrl() {
        return url;
    }


    public String findImage(String page) {
        return " ";
    }

    public Info setImageData(byte[] data) {
        return this;
    }
}
