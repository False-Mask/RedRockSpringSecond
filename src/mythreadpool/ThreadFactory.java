package mythreadpool;

import java.util.concurrent.ThreadPoolExecutor;

public class ThreadFactory {
    //新建的线程的个数
    private static int number = 0;
    /**
     * 新开一个Thread
     * @param runnable 需要完成的任务
     * @return thread对象
     */
    public static Thread newThread(Runnable runnable){
        return new Thread(runnable,"pool-thread-"+number++);
    }
}
