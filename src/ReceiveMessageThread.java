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
    private byte[] messageTypeBytes = new byte[4];
    private byte[] subMessageTypeBytes = new byte[4];
    private byte[] dataSizeBytes = new byte[4];

    public ReceiveMessageThread(Socket clientSocket){
        this.clientSocket = clientSocket;
    }

    @Override
    public void run(){
        while(true){
            try {
                //Get the message
                Message message = receiveMessage();

                //Check if it's a message that the main thread would need
                //Main thread needs all messages except the querying of the messages
                if(message.getMessageType() == MessageType.QUERY_MESSAGES){
                    //Check the query results
                    if(message.getSubMessageType() == 1){
                        //Print out the message contents
                        String[] messageInfo = message.getData().split(",", 3);
                        System.out.println(messageInfo[1] + " " + messageInfo[0] + ": " + messageInfo[2]);
                    }
                    else if(message.getSubMessageType() == 2){
                        System.out.println(message.getData());
                    }
                }
                else if(message.getMessageType() == MessageType.LOGOFF && message.getSubMessageType() == 2){
                    System.out.println(message.getData());
                    Client.logout();
                }
                else{
                    receivedMessage = message;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Message getReceivedMessage(){
        while(receivedMessage == null){
            try{Thread.sleep(200);} catch (InterruptedException e) {}
        }

        Message message = receivedMessage;

        //Set receivedMessage to null
        receivedMessage = null;

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
        in.read(messageTypeBytes);
        int messageType = ByteBuffer.wrap(messageTypeBytes).getInt();

        in.read(subMessageTypeBytes);
        int subMessageType = ByteBuffer.wrap(subMessageTypeBytes).getInt();

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