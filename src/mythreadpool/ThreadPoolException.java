package mythreadpool;

public class ThreadPoolException extends RuntimeException{
    private final String what;

    public ThreadPoolException(String what){
        this.what = what;
    }

    public void printStackTrace() {
        System.err.println("发生了ThreadPoolException描述如下\n"+what);
    }
}
