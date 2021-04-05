package mythreadpool;

import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadPoolExecutor {

    //定义线程可见的属性
    private final int corePoolSize;
    private final long keepAliveTime;
    private final int maximumPoolSize;
    private final BlockingQueue<Runnable> workQueue;
    
    //ThreadPoolExecutor的工作线程
    //final Thread thread

    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));

    private final HashSet<Worker> workers = new HashSet<>();

    //Integer的最大二进制位数-3 是个’工具人‘Integer
    private static final int COUNT_BITS = Integer.SIZE - 3;

    //用于将ctl解析成workCount和runStatus的‘工具人’Integer
    private static final int COUNT_MASK = (1 << COUNT_BITS) - 1;

    //表示状态的变量
    private static final int RUNNING    = -1 << COUNT_BITS;
    private static final int SHUTDOWN   =  0 << COUNT_BITS;
    private static final int STOP       =  1 << COUNT_BITS;
    private static final int TIDYING    =  2 << COUNT_BITS;
    private static final int TERMINATED =  3 << COUNT_BITS;

    private static final ReentrantLock lock = new ReentrantLock();
    /**
     * 生成一个包含RunStatus和WorkCount的Integer
     * @param runState 运行状态
     * @param workCount 当前工作线程的数量
     * @return Integer
     */
    private int ctlOf(int runState, int workCount) {
        return runState | workCount;
    }

    /**
     * 获取ctl中的RunStatus
     * @param ctl ctl实例变量
     * @return RunStatus的值
     */
    private static int runStateOf(int ctl){ return ctl & ~COUNT_MASK; }

    /**
     * 获取ctl中的WorkCount
     * @param ctl ctl实例变量
     * @return WorkCount的值
     */
    private static int workerCountOf(int ctl){ return ctl & COUNT_MASK; }

    /**
     * 比较RunStatus的位置
     * @param c ctl实例变量
     * @param s 比较的Status
     * @return 如果当前的Running状态小于传入的状态返回true 否者返回false
     */
    private static boolean runStateLessThan(int c, int s) {
        return c < s;
    }

    /**
     * 比较RunStatus的位置
     * @param c ctl实例变量
     * @param s 比较的Status
     * @return 如果当前的Running状态大于等于传入的状态返回true 否者返回false
     */
    private static boolean runStateAtLeast(int c, int s) {
        return c >= s;
    }

    /**
     * RunStatus是否是Running状态
     * @param c ctl实例变量
     * @return 如果当前状态为Running -> ture
     *         非Running -> false
     */
    private static boolean isRunning(int c) {
        return c < SHUTDOWN;
    }
    //原子性-- 并获取更新后的值
    private void decrementWorkerCount() {
        ctl.addAndGet(-1);
    }

    /**
     *线程池的构造函数 用于配置各类参数
     * @param corePoolSize 核心线程数
     * @param maximumPoolSize 最大线程数量
     * @param keepAliveTime 非核心线程闲置时候的存活时间
     * @param unit keepAliveTime的单位 ms/s/min/hour/day
     * @param workQueue 任务队列
     * @throws ThreadPoolException 当ThreadPool出现异常的时候抛出
     *  1.核心数大于最大线程数
     */
    public ThreadPoolExecutor(
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue)  {
        if (corePoolSize > maximumPoolSize){
            //throw new ThreadPoolException("核心线程数大于最大线程数");
        }
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.keepAliveTime = unit.toNanos(keepAliveTime);
        this.workQueue = workQueue;
    }

    //工作线程

    public class Worker implements Runnable {
        private Runnable runnable;
        final Thread thread;
        boolean isCore;


        /**
         * Worker对象的构造方法
         * @param runnable 传入Runnable对象初始化thread 和 runnable
         */
        public Worker(Runnable runnable,boolean isCore){
            this.runnable = runnable;
            this.thread = ThreadFactory.newThread(this);
            this.isCore = isCore;
        }

        @Override
        public void run() {
            runWork(this);
        }
    }

    public void execute(Runnable runnable) {
        if (runnable == null)
            throw new NullPointerException("execute的参数不能为空");
//        int c = ctl.get();
//        if (workerCountOf(c) < corePoolSize) {
//            if (addWorker(runnable , true))
//                return;
//            ctl.compareAndSet(c,c+1);
//        }else if (workerCountOf(c) > corePoolSize && workerCountOf(c) < maximumPoolSize){
//            if (addWorker(runnable , false)) return;
//        }else if (workerCountOf(c) >= maximumPoolSize && workerCountOf(c)< maximumPoolSize + workQueue.size()){
//            if (workQueue.offer(runnable)){
//                System.out.println("任务队列加入失败");
//            }
        addWorker(runnable);
    }

    private boolean addWorker(Runnable firstTask){
//        Worker worker = new Worker(firstTask, core);
//        final Thread thread = worker.thread;
//        //开启一个新的线程
//        if (thread!=null) thread.start();
//        return thread!=null;
        int c = ctl.get();
        //如果当前线程的数量小于核心线程数 只添加核心线程
        if (workerCountOf(c) < corePoolSize){
            Worker worker = new Worker(firstTask, true);
            final Thread thread = worker.thread;
            if (thread!=null){
                thread.start();
                ctl.compareAndSet(c,ctlOf(RUNNING,c+1));
                return true;
            }else {
                return false;
            }
        }
        //如果当前线程数大于核心线程数并且小于总线程数 只添加非核心线程
        else if (workerCountOf(c) < maximumPoolSize){
            Worker worker = new Worker(firstTask,false);
            final Thread thread = worker.thread;
            if (thread!=null){
                thread.start();
                ctl.compareAndSet(c,ctlOf(RUNNING,c+1));
                return true;
            }
            return false;
        }
        //如果当前线程数大于最大线程 那么就放入阻塞队列里面缓存
        else if (workQueue.offer(firstTask)){
//            boolean isSucceed = workQueue.offer(firstTask);
//            if (isSucceed) ctl.compareAndSet(c,ctlOf(RUNNING,c+1));
//            return isSucceed;
            ctl.compareAndSet(c,ctlOf(RUNNING,c+1));
            return true;
        }
        //如果大于最大容纳的数量(容纳最大线程数+阻塞队列长度)
        else {
            return false;
        }
    }

    /**
     * 执行任务
     * @param worker 当前运行的worker的对象
     */
    private void runWork(Worker worker){
        Thread thread = Thread.currentThread();
        //第一次获取任务
        Runnable task = worker.runnable;
        //缓存到task以后runnable设置为null
        worker.runnable = null;
        while( task!=null || (task = getTask(worker))!=null ){
            task.run();
            task = null;
        }
        //退出循环说明该线程是非核心线程 并且 该线程已经闲置超过 keepAliveTime
        clearTheWorker(worker);
    }

    /**
     * 清除传入的worker
     * @param worker 传入的需要移除的worker
     */
    private void clearTheWorker(Worker worker) {
        workers.remove(worker);
    }

    private Runnable getTask(Worker worker)  {
        Runnable r = null;
            try {
                r = worker.isCore ? workQueue.take():
                        workQueue.poll(keepAliveTime,TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        return r;
    }
}