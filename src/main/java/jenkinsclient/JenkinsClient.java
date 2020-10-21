package jenkinsclient;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.HttpResponseException;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.client.JenkinsHttpConnection;

import jenkinsclient.exceptions.InvalidJenkinsClientException;
import jenkinsclient.exceptions.JenkinsConnectionFailedException;
import jenkinsclient.exceptions.JenkinsExecutionException;
import jenkinsclient.exceptions.MaximumOperationsReachedException;

public class JenkinsClient {

	private static final int MAX_OPERATIONS = 20;
	private int liveOperationCount = 0;

	private synchronized int getLiveOperationCount() {
		return liveOperationCount;
	}

	private synchronized void incrementLiveOperationCount() {
		++liveOperationCount;
	}

	private synchronized void decrementLiveOperationCount() {
		--liveOperationCount;
	}

	// ----------------------------------------------------

	private static HashMap<String, Jenkins> jenkinsMap = new HashMap<String, Jenkins>();
	private static HashMap<Jenkins, JenkinsClient> jenkinsClientMap = new HashMap<Jenkins, JenkinsClient>();

	private String jenkinsUrl;
	private String username;
	private String password;

	public String getJenkinsUrl() {
		return jenkinsUrl;
	}

	public String getUsername() {
		return username;
	}

	private JenkinsHttpConnection jenkinsHttpConnection;
	private boolean crumbFlag;

	private void setJenkinsHttpConnection() throws InvalidJenkinsClientException {
		JenkinsServer jenkinsServer = null;
		try {
			try {
				jenkinsServer = getJenkinsConnection();
				this.jenkinsHttpConnection = jenkinsServer.getQueue().getClient();
			} catch (IOException | MaximumOperationsReachedException e) {
				throw new InvalidJenkinsClientException(
						String.format("Provided Jenkins client %s is invalid", jenkinsUrl));
			}
		} finally {
			closeJenkinsConnection(jenkinsServer);
		}
	}

	JenkinsHttpConnection getJenkinsHttpConnection() {
		return jenkinsHttpConnection;
	}

	private void setCrumbFlag() {
		boolean flag = false;
		JenkinsServer jenkinsServer = null;
		try {
			try {
				jenkinsServer = getJenkinsConnection();
				jenkinsServer.createFolder("CrumbTest403");
				jenkinsServer.deleteJob("CrumbTest403");
			} catch (IOException | MaximumOperationsReachedException ex) {
				if (((HttpResponseException) ex).getStatusCode() == 403)
					flag = true;
			}
			this.crumbFlag = flag;
		} finally {
			closeJenkinsConnection(jenkinsServer);
		}
	}

	boolean getCrumbFlag() {
		return this.crumbFlag;
	}

	private JenkinsClient(String jenkinsUrl, String username, String password) throws InvalidJenkinsClientException {
		this.jenkinsUrl = jenkinsUrl;
		this.username = username;
		this.password = password;
		checkIfJenkinsServerIsValid();
		setCrumbFlag();
		setJenkinsHttpConnection();
	}

	private boolean checkIfJenkinsServerIsValid() throws InvalidJenkinsClientException {
		JenkinsServer jenkinsServer = null;
		try {
			jenkinsServer = getJenkinsConnection();
			jenkinsServer.getComputers();
			String jenkinsVersion = jenkinsServer.getVersion().toString();
			return jenkinsVersion.equals("") ? false : true;
		} catch (MaximumOperationsReachedException | JenkinsConnectionFailedException | IOException e) {
			throw new InvalidJenkinsClientException(String.format("Provided Jenkins client %s is invalid", jenkinsUrl));
		} finally {
			closeJenkinsConnection(jenkinsServer);
		}
	}

	JenkinsServer getJenkinsConnection() throws MaximumOperationsReachedException {
		incrementLiveOperationCount();
		if (getLiveOperationCount() > MAX_OPERATIONS)
			throw new MaximumOperationsReachedException(
					String.format("Maximum simultaneous operation %s limit reached", MAX_OPERATIONS));
		try {
			if (username == null || password == null || username.isEmpty() || password.isEmpty())
				return new JenkinsServer(new URI(jenkinsUrl));
			else
				return new JenkinsServer(new URI(jenkinsUrl), username, password);
		} catch (URISyntaxException e) {
			throw new JenkinsConnectionFailedException("Jenkins connection failed", e);
		}
	}

