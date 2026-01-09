import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class MainFinder {

    private static final AtomicInteger processed = new AtomicInteger(0);
    public static final ConcurrentLinkedQueue<String> foundServers = new ConcurrentLinkedQueue<>();
    private static Random random = new Random();

    public static void main(String[] args) throws InterruptedException {

        long startTime = System.nanoTime();
        DataBaseClass.init();
        int threads = 2000;
        int port = 25565;
        int timeout = 400;
        int totalIps = 1000000;

        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < totalIps; i++) {
            String ip = generateIp();

            if (isBadIp(ip)) {
                processed.incrementAndGet();
                continue;
            }

            pool.submit(() -> {

                if (MCServerDetector.isMinecraftServer(ip, port, timeout)) {
                    foundServers.add(ip);
                }


                int done = processed.incrementAndGet();

                if (done % 500 == 0 || done == totalIps) {
                    double percent = (done * 100.0) / totalIps;
                    System.out.printf(
                            "Progress: %.2f%% (%d/%d)%n",
                            percent, done, totalIps
                    );
                }


            });
        }

        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.MINUTES);
        long endTime = System.nanoTime();
        double seconds = (endTime - startTime) / 1_000_000_000.0;
        System.out.println("=================================");
        System.out.printf("Finalizado en %.2f segundos%n", seconds);
        System.out.println("=================================");
        System.out.println("Total IPs: " + foundServers.size());
    }

    /*
    public static String generateIp() {
        return (int) (Math.random() * 256) + "."
                + (int) (Math.random() * 256) + "."
                + (int) (Math.random() * 256) + "."
                + (int) (Math.random() * 256);
    }
    * */

    public static String generateIp() {
        return (random.nextInt(256)) + "."
                + (random.nextInt(256)) + "."
                + (random.nextInt(256)) + "."
                + (random.nextInt(256));
    }

    public static boolean isPortOpen(String ip, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), timeoutMs);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean isBadIp(String ip) {
        return ip.startsWith("0.") ||
                ip.startsWith("127.") ||
                ip.startsWith("10.") ||
                ip.startsWith("192.168.") ||
                ip.matches("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*");
    }
}


