public class Main {

    public static void main(String[] args) {

        final Fiber<Void> fiber01 = Fiber.schedule(() -> {
            System.out.println("1st fiber before sleep");
            sleep(1000);
            System.out.println("1st fiber after sleep");
        });
        final Fiber<Void> fiber02 = Fiber.schedule(() -> {
            System.out.println("2nd fiber before sleep");
            sleep(500);
            System.out.println("2nd fiber after sleep");
        });
        fiber01.join();
        fiber02.join();
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
