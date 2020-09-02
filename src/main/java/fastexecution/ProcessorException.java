package fastexecution;

public class ProcessorException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ProcessorException() {
		super();
	}
	
	public ProcessorException(String message) {
		super(message);
	}
}
