package course_management_swing_ui.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Overview A class contains the thread pool that reuses a fixed number of threads operating off a shared unbounded
 * queue
 */
public class ThreadPool {
    public static final ExecutorService executor = Executors.newFixedThreadPool(8);

    private ThreadPool() {
    }
}
