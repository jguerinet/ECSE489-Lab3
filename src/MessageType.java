/**
 * Author: Julien
 * Date: 2014-03-11 9:32
 */
public enum MessageType {
    EXIT,
    BADLY_FORMATTED_MESSAGE,
    ECHO,
    LOGIN,
    LOGOFF,
    CREATE_USER,
    DELETE_USER,
    CREATE_STORE,
    SEND_MESSAGE,
    QUERY_MESSAGES;

    public int getMessageTypeInt(){
        return this.ordinal();
    }

    public static MessageType getMessageType(int messageTypeInt){
        return MessageType.values()[messageTypeInt];
    }
}
