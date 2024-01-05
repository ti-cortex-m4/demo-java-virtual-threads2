package virtual_threads;

import java.util.concurrent.TimeUnit;

class AbstractTest {

    protected void sleep(int milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
