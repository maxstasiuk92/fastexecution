package fastexecution;

public interface LoadBalancer<T> {
	void registerProcessor(Processor processor, T coordinator);
}
