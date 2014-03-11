import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * Author: Julien
 * Date: 17/02/14 4:08 PM
 */
public class Client {
    public static final String SERVER_URL = "dsp2014.ece.mcgill.ca";
    public static final int SERVER_PORT = 5000;

    static BufferedReader reader;
    static Socket clientSocket;

    public static void main(String[] args) throws IOException {
        //Set up the server socket
        InetAddress serverAddress = InetAddress.getByName(SERVER_URL);
        Socket serverSocket = new Socket(serverAddress.getHostAddress(), SERVER_PORT);

        //Set up the client socket
        clientSocket = new Socket();
        clientSocket.connect(serverSocket.getRemoteSocketAddress());

        //Set up the CLI reader
        reader = new BufferedReader(new InputStreamReader(System.in,"UTF-8"));

        //Get the user's name
//        System.out.print("Please enter your name: ");
//        name = reader.readLine().trim();
        System.out.println("Server Address: " + serverAddress.getHostAddress());
    }

    public void sendMessage(Message message) throws IOException{
        //Convert the data in bytes
        byte[] dataBytes = message.getData().getBytes();

        //Get the length of the dataBytes
        int dataSize = dataBytes.length;

        //Get all of the necessary ints in bytes
        byte[] messageTypeBytes = ByteBuffer.allocate(4).putInt(message.getMessageType()).array();
        byte[] subMessageTypeBytes = ByteBuffer.allocate(4).putInt(message.getSubMessageType()).array();
        byte[] dataSizeBytes = ByteBuffer.allocate(4).putInt(dataSize).array();

        //Put it all into one array of bytes
        byte[] messageBytes = new byte[12 + dataSize];
        System.arraycopy(messageTypeBytes, 0, messageBytes, 0, 4);
        System.arraycopy(subMessageTypeBytes, 0, messageBytes, 4, 4);
        System.arraycopy(dataSizeBytes, 0, messageBytes, 8, 4);
        System.arraycopy(dataBytes, 0, messageBytes, 12, dataSize);

        BufferedOutputStream out = new BufferedOutputStream(clientSocket.getOutputStream());
        out.write(messageBytes);
        out.flush();
//
//        BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
//
//        byte[] packetBuffer = new byte[messageBytes.length];
//        in.read(packetBuffer);
//
//        System.out.println("Packet Data: " + new String(packetBuffer));
    }
}