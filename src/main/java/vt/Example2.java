package vt;

import java.util.concurrent.ExecutionException;

public class Example2 {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        Thread.Builder builder = Thread.ofVirtual()
            .name("MyThread");
        Thread thread = builder.start(() -> System.out.println("Hello"));
        //assertEquals
        thread.join();
    }
}
