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
    private static ReceiveThread receiveThread;

    private static boolean hasAccount = false;
    private static boolean loggedOn = false;

    public static void main(String[] args) throws IOException {
        //Set up the server socket
        InetAddress serverAddress = InetAddress.getByName(SERVER_URL);
        Socket serverSocket = new Socket(serverAddress.getHostAddress(), SERVER_PORT);

        //Set up the client socket
        clientSocket = new Socket();
        clientSocket.connect(serverSocket.getRemoteSocketAddress());

        //Set up the CLI reader
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in,"UTF-8"));

        //Set up the receiving thread
        receiveThread = new ReceiveThread();
        receiveThread.start();



        while(!loggedOn) {
            //Ask the user whether they want to sign in or sign up
            System.out.println("Sign In (I)   --   Sign Up (U) :");
            String signInOrUp = reader.readLine();


            //Verify the user entered either an I or a U
            if (signInOrUp.equalsIgnoreCase("i")) {
                 //user thinks they already have an account
                String username = "";
                boolean usernameValid = false;
                while (!usernameValid) {
                    System.out.println("Please enter your username:");
                    username = reader.readLine();

                    //check whether the entered username is valid
                    if (username.isEmpty() || username.contains(",") ) {
                         System.out.println("Please enter a valid username.");
                         continue;
                    }else{
                        usernameValid = true;
                    }
                }
                String password = "";
                boolean passwordValid = false;
                while(!passwordValid) {
                    System.out.println("Please enter your password:");
                    password = reader.readLine();

                    //check whether the password is valid
                    if (password.isEmpty()) {
                        System.out.println("Password must have a valid password.");
                        continue;
                    }else{
                        passwordValid = true;
                    }

                }
                System.out.println("Logging in...");
                //create login message
                Message loginMessage = new Message(MessageType.LOGIN,0, username + "," + password);

                sendMessage(loginMessage);
                Message loginResponse =  receiveThread.getReceivedMessage();
                System.out.println(loginResponse.getData());
                if (loginResponse.getSubMessageType() == 0 || loginResponse.getSubMessageType() == 1) {
                    //user is logged in
                    loggedOn = true;
                    System.out.println("User is now logged on.");
                }else if(loginResponse.getSubMessageType() == 2) {
                    //user entered incorrect credentials
                    System.out.println("The credentials you entered were incorrect. Please try again.");

                }

            }else if(signInOrUp.equalsIgnoreCase("u")) {
                //user wants to create account

                String username = "";
                boolean usernameValid = false;
                while (!usernameValid) {
                    System.out.println("Please enter your desired username:");
                    username = reader.readLine();

                    //check whether the entered username is valid
                    if (username.isEmpty() || username.contains(",") ) {
                        System.out.println("Username must be at least 1 letter long and contain no commas.");
                        continue;
                    }else{
                        usernameValid = true;
                    }
                }
                String password1 = "";
                String password2 = "";

                boolean passwordsValid = false;
                while(!passwordsValid) {
                    System.out.println("Please enter your password:");
                    password1 = reader.readLine();

                    //check whether the password is valid
                    if (password1.isEmpty()) {
                        System.out.println("Password must have at least 1 character.");
                        continue;
                    }else{
                        //ask user to verify their password
                        System.out.println("Please re-enter your password:");
                        password2 = reader.readLine();

                        if (password2.equals(password1)) {
                            passwordsValid = true;
                        }else{
                            System.out.println("Your passwords did not match. Please try again.");
                        }
                    }
                }


                Message createUserMessage = new Message(MessageType.CREATE_USER,0,username + "," + password1);

                sendMessage(createUserMessage);

                Message sendMessageResponse = receiveThread.receiveMessage();

                System.out.println(sendMessageResponse.getData());


            } else {
                //alert user their input was invalid and restart sign in process
                System.out.println("Error: Improperly formatted response. Please try again.");
                continue;
            }
        }



        //Main Loop
        while(true){

        }
    }

    public static void sendMessage(Message message) throws IOException{
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

    public static class ReceiveThread extends Thread{
        private Message receivedMessage;
        private boolean run;

        public ReceiveThread(){
            run = true;
        }

        @Override
        public void run(){
            try {
                while(run){
                    //Get the message
                    receivedMessage = receiveMessage();

                    //Check if it's a message that the main thread would need
                    //Main thread needs all message except the inactivity logout and the querying of the messages
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
}