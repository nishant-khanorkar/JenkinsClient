package jenkinsclient.exceptions;

public class JenkinsExecutionException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8925234960449466517L;

	public JenkinsExecutionException(String message) {
		super(message);
	}

	public JenkinsExecutionException(String message, Throwable e) {
		super(String.format("%s. %s", message, e.getMessage()), e);
	}
}
