package fastexecution.threads;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import fastexecution.ProcessorCallback;

public class QueueHandlerThread<T> extends AbstractThread {
	protected AtomicBoolean active;
	Queue<T> requestQueue;
	Consumer<T> function;
	ProcessorCallback callback;
	
	public QueueHandlerThread(ProcessorCallback callback, Queue<T> requestQueue, Consumer<T> function) {
		super(callback);
		this.requestQueue = requestQueue;
		this.function = function;
	}
	
	@Override
	protected void function() {
		T request = requestQueue.poll();
		if (request != null) {
			function.accept(request);
		} else {
			Thread.yield();
		}
	}
}
