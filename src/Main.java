import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;

public class Main {

    private final int MAX_FIBERS = 64;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void main(String[] args) {
        new Main().run();
    }

    private void run() {
        final ServerSocket serverSocket;
        try {
            serverSocket = ServerSocketChannel.open().socket();
            serverSocket.bind(new InetSocketAddress(7777), 1024);
        } catch (IOException e) {
            throw new RuntimeException("failed to start server", e);
        }
        final Fiber<Void> serverFiber = Fiber.schedule(executor, () -> startServer(serverSocket.getChannel()));

        IntStream.range(0, MAX_FIBERS)
                .mapToObj(i -> Fiber.schedule(executor, this::startClient))
                .collect(toList())
                .forEach(Fiber::join);

        serverFiber.cancel();
        try {
            serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("failed to close server socket", e);
        }
        executor.shutdown();
    }

    private void startServer(ServerSocketChannel serverChannel) {
        try {
            while (true) {
                final SocketChannel clientChannel = serverChannel.accept();
                System.out.printf("accepted client %s%n", clientChannel.getRemoteAddress());
                Fiber.schedule(executor, () -> talkToClient(clientChannel));
            }
        } catch (IOException e) {
            throw new RuntimeException("failed to accept from server socket", e);
        }
    }

    private void talkToClient(SocketChannel clientChannel) {
        try {
            final SocketAddress remoteAddress = clientChannel.getRemoteAddress();
            System.out.printf("will talk to client %s%n", remoteAddress);
            final ByteBuffer byteBuffer = ByteBuffer.allocate(64);
            while (clientChannel.read(byteBuffer) > 0) {
                System.out.printf("got %d bytes from client %s%n", byteBuffer.position(), remoteAddress);
                byteBuffer.flip();
                clientChannel.write(byteBuffer);
                System.out.printf("sent to client %s%n", remoteAddress);
            }
            clientChannel.close();
            System.out.printf("closed client %s%n", remoteAddress);
        } catch (IOException e) {
            throw new RuntimeException("failed to close client channel", e);
        }
    }

    private void startClient() {
        final InetSocketAddress socketAddress = new InetSocketAddress("localhost", 7777);
        try {
            final SocketChannel channel = SocketChannel.open(socketAddress);
            final ByteBuffer buffer = ByteBuffer.wrap("Hello World!".getBytes(UTF_8));
            channel.write(buffer);
            final SocketAddress localAddress = channel.getLocalAddress();
            System.out.printf("sent data to server from local address %s%n", localAddress);
            buffer.flip();
            while (channel.read(buffer) > 0) {
                System.out.printf("read %d bytes from server from local address %s%n", buffer.position(), localAddress);
            }
            channel.close();
            System.out.printf("closed server from local address %s%n", localAddress);
        } catch (IOException e) {
            throw new RuntimeException("failed to connect", e);
        }
    }
}
