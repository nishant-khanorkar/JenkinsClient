package jenkinsclient;

import java.io.IOException;

import org.apache.http.client.HttpResponseException;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.QueueItem;
import com.offbytwo.jenkins.model.QueueReference;

import jenkinsclient.exceptions.JenkinsExecutionException;
import jenkinsclient.exceptions.MaximumOperationsReachedException;

public class QueueOps extends AbstractOps {

	public QueueOps(JenkinsClient client) {
		super(client);
	}

	private Build getBuild(JenkinsServer jenkinsServer, String queueReferenceUrl) throws JenkinsExecutionException {
		try {
			return jenkinsServer.getBuild(jenkinsServer.getQueueItem(new QueueReference(queueReferenceUrl)));
		} catch (IOException e) {
			throw new JenkinsExecutionException(
					String.format("Error getting build with queue reference url %s", queueReferenceUrl), e);
		}
	}

	public QueueItem getQueueItem(JenkinsServer jenkinsServer, String queueReferenceUrl)
			throws MaximumOperationsReachedException, JenkinsExecutionException {
		try {
			return jenkinsServer.getQueueItem(new QueueReference(queueReferenceUrl));
		} catch (IOException e) {
			throw new JenkinsExecutionException(
					String.format("Error checking if build %s is in queue", queueReferenceUrl), e);
		}
	}

	/**
	 * Get the build Url
	 * 
	 * @param queueReferenceUrl
	 *            (ex. "jenkinsUrl/queue/item/itemId/") was returned when the job
	 *            was triggered.
	 * @throws JenkinsExecutionException
	 * @throws MaximumOperationsReachedException
	 */
	public String getBuildUrl(String queueReferenceUrl)
			throws MaximumOperationsReachedException, JenkinsExecutionException {
		JenkinsServer jenkinsServer = null;
		try {
			jenkinsServer = client.getJenkinsConnection();
			return getBuild(jenkinsServer, queueReferenceUrl).getUrl();
		} finally {
			client.closeJenkinsConnection(jenkinsServer);
		}
	}

	/**
	 * Get the build number
	 * 
	 * @param queueReferenceUrl
	 *            (ex. "jenkinsUrl/queue/item/itemId/") was returned when the job
	 *            was triggered.
	 * @throws MaximumOperationsReachedException
	 * @throws JenkinsExecutionException
	 */
	public int getBuildNumber(String queueReferenceUrl)
			throws MaximumOperationsReachedException, JenkinsExecutionException {
		JenkinsServer jenkinsServer = null;
		try {
			jenkinsServer = client.getJenkinsConnection();
			return getBuild(jenkinsServer, queueReferenceUrl).getNumber();
		} finally {
			client.closeJenkinsConnection(jenkinsServer);
		}
	}

	/**
	 * To check if the triggered job is still in queue.
	 * 
	 * @param queueReferenceUrl
	 *            (ex. "jenkinsUrl/queue/item/itemId/") was returned when the job
	 *            was triggered.
	 * @throws MaximumOperationsReachedException
	 * @throws JenkinsExecutionException
	 */
	public boolean isInQueue(String queueReferenceUrl)
			throws MaximumOperationsReachedException, JenkinsExecutionException {
		JenkinsServer jenkinsServer = null;
		try {
			jenkinsServer = client.getJenkinsConnection();
			QueueItem q = getQueueItem(jenkinsServer, queueReferenceUrl);
			return q != null && q.getExecutable() == null ? true : false;
		} finally {
			client.closeJenkinsConnection(jenkinsServer);
		}
	}

	/**
	 * To check if the triggered job is cancelled by the user in the queue
	 * 
	 * @param queueReferenceUrl
	 *            (ex. "jenkinsUrl/queue/item/itemId/") was returned when the job
	 *            was triggered.
	 * @return
	 * @throws MaximumOperationsReachedException
	 * @throws JenkinsExecutionException
	 */
	public boolean isCancelledInQueue(String queueReferenceUrl)
			throws MaximumOperationsReachedException, JenkinsExecutionException {
		JenkinsServer jenkinsServer = null;
		try {
			jenkinsServer = client.getJenkinsConnection();
			QueueItem q = getQueueItem(jenkinsServer, queueReferenceUrl);
			return (q != null && q.getExecutable() == null && q.isCancelled()) ? true : false;
		} finally {
			client.closeJenkinsConnection(jenkinsServer);
		}
	}

	/**
	 * Stops build corresponding to the queueReferenceUrl currently queued and not
	 * picked up by an executor.
	 * 
	 * @param queueReferenceUrl
	 *            (ex. "jenkinsUrl/queue/item/itemId/") was returned when the job
	 *            was triggered.
	 * @throws MaximumOperationsReachedException
	 * @throws JenkinsExecutionException
	 */
	public void stopQueuedBuild(String queueReferenceUrl)
			throws MaximumOperationsReachedException, JenkinsExecutionException {
		JenkinsServer jenkinsServer = null;
		try {
			jenkinsServer = client.getJenkinsConnection();

			QueueItem q = getQueueItem(jenkinsServer, queueReferenceUrl);
			String itemId = q.getId().toString();

			try {
				client.getJenkinsHttpConnection().get(client.getJenkinsUrl() + "/queue/cancelItem?id=" + itemId);
			} catch (IOException ex) {
				if (((HttpResponseException) ex).getStatusCode() == 405) {
					try {
						client.getJenkinsHttpConnection()
								.post(client.getJenkinsUrl() + "/queue/cancelItem?id=" + itemId, client.getCrumbFlag());
					} catch (IOException e) {
						throw new JenkinsExecutionException(String.format(
								"Error stopping queued build with queue reference url %s", queueReferenceUrl), e);
					}
				}
			}
		} finally {
			client.closeJenkinsConnection(jenkinsServer);
		}
	}
}
