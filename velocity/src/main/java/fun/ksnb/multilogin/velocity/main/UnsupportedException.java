package fun.ksnb.multilogin.velocity.main;

/**
 * 不支持异常
 */
public class UnsupportedException extends Exception{
    public UnsupportedException(String message) {
        super(message);
    }
}
