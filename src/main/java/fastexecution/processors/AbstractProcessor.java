package fastexecution.processors;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import fastexecution.ProcessorCallback;
import fastexecution.ProcessorException;
import fastexecution.ProcessorThread;
import fastexecution.Processor;

public abstract class AbstractProcessor<T> implements Processor {
	protected final T coordinator;
	protected final List<ProcessorThread> processorThreads;
	protected final AtomicBoolean changingThreadNumber;
	protected final AtomicInteger effectiveThreadNumber;
	protected final AtomicBoolean running;
	
	/**
	 * @exception ProcessorException if did not start single non-extra thread
	 * (see {@link #setThreadNumber(int) setExtraThreadNumber})
	 */		
	public AbstractProcessor(T coordinator) throws ProcessorException {
		this.coordinator = coordinator;
		//Collections.synchronizedList(...) has not synchronized iterator 
		this.processorThreads = new LinkedList<ProcessorThread>();
		this.changingThreadNumber = new AtomicBoolean(false);
		this.effectiveThreadNumber = new AtomicInteger(0);
		this.running = new AtomicBoolean(false);
	}
	
	@Override
	public boolean start() {
		if (running.get()) {
			return true;
		}
		if (startThreads(1) == 1) {
			running.set(true);
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public boolean stop() {
		if (!running.get()) {
			return true;
		}
		if (stopThreads(effectiveThreadNumber.get()) < 0) {
			return false;
		} else {
			running.set(false);
			return true;
		}
	}
	
	@Override
	public boolean isRunning() {
		return running.get();
	}
		
	@Override
	public int setThreadNumber(int threadNumber) {
		if (threadNumber < 0 || !running.get()) {
			return -1;
		}
		if (threadNumber == 0) {
			threadNumber = 1;
		}
		int deltaThreads = threadNumber - effectiveThreadNumber.get();
		boolean set = true;
		if (deltaThreads > 0) {
			set = !(startThreads(deltaThreads) < 0);
		} else if (deltaThreads < 0) {
			set = !(stopThreads(-deltaThreads) < 0);
		}
		if (set) {
			return effectiveThreadNumber.get();
		} else {
			return -1;
		}
	}
	
	@Override
	public int getThreadNumber() {
		return effectiveThreadNumber.get();
	}
	
	@Override
	public boolean changedThreadNumber() {
		int processorThreadsSize;
		synchronized (processorThreads) {
			processorThreadsSize = processorThreads.size();
		}
		return processorThreadsSize == effectiveThreadNumber.get();
	}
	
	protected int startThreads(int number) {
		synchronized (changingThreadNumber) {
			if (changingThreadNumber.get()) {
				return -1;
			} else {
				changingThreadNumber.set(true);
			}
		}
		int started = 0;
		ProcessorThread processorThread;
		ArrayList<ProcessorThread> newProcessorThreads = new ArrayList<>(number);
		while (started < number && (processorThread = getProcessorThread(this::onThreadStopped, coordinator)) != null) {
			newProcessorThreads.add(processorThread);
			Thread thread = new Thread(processorThread);
			thread.start();
			started++;
		}
		synchronized (processorThreads) {
			processorThreads.addAll(newProcessorThreads);
		}
		effectiveThreadNumber.addAndGet(started);
		changingThreadNumber.set(false);
		return started;
	}
	
	protected int stopThreads(int number) {
		synchronized (changingThreadNumber) {
			if (changingThreadNumber.get()) {
				return -1;
			} else {
				changingThreadNumber.set(true);
			}
		}
		int stopped = 0;
		synchronized (processorThreads) {
			var iterator = processorThreads.iterator(); //iterator is not concurrent -> quite fast
			while (iterator.hasNext() && stopped < number) {
				ProcessorThread thread = iterator.next();
				if (thread.isActive()) {
					thread.stop();
					stopped++;
				}
			}
		}
		if (stopped != number) {
			throw new ProcessorException("number of stopped threads is not equal to requested number");
		}
		effectiveThreadNumber.addAndGet(-stopped);
		changingThreadNumber.set(false);
		return stopped;
	}
	
	/**
	 * Callback when ProcessorThread is ready to be removed
	 * @exception ProcessorException if invoked by unfamiliar thread
	 */
	protected void onThreadStopped(ProcessorThread thread) throws ProcessorException {
		synchronized (processorThreads) {
			if (!processorThreads.remove(thread)) {
				throw new ProcessorException("onThreadStopped was invoked by unfamiliar thread");
			}
		}
	}
	
	@Override
	protected void finalize() {
		if (!stop()) {
			throw new ProcessorException("did not stop threads during finalization");
		}
	}
	
	/**
	 * ProcessorThread should be activated by default
	 */
	protected abstract ProcessorThread getProcessorThread(ProcessorCallback callback, T coordinator);
}
