package x4;

import java.util.ArrayList;
import java.util.List;

public class Start100000VirtualThreads {

    public static void main(String[] args) throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        int threadCount = 100_000;

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            Thread vThread = Thread.ofVirtual().start(() -> {
                System.out.println("thread: " + idx);
            });

            threads.add(vThread);
        }

        for (int i = 0; i < threads.size(); i++) {
            threads.get(i).join();
        }
    }
}
