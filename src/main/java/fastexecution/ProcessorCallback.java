package fastexecution;

public interface ProcessorCallback {
	/**
	 * Thread should call this method right before stop. Thread should not decide to
	 * stop itself. Not following this contract may lead to memory leakage or ProcessorException
	 */
	void onStopped(ProcessorThread thread);
}
