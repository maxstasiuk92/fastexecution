package fastexecution.processors.abstract_queue_processor;

import fastexecution.ProcessorCallback;
import fastexecution.ProcessorException;
import fastexecution.ProcessorThread;
import fastexecution.processors.AbstractProcessor;
import fastexecution.threads.AbstractThread;

public class EmptyProcessor extends AbstractProcessor<Object>{

	public EmptyProcessor(Object coordinator) throws ProcessorException {
		super(coordinator);
	}
	
	@Override
	protected ProcessorThread getProcessorThread(ProcessorCallback callback, Object coordinator) {
		return new EmptyThread(callback);
	}

}

class EmptyThread extends AbstractThread {
	public EmptyThread(ProcessorCallback callback) {
		super(callback);
	}

	@Override
	protected void function() {
		return;
	}
}