	void closeJenkinsConnection(JenkinsServer jenkinsServer) {
		if (jenkinsServer != null)
			try {
				jenkinsServer.close();
			} catch (Exception e) {

			}
		decrementLiveOperationCount();
	}

	public synchronized static String createClient(String jenkinsUrl, String username, String password)
			throws InvalidJenkinsClientException {
		Jenkins jenkins = new Jenkins(jenkinsUrl, username, password);
		for (Map.Entry<String, Jenkins> jenkinsMapEntry : jenkinsMap.entrySet()) {
			if (jenkinsMapEntry.getValue().compareTo(jenkins) == 0)
				return jenkinsMapEntry.getKey();
		}

		String jenkinsId = jenkins.hashCode() + "ID" + jenkinsMap.size();
		jenkins.setJenkinsId(jenkinsId);
		JenkinsClient jenkinsClient = new JenkinsClient(jenkinsUrl, username, password);

		jenkinsMap.put(jenkinsId, jenkins);
		jenkinsClientMap.put(jenkins, jenkinsClient);

		return jenkinsId;
	}

	public static JenkinsClient getClient(String jenkinsId) throws InvalidJenkinsClientException {
		Jenkins jenkins = jenkinsMap.get(jenkinsId);
		if (jenkins == null) {
			throw new InvalidJenkinsClientException("Invalid jenkinsId : Found no Jenkins mapped to this id");
		} else {
			return jenkinsClientMap.get(jenkins);
		}
	}

	// Methods --------------------------------------------

	/**
	 * Get the jenkins version
	 * 
	 * @return Jenkins version
	 * @throws MaximumOperationsReachedException
	 */
	public String getJenkinsVersion() throws MaximumOperationsReachedException {
		JenkinsServer jenkinsServer = null;
		try {
			jenkinsServer = getJenkinsConnection();
			return jenkinsServer.getVersion().toString();
		} finally {
			closeJenkinsConnection(jenkinsServer);
		}
	}

	/**
	 * Runs the provided groovy script on the server and returns the result.
	 * 
	 * @param script
	 *            The script to be executed.
	 * @throws MaximumOperationsReachedException
	 * @throws URISyntaxException
	 * @throws IOException
	 * @throws JenkinsExecutionException
	 */
	public String runGroovyScript(String script) throws MaximumOperationsReachedException, JenkinsExecutionException {
		JenkinsServer jenkinsServer = null;
		try {
			jenkinsServer = getJenkinsConnection();
			try {
				return jenkinsServer.runScript(script, crumbFlag);
			} catch (IOException e) {
				throw new JenkinsExecutionException("Error executing groovy script", e);
			}
		} finally {
			closeJenkinsConnection(jenkinsServer);
		}
	}

	/**
	 * Get total executor count
	 * 
	 * @param labelName
	 * @return
	 * @throws URISyntaxException
	 * @throws MaximumOperationsReachedException
	 * @throws JenkinsExecutionException
	 */
	public int getTotalExecutors(String labelName) throws MaximumOperationsReachedException, JenkinsExecutionException {
		JenkinsServer jenkinsServer = null;
		try {
			jenkinsServer = getJenkinsConnection();
			return jenkinsServer.getLabel(labelName).getTotalExecutors();
		} catch (IOException e) {
			throw new JenkinsExecutionException("Error getting total executors", e);
		} finally {
			closeJenkinsConnection(jenkinsServer);
		}
	}

	/**
	 * Get idle executor count
	 * 
	 * @param labelName
	 * @return
	 * @throws URISyntaxException
	 * @throws MaximumOperationsReachedException
	 * @throws JenkinsExecutionException
	 */
	public int getIdleExecutors(String labelName)
			throws URISyntaxException, MaximumOperationsReachedException, JenkinsExecutionException {
		JenkinsServer jenkinsServer = null;
		try {
			jenkinsServer = getJenkinsConnection();
			return jenkinsServer.getLabel(labelName).getIdleExecutors();
		} catch (IOException e) {
			throw new JenkinsExecutionException("Error getting idle executors", e);
		} finally {
			closeJenkinsConnection(jenkinsServer);
		}
	}

	// Objects --------------------------------------------

	public JobOps jobOps() {
		return new JobOps(this);
	}

	public QueueOps queueOps() {
		return new QueueOps(this);
	}

	public BuildOps buildOps() {
		return new BuildOps(this);
	}

}
