import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class VirtualThreads {

    public static void main(String[] args) {
        var deadline = Instant.now().plusMillis(1000);
        try (var executor = Executors.newVirtualThreadExecutor().withDeadline(deadline)) {
            final AtomicInteger count = new AtomicInteger(0);
            for (int i = 0; i < 100; i++) {
                executor.submit(() -> {
                    var taskId = count.getAndIncrement();
                    try {
                        Thread.sleep(100 * taskId);
                    } catch (InterruptedException e) {
                        System.err.printf("Sleep got interrupted %d.%n", taskId);
                        return;
                    }
                    System.out.printf("Hello %d!%n", taskId);
                });
            }
        }
    }
}
