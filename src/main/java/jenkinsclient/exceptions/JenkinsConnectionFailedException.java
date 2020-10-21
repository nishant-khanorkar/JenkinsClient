package jenkinsclient.exceptions;

public class JenkinsConnectionFailedException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2709841732920216532L;

	public JenkinsConnectionFailedException(String message, Throwable e) {
		super(message, e);
	}

}
