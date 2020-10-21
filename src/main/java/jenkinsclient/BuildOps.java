package jenkinsclient;

import java.io.IOException;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildResult;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.FolderJob;
import com.offbytwo.jenkins.model.JobWithDetails;

import jenkinsclient.exceptions.JenkinsExecutionException;
import jenkinsclient.exceptions.MaximumOperationsReachedException;

public class BuildOps extends AbstractOps {

	public BuildOps(JenkinsClient client) {
		super(client);
	}

	/**
	 * Returns Build object for the job specified by jobPath and buildNumber.
	 * 
	 * @param jobPath
	 *            Path of the job, for example "/Project/JobA"
	 *            implies for the job "JobA".
	 */
	private Build getBuild(JenkinsServer jenkinsServer, String jobPath, int buildNumber)
			throws JenkinsExecutionException {
		String[] folders = jobPath.split("/");
		String jobName = folders[folders.length - 1];
		folders[folders.length - 1] = "";
		FolderJob folderJob = new FolderJob("", generateUrl(folders));
		JobWithDetails job;
		try {
			job = jenkinsServer.getJob(folderJob, jobName);
		} catch (IOException e) {
			throw new JenkinsExecutionException(
					String.format("Error getting build with build number %s and job path %s", buildNumber, jobPath), e);
		}
		if (job != null) {
			return job.getBuildByNumber(buildNumber);
		} else
			throw new JenkinsExecutionException(String.format("Job %s not found.", jobName));
	}

	/**
	 * Get the status of Build for job specified
	 * 
	 * @param jobPath
	 *            Path of the job, for example "/Project/JobA" implies for the job
	 *            "JobA"
	 * @param buildNumber
	 * @throws JenkinsExecutionException
	 * @throws MaximumOperationsReachedException
	 */
	public BuildResult getBuildResult(String jobPath, int buildNumber)
			throws MaximumOperationsReachedException, JenkinsExecutionException {
		JenkinsServer jenkinsServer = null;
		try {
			jenkinsServer = client.getJenkinsConnection();
			BuildWithDetails b;
			try {
				b = getBuild(jenkinsServer, jobPath, buildNumber).details();
			} catch (IOException e) {
				throw new JenkinsExecutionException(
						String.format("Error getting build status for build with build number %s and job path %s",
								buildNumber, jobPath),
						e);
			}
			if (b.getResult() == null) {
				return b.isBuilding() ? BuildResult.BUILDING : BuildResult.UNKNOWN;
			} else
				return b.getResult();
		} finally {
			client.closeJenkinsConnection(jenkinsServer);
		}
	}

	/**
	 * Stops build for the job specified by jobPath and buildNumber currently being
	 * executed by an executor.
	 * 
	 * @param jobPath
	 *            Path of the job, for example "/Project/JobA"
	 *            implies for the job "JobA".
	 * @param buildNumber
	 * @throws MaximumOperationsReachedException
	 * @throws JenkinsExecutionException
	 */
	public void stopRunningBuild(String jobPath, int buildNumber)
			throws MaximumOperationsReachedException, JenkinsExecutionException {
		JenkinsServer jenkinsServer = null;
		try {
			jenkinsServer = client.getJenkinsConnection();
			Build b = getBuild(jenkinsServer, jobPath, buildNumber);
			try {
				b.Stop(client.getCrumbFlag());
			} catch (IOException e) {
				throw new JenkinsExecutionException(String
						.format("Error stopping build with build number %s and job path %s", buildNumber, jobPath), e);
			}
		} finally {
			client.closeJenkinsConnection(jenkinsServer);
		}
	}
}