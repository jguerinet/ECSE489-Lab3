/**
 * Author: Julien
 * Date: 2014-03-11 9:04
 */
public class Message {
    private MessageType messageType;
    private int subMessageType;
    private String data;

    public Message(MessageType messageType, int subMessageType, String data){
        this.messageType = messageType;
        this.subMessageType = subMessageType;
        this.data = data;
    }

    public MessageType getMessageType(){
        return this.messageType;
    }

    public int getSubMessageType(){
        return this.subMessageType;
    }

    public String getData(){
        return this.data;
    }
}
