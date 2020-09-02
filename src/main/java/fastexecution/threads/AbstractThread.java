package fastexecution.threads;

import java.util.concurrent.atomic.AtomicBoolean;

import fastexecution.ProcessorCallback;
import fastexecution.ProcessorThread;

public abstract class AbstractThread implements ProcessorThread {
	protected AtomicBoolean active;
	ProcessorCallback callback;
	
	public AbstractThread(ProcessorCallback callback) {
		this.active = new AtomicBoolean(true);
		this.callback = callback;
	}
	
	@Override
	public void stop() {
		active.set(false);
	}
	
	@Override
	public boolean isActive() {
		return active.get();
	}
	
	@Override
	public void run() {
		while (active.get()) {
			function();
		}
		callback.onStopped(this);
	}
	
	protected abstract void function();
}
