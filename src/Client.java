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
    private static final String SERVER_URL = "dsp2014.ece.mcgill.ca";
    private static final int SERVER_PORT = 5003;

    private static Socket clientSocket;
    private static ReceiveMessageThread receiveThread;

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
        receiveThread = new ReceiveMessageThread(clientSocket);
        receiveThread.start();

        //Set up the querying thread
        new QueryThread().start();

        while(true){
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
                    Message loginMessage = new Message(MessageType.LOGIN, username + "," + password);

                    sendMessage(loginMessage);
                    Message loginResponse =  receiveThread.getReceivedMessage();
                    System.out.println(loginResponse.getData());
                    if (loginResponse.getSubMessageType() == 0  ) {
                        //user is logged in
                        loggedOn = true;
                        //System.out.println("User is now logged in.");
                    }else if(loginResponse.getSubMessageType() == 1){
                         //user is already logged in
                        //System.out.println("Someone is already logged in to this account. Please try a different one.");

                    }else if(loginResponse.getSubMessageType() == 2) {
                        //user entered incorrect credentials
                        //System.out.println("The credentials you entered were incorrect. Please try again.");

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

                    //send create user message
                    Message createUserMessage = new Message(MessageType.CREATE_USER,username + "," + password1);

                    sendMessage(createUserMessage);

                    //wait for create user response
                    Message createUserResponse = receiveThread.getReceivedMessage();
                    System.out.println(createUserResponse.getData());

                    //If successful then log the user in.
                    if(createUserResponse.getSubMessageType() == 0) {
                        Message loginMessage = new Message(MessageType.LOGIN,username + "," + password1);
                        sendMessage(loginMessage);

                        //wait for response
                        Message loginResponse = receiveThread.getReceivedMessage();
                        System.out.println(loginResponse.getData());

                        if(loginResponse.getSubMessageType() == 0 || loginResponse.getSubMessageType() == 1) {
                            //login success -> create store
                            loggedOn = true;

                            Message createStoreMessage = new Message(MessageType.CREATE_STORE," ");
                            sendMessage(createStoreMessage);

                            receiveThread.getReceivedMessage();

                        }else {
                            System.out.println("An error has occurred...");
                            continue;
                        }

                    }


                } else {
                    //alert user their input was invalid and restart sign in process
                    System.out.println("Error: Improperly formatted response. Please try again.");
                    continue;
                }
            }


            System.out.println("Message format: @username, your message here...");
            System.out.println("Type 'Logout' to exit.");
            //send message loop
            while(loggedOn){
                //this is where the user sends messages
                System.out.print(">");
                String line = reader.readLine();

                if (line.equalsIgnoreCase("logout")) {
                    loggedOn = false;

                    //log user out
                    Message logoutMessage = new Message(MessageType.LOGOFF," ");
                    sendMessage(logoutMessage);

                    receiveThread.getReceivedMessage();

                }  else if(line.startsWith("@")){      //ensure that the username started with a @
                    //split message with fist comma
                     String[] lineArray = line.split(",",2);

                    //ensure the user put a comma by ensuring the line got split into two pieces
                     if (lineArray.length == 2) {
                         //create and send a message with the string the user entered minus that first @
                         Message message = new Message(MessageType.SEND_MESSAGE,line.substring(1));
                         sendMessage(message);

                         //wait for response
                         Message messageResponse = receiveThread.getReceivedMessage();

                         //if response is error -> print error
                         if (messageResponse.getSubMessageType() != 0) {
                             System.out.println(messageResponse.getData());
                         }
                     }else {
                         //error - user entered message with wrong format
                         System.out.println("Syntax Error: Please try again.");
                         System.out.println("Message format: @username, your message here...");
                     }

                }else {
                    //error - user entered message with wrong format
                    System.out.println("Syntax Error: Please try again.");
                    System.out.println("Message format: @username, your message here...");
                }




            }

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

    private static class QueryThread extends Thread{

        @Override
        public void run(){
            while(true){
                //Wait one second
                try{
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                //If user is logged on, send a query
                if(loggedOn){
                    try{
                        sendMessage(new Message(MessageType.QUERY_MESSAGES, " "));
                    }
                    catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}