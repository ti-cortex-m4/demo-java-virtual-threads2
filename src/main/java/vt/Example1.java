package vt;

import java.util.concurrent.ExecutionException;

public class Example1 {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        Thread thread = Thread.ofVirtual().start(() -> System.out.println("Hello"));
        thread.join();
    }
}
