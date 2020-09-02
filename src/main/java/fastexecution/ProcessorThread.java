package fastexecution;

public interface ProcessorThread extends Runnable {
	void stop();
	boolean isActive();
}
