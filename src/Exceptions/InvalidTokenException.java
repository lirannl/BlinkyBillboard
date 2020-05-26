package Exceptions;

public class InvalidTokenException extends Exception{
    byte[] token;
    public InvalidTokenException(byte[] token){
        super("Invalid token");
        this.token = token;
    }

    public byte[] getToken(){
        return this.token;
    }
}
