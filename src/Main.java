import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;

public class Main {

    private final static int MAX_FIBERS = 64;

    private final ExecutorService executor = Executors.newVirtualThreadExecutor();

    public static void main(String[] args) {
        new Main().run();
    }

    private void run() {
        final ServerSocketChannel serverChannel;
        try {
            serverChannel = ServerSocketChannel.open();
            ServerSocket serverSocket = serverChannel.socket();
            serverSocket.bind(new InetSocketAddress(7777), 1024);
        } catch (IOException e) {
            throw new RuntimeException("failed to start server", e);
        }
        var serverFiber = executor.submitTask(() -> startServer(serverChannel));

        IntStream.range(0, MAX_FIBERS)
                .mapToObj(i -> executor.submitTask(this::startClient))
                .collect(toList())
                .forEach(CompletableFuture::join);

        serverFiber.cancel(true);
        try {
            serverChannel.close();
            System.out.printf("closed server channel%n");
        } catch (IOException e) {
            throw new RuntimeException("failed to close server socket", e);
        }
        executor.shutdown();
    }

    private Void startServer(ServerSocketChannel serverChannel) {
        try {
            while (true) {
                var clientChannel = serverChannel.accept();
                System.out.printf("accepted client %s%n", clientChannel.getRemoteAddress());
                executor.submitTask(() -> talkToClient(clientChannel));
            }
        } catch (IOException e) {
            throw new RuntimeException("failed to accept from server socket", e);
        }
    }

    private Void talkToClient(SocketChannel clientChannel) {
        try {
            var remoteAddress = clientChannel.getRemoteAddress();
            System.out.printf("will talk to client %s%n", remoteAddress);
            var byteBuffer = ByteBuffer.allocate(1024);
            while (clientChannel.read(byteBuffer) > 0) {
                System.out.printf("got %d bytes from client %s%n", byteBuffer.position(), remoteAddress);
                byteBuffer.flip();
                clientChannel.write(byteBuffer);
                System.out.printf("sent to client %s%n", remoteAddress);
                byteBuffer.clear();
            }
            clientChannel.close();
            System.out.printf("closed client %s%n", remoteAddress);
            return null;
        } catch (IOException e) {
            throw new RuntimeException("failed to close client channel", e);
        }
    }

    private Void startClient() {
        var socketAddress = new InetSocketAddress("localhost", 7777);
        try {
            var channel = SocketChannel.open(socketAddress);
            var buffer = ByteBuffer.wrap("Hello World!".getBytes(UTF_8));
            channel.write(buffer);
            var localAddress = channel.getLocalAddress();
            System.out.printf("sent data to server from local address %s%n", localAddress);
            buffer.clear();
            channel.read(buffer);
            System.out.printf("read %d bytes from server from local address %s%n", buffer.position(), localAddress);
            channel.close();
            System.out.printf("closed server from local address %s%n", localAddress);
            return null;
        } catch (IOException e) {
            throw new RuntimeException("failed to connect", e);
        }
    }
}
