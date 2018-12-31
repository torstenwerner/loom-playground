import java.util.stream.IntStream;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.util.stream.Collectors.toList;

public class Main {

    private final int MAX_FIBERS = 64;

    public static void main(String[] args) {
        new Main().run();
    }

    private void run() {
        IntStream.range(0, MAX_FIBERS)
                .mapToObj(this::startFiber)
                .collect(toList())
                .forEach(Fiber::join);
    }

    private Fiber startFiber(int id) {
        final Fiber<Void> fiber = Fiber.schedule(() -> {
            System.out.printf("fiber %d before sleep, time %d%n", id, currentTimeMillis());
            sleep((MAX_FIBERS - id) * 100);
            System.out.printf("fiber %d after sleep, time %d%n", id, currentTimeMillis());
        });
        System.out.printf("fiber %d scheduled with name %s%n", id, fiber);
        return fiber;
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            currentThread().interrupt();
        }
    }
}
