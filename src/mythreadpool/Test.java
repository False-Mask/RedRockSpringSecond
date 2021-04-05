package mythreadpool;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Test {
    public static void main(String[] args) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(2,4,1000, TimeUnit.MICROSECONDS,new ArrayBlockingQueue<>(1000));
        for (int i=0;i<1000;++i){
            executor.execute(()->{
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("hello world"+Thread.currentThread().getName());
            });
        }

    }
}
