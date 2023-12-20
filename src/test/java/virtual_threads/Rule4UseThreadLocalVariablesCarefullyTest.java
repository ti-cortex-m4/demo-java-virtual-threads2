package virtual_threads;

import org.junit.jupiter.api.Test;

public class Rule4UseThreadLocalVariablesCarefullyTest {

    @Test
    public void threadLocalVariablesTest() {
    }

    public final static ScopedValue<String> LOGGED_IN_USER = ScopedValue.newInstance();

    @Test
    public void scopedValuesTest() {
//        ScopedValue.where(LOGGED_IN_USER, user.get()).run(() -> service.getData());

//        User loggedInUser = Server.LOGGED_IN_USER.get();
    }
}
