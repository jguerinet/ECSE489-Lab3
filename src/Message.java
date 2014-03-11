/**
 * Author: Julien
 * Date: 2014-03-11 9:04
 */
public class Message {
    private int messageType, subMessageType;
    private String data;

    public Message(int messageType, int subMessageType, String data){
        this.messageType = messageType;
        this.subMessageType = subMessageType;
        this.data = data;
    }

    public int getMessageType(){
        return this.messageType;
    }

    public int getSubMessageType(){
        return this.subMessageType;
    }

    public String getData(){
        return this.data;
    }
}
