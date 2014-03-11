import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * Author: Julien
 * Date: 2014-03-11 11:00
 */
public class ReceiveMessageThread extends Thread{
    private Message receivedMessage;
    private Socket clientSocket;
    private boolean run = true;

    public ReceiveMessageThread(Socket clientSocket){
        this.clientSocket = clientSocket;
    }

    @Override
    public void run(){
        try {
            while(run){
                //Get the message
                receivedMessage = receiveMessage();

                //Check if it's a message that the main thread would need
                //Main thread needs all messages except the inactivity logout and the querying of the messages
                //TODO Add the inactivity logout here
                if(receivedMessage.getMessageType() == MessageType.QUERY_MESSAGES){
                    //Keep on running the code if ever it's just a query
                    run = true;
                }
                else{
                    run = false;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Message getReceivedMessage(){
        while(receivedMessage == null){
            try{Thread.sleep(200);} catch (InterruptedException e) {}
        }

        Message message = this.receivedMessage;

        //Set receivedMessage to null
        receivedMessage = null;

        //Continue running the code
        run = true;

        return message;
    }

    private Message receiveMessage() throws IOException{
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