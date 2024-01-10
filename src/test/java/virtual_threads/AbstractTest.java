package virtual_threads;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import virtual_threads.part2.Rule2WriteBlockingSynchronousCodeTest;

import java.util.concurrent.TimeUnit;

public class AbstractTest {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected void sleep(int milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected void sleep() {
        sleep(1000);
    }
}
