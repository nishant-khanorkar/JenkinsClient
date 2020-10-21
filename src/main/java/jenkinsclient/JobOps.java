package jenkinsclient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.FolderJob;
import com.offbytwo.jenkins.model.JobWithDetails;

import jenkinsclient.exceptions.JenkinsExecutionException;
import jenkinsclient.exceptions.MaximumOperationsReachedException;

public class JobOps extends AbstractOps {

	public JobOps(JenkinsClient client) {
		super(client);
	}

	/**
	 * Will create folders and subfolders if not created.
	 * 
	 * @param folderPath
	 *            Path of the folder to be created , for example "/Project/A/B/C" or
	 *            "/Project".
	 * @throws MaximumOperationsReachedException
	 * @throws JenkinsExecutionException
	 */
	public void createFolder(String folderPath) throws MaximumOperationsReachedException, JenkinsExecutionException {
		JenkinsServer jenkinsServer = null;
		try {
			jenkinsServer = client.getJenkinsConnection();
			String[] folders = folderPath.split("/");
			StringBuilder folderUrl = new StringBuilder("/");
			FolderJob folderJob = null;
			for (String folder : folders) {
				if (folder.isEmpty())
					continue;
				folderJob = new FolderJob("", folderUrl.toString());
				try {
					if (jenkinsServer.getJob(folderJob, folder) == null)
						jenkinsServer.createFolder(folderJob, folder, client.getCrumbFlag());
				} catch (IOException e) {
					throw new JenkinsExecutionException("Error creating folder", e);
				}
				folderUrl.append("job/").append(folder).append("/");
			}
		} finally {
			client.closeJenkinsConnection(jenkinsServer);
		}
	}

	/**
	 * Will delete folder or job if present.
	 * 
	 * @param path
	 *            Path of the folder or job to be deleted, for example
	 *            "/Project/A/B/C" will delete "C" folder or job.
	 * @throws MaximumOperationsReachedException
	 * @throws JenkinsExecutionException
	 */
	public void deleteFolderOrJob(String path) throws MaximumOperationsReachedException, JenkinsExecutionException {
		JenkinsServer jenkinsServer = null;
		try {
			jenkinsServer = client.getJenkinsConnection();
			String[] folders = path.split("/");
			String folderToDelete = folders[folders.length - 1];
			folders[folders.length - 1] = "";
			FolderJob folderJob = new FolderJob("", generateUrl(folders));
			try {
				if (jenkinsServer.getJob(folderJob, folderToDelete) != null)
					jenkinsServer.deleteJob(folderJob, folderToDelete, client.getCrumbFlag());
			} catch (IOException e) {
				throw new JenkinsExecutionException("Error deleting folder or job", e);
			}
		} finally {
			client.closeJenkinsConnection(jenkinsServer);
		}
	}

	/**
	 * Create a job on the server using the provided xml and in the provided folder.
	 * 
	 * @param folderPath
	 *            Path of the folder where job will be created, for example
	 *            "/Project/A/B" will create job in "B" folder.
	 * @param jobName
	 *            Name of the job to be created.
	 * @param jobXml
	 *            The config.xml which should be used to create the job.
	 * @throws MaximumOperationsReachedException
	 * @throws JenkinsExecutionException
	 */
	public void createJob(String folderPath, String jobName, String jobXml)
			throws MaximumOperationsReachedException, JenkinsExecutionException {
		JenkinsServer jenkinsServer = null;
		try {
			jenkinsServer = client.getJenkinsConnection();
			String[] folders = folderPath.split("/");
			FolderJob folderJob = new FolderJob("", generateUrl(folders));
			try {
				jenkinsServer.createJob(folderJob, jobName, jobXml, client.getCrumbFlag());
			} catch (IOException e) {
				throw new JenkinsExecutionException(String.format("Error creating job %s", jobName), e);
			}
		} finally {
			client.closeJenkinsConnection(jenkinsServer);
		}
	}

	/**
	 * Will trigger build for a job specified by jobPath with parameters.
	 * 
	 * @param jobPath
	 *            Path of the job, for example "/ProjectABC/JobA" will trigger job
	 *            "JobA".
	 * 
	 * @return queueReferenceUrl (ex. "jenkinsUrl/queue/item/itemId") the job's
	 *         reference url for the QueueItem
	 * @throws MaximumOperationsReachedException
	 * @throws JenkinsExecutionException
	 */
	public String triggerJob(String jobPath, Map<String, String> params)
			throws MaximumOperationsReachedException, JenkinsExecutionException {
		JenkinsServer jenkinsServer = null;
		try {
			jenkinsServer = client.getJenkinsConnection();
			String[] folders = jobPath.split("/");
			String jobToTrigger = folders[folders.length - 1];
			folders[folders.length - 1] = "";
			FolderJob folderJob = new FolderJob("", generateUrl(folders));
			JobWithDetails job = null;
			try {
				job = jenkinsServer.getJob(folderJob, jobToTrigger);
			} catch (IOException e) {
				throw new JenkinsExecutionException(String.format("Error triggering job %s", jobPath), e);
			}
			if (job != null) {
				if (params == null)
					params = new HashMap<String, String>();
				params.put("delay", "0sec");
				try {
					return job.build(params, client.getCrumbFlag()).getQueueItemUrlPart();
				} catch (IOException e) {
					throw new JenkinsExecutionException(String.format("Error triggering job %s", jobPath), e);
				}
			} else
				throw new JenkinsExecutionException(String.format("Job %s not found.", jobToTrigger));
		} finally {
			client.closeJenkinsConnection(jenkinsServer);
		}
	}
}
