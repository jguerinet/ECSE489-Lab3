import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * Author: Julien
 * Date: 17/02/14 4:08 PM
 */
public class Client {
    private static final String SERVER_URL = "dsp2014.ece.mcgill.ca";
    private static final int SERVER_PORT = 5000;

    private static Socket clientSocket;

    private boolean hasAccount = false;
    private boolean loggedOn = false;

    public static void main(String[] args) throws IOException {
        //Set up the server socket
        InetAddress serverAddress = InetAddress.getByName(SERVER_URL);
        Socket serverSocket = new Socket(serverAddress.getHostAddress(), SERVER_PORT);

        //Set up the client socket
        clientSocket = new Socket();
        clientSocket.connect(serverSocket.getRemoteSocketAddress());

        //Set up the CLI reader
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in,"UTF-8"));

        //Main Loop
        while(true){

        }
    }

    public void sendMessage(Message message) throws IOException{
        //Convert the data in bytes
        byte[] dataBytes = message.getData().getBytes();

        //Get the length of the dataBytes
        int dataSize = dataBytes.length;

        //Get all of the necessary ints in bytes
        byte[] messageTypeBytes = ByteBuffer.allocate(4).putInt(message.getMessageType().getMessageTypeInt()).array();
        byte[] subMessageTypeBytes = ByteBuffer.allocate(4).putInt(message.getSubMessageType()).array();
        byte[] dataSizeBytes = ByteBuffer.allocate(4).putInt(dataSize).array();

        //Put it all into one array of bytes
        byte[] messageBytes = new byte[12 + dataSize];
        System.arraycopy(messageTypeBytes, 0, messageBytes, 0, 4);
        System.arraycopy(subMessageTypeBytes, 0, messageBytes, 4, 4);
        System.arraycopy(dataSizeBytes, 0, messageBytes, 8, 4);
        System.arraycopy(dataBytes, 0, messageBytes, 12, dataSize);

        //Send the message
        BufferedOutputStream out = new BufferedOutputStream(clientSocket.getOutputStream());
        out.write(messageBytes);
        out.flush();
    }

    public Message receiveMessage() throws IOException{
        InputStream in = new BufferedInputStream(clientSocket.getInputStream());
        int bytesAvailable = in.available();

        //Wait until we have at least 12 bytes available so we can find out how much data we have to read
        while(bytesAvailable < 12){
            bytesAvailable = in.available();
        }

        //Retrieve the first 12 bytes, convert them into numbers
        byte[] messageTypeBytes = new byte[4];
        in.read(messageTypeBytes);
        int messageType = ByteBuffer.wrap(messageTypeBytes).getInt();

        byte[] subMessageTypeBytes = new byte[4];
        in.read(subMessageTypeBytes);
        int subMessageType = ByteBuffer.wrap(subMessageTypeBytes).getInt();

        byte[] dataSizeBytes = new byte[4];
        in.read(dataSizeBytes);
        int dataSize = ByteBuffer.wrap(dataSizeBytes).getInt();

        //Retrieve the correct number of bytes for the data, convert it into a String
        byte[] dataBytes = new byte[dataSize];
        in.read(dataBytes);
        String data = new String(dataBytes);

        //Return a message with all of this data
        return new Message(MessageType.getMessageType(messageType), subMessageType, data);
    }
}