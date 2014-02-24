import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

/**
 * Author: Julien
 * Date: 17/02/14 4:08 PM
 */
public class Client {
    public static final String SERVER_URL = "dsp2014.ece.mcgill.ca";
    public static final int SERVER_PORT = 5000;

    static BufferedReader reader;
    static Socket socket;

    static InetAddress destinationAddress, hostInternalAddress;

    public static void main(String[] args) throws IOException {
        //Set up the internal info
        hostInternalAddress = InetAddress.getLocalHost();

        //Set up the socket
        socket = new Socket();

        //Set up the CLI reader
        reader = new BufferedReader(new InputStreamReader(System.in,"UTF-8"));

        //Get the user's name
//        System.out.print("Please enter your name: ");
//        name = reader.readLine().trim();

        //Start the call thread
        destinationAddress = InetAddress.getByName(SERVER_URL);
        System.out.println("Server Address: " + destinationAddress.getHostAddress());
        Socket destinationSocket = new Socket(destinationAddress.getHostAddress(), SERVER_PORT);

        String message = "Hey there buddy";
        byte[] messageBytes = message.getBytes();
        int messageLength = messageBytes.length;
        byte[] messageType = ByteBuffer.allocate(4).putInt(2).array();
        byte[] subMessageType = ByteBuffer.allocate(4).putInt(10).array();
        byte[] size = ByteBuffer.allocate(4).putInt(messageLength).array();
        byte[] payloadBytes = new byte[12 + messageLength];
        System.arraycopy(messageType, 0, payloadBytes, 0, 4);
        System.arraycopy(subMessageType, 0, payloadBytes, 4, 4);
        System.arraycopy(size, 0, payloadBytes, 8, 4);
        System.arraycopy(messageBytes, 0, payloadBytes, 12, messageLength);
        DatagramPacket packet = new DatagramPacket(payloadBytes, payloadBytes.length, destinationAddress, SERVER_PORT);

        socket.connect(destinationSocket.getRemoteSocketAddress());
        BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
        out.write(payloadBytes);
        out.flush();

        BufferedInputStream in = new BufferedInputStream(socket.getInputStream());

        byte[] packetBuffer = new byte[payloadBytes.length];
        in.read(packetBuffer);

        System.out.println("Packet Data: " + new String(packetBuffer));
    }
}