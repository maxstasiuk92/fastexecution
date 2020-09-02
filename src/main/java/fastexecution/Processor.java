package fastexecution;

public interface Processor {
	
	public boolean start();
	
	public boolean stop();
	
	public boolean isRunning();
	
	/**
	 * At least 1 thread is always active and processes requests. Method sets number of extra threads. 
	 * If new thread should be created - method returns after they are started.
	 * If some threads should be removed - method notify that threads and returns. Threads will be actually removed
	 * after they stop
	 * @return Actual number of extra threads or -1 if other thread is running this method(this may be a signal
	 * to load balancer) or starting/stopping processor or processor is stopped
	 */
	int setThreadNumber(int extraThreads);
	
	int getThreadNumber();
	
	/**
	 * {@link #setThreadNumber(int) setExtraThreadNumber} do not instantly stops and removes
	 * threads. This method checks if change of thread number took effect
	 * @return true if change of thread number took effect
	 */
	boolean changedThreadNumber();
}
