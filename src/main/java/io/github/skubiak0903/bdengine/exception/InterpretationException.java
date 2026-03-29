package io.github.skubiak0903.bdengine.exception;

public class InterpretationException extends RuntimeException {
	private static final long serialVersionUID = -7402859329423662754L;

	public InterpretationException() {
		super();
	}
	
	public InterpretationException(String message) {
		super(message);
	}
	
	public InterpretationException(Throwable cause) {
		super(cause);
	}
	
	public InterpretationException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public InterpretationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
