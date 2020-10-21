package jenkinsclient.test;

import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.offbytwo.jenkins.model.TestCase;

import jenkinsclient.JenkinsClient;

@RunWith(Parameterized.class)
public class JenkinsClientApiTest extends TestCase implements Runnable {

	private static String[][] jenkins = new String[][] {
			{ "http://<jenkins1-site-url>:<port>", "<username>", "<password>" },
			{ "http://<jenkins2-site-url>:<port>", "<username>", "<password>" } };

	// Run As JUnit Test

	@Parameterized.Parameters
	public static Collection<String[]> primeNumbers() {
		return Arrays.asList(jenkins);
	}

	private String jenkinsUrl;
	private String username;
	private String password;
	private String labelName = "master";
	private static String folderPath = "/TestFolder1/TestFolder2/TestFolder3";
	private String jobName = "RandomTestJob";
	private String jobPath = folderPath + '/' + jobName;
	
	// Copy xml from resources file
	private static String jobXml = 
			"<?xml version='1.0' encoding='UTF-8'?>\r\n" + 
			"<project>\r\n" + 
			"	<actions />\r\n" + 
			"	<description></description>\r\n" + 
			"	<keepDependencies>false</keepDependencies>\r\n" + 
			"	<properties>\r\n" + 
			"		<hudson.model.ParametersDefinitionProperty>\r\n" + 
			"			<parameterDefinitions>\r\n" + 
			"				<hudson.model.StringParameterDefinition>\r\n" + 
			"					<name>abc</name>\r\n" + 
			"					<description>abc</description>\r\n" + 
			"					<defaultValue>abc</defaultValue>\r\n" + 
			"				</hudson.model.StringParameterDefinition>\r\n" + 
			"			</parameterDefinitions>\r\n" + 
			"		</hudson.model.ParametersDefinitionProperty>\r\n" + 
			"	</properties>\r\n" + 
			"	<scm class=\"hudson.scm.NullSCM\" />\r\n" + 
			"	<assignedNode>master</assignedNode>\r\n" + 
			"	<canRoam>false</canRoam>\r\n" + 
			"	<disabled>false</disabled>\r\n" + 
			"	<blockBuildWhenDownstreamBuilding>false\r\n" + 
			"	</blockBuildWhenDownstreamBuilding>\r\n" + 
			"	<blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>\r\n" + 
			"	<triggers />\r\n" + 
			"	<concurrentBuild>true</concurrentBuild>\r\n" + 
			"	<builders>\r\n" + 
			"		<hudson.tasks.Shell>\r\n" + 
			"			<command>sleep 1m</command>\r\n" + 
			"		</hudson.tasks.Shell>\r\n" + 
			"	</builders>\r\n" +
			"	<publishers />\r\n" + 
			"	<buildWrappers />\r\n" + 
			"</project>";

	private Map<String, String> params = new HashMap<String, String>();

	public JenkinsClientApiTest(String jenkinsUrl, String username, String password) {
		this.jenkinsUrl = jenkinsUrl;
		this.username = username;
		this.password = password;
	}

	@Test
	public void establishConnection() throws URISyntaxException, IOException, Exception {
		String jenkinsId = JenkinsClient.createClient(jenkinsUrl, username, password);
		JenkinsClient jenkinsClient = JenkinsClient.getClient(jenkinsId);
		assertFalse(jenkinsClient.getJenkinsVersion().isEmpty());
	}

	@Test
	public void createAndDeleteFolder() throws URISyntaxException, IOException, Exception {
		String jenkinsId = JenkinsClient.createClient(jenkinsUrl, username, password);
		JenkinsClient jenkinsClient = JenkinsClient.getClient(jenkinsId);
		jenkinsClient.jobOps().createFolder(folderPath);
		Thread.sleep(10000);
		jenkinsClient.jobOps().deleteFolderOrJob(folderPath);
	}

	@Test
	public void createAndDeleteJob() throws URISyntaxException, IOException, Exception {
		String jenkinsId = JenkinsClient.createClient(jenkinsUrl, username, password);
		JenkinsClient jenkinsClient = JenkinsClient.getClient(jenkinsId);
		jenkinsClient.jobOps().deleteFolderOrJob(folderPath);
		jenkinsClient.jobOps().createFolder(folderPath);
		jenkinsClient.jobOps().createJob(folderPath, jobName, jobXml);
		Thread.sleep(10000);
		jenkinsClient.jobOps().deleteFolderOrJob(jobPath);
	}

	@Test
	public void triggerJob() throws URISyntaxException, IOException, Exception {
		String jenkinsId = JenkinsClient.createClient(jenkinsUrl, username, password);
		JenkinsClient jenkinsClient = JenkinsClient.getClient(jenkinsId);
		jenkinsClient.jobOps().createFolder(folderPath);
		try {
			jenkinsClient.jobOps().createJob(folderPath, jobName, jobXml);
		} catch (Exception e) {
			e.printStackTrace();
		}
		params.put("abc", "abc");
		jenkinsClient.jobOps().triggerJob(jobPath, params);
	}

	@Test
	public void getBuildQueueReferenceUrl() throws URISyntaxException, IOException, Exception {
		String jenkinsId = JenkinsClient.createClient(jenkinsUrl, username, password);
		JenkinsClient jenkinsClient = JenkinsClient.getClient(jenkinsId);
		jenkinsClient.jobOps().createFolder(folderPath);
		try {
			jenkinsClient.jobOps().createJob(folderPath, jobName, jobXml);
		} catch (Exception e) {
			e.printStackTrace();
		}
		params.put("abc", "abc");
		String queueReferenceUrl = jenkinsClient.jobOps().triggerJob(jobPath, params);
		while (jenkinsClient.queueOps().isInQueue(queueReferenceUrl)) {
			Thread.sleep(1000);
		}
		jenkinsClient.queueOps().getBuildNumber(queueReferenceUrl);
	}

	@Test
	public void getBuildUsingBuildNumber() throws URISyntaxException, IOException, Exception {
		String jenkinsId = JenkinsClient.createClient(jenkinsUrl, username, password);
		JenkinsClient jenkinsClient = JenkinsClient.getClient(jenkinsId);
		jenkinsClient.jobOps().createFolder(folderPath);
		try {
			jenkinsClient.jobOps().createJob(folderPath, jobName, jobXml);
		} catch (Exception e) {
			e.printStackTrace();
		}
		params.put("abc", "abc");
		String queueReferenceUrl = jenkinsClient.jobOps().triggerJob(jobPath, params);
		while (jenkinsClient.queueOps().isInQueue(queueReferenceUrl)) {
			Thread.sleep(1000);
		}
		jenkinsClient.queueOps().getBuildNumber(queueReferenceUrl);
	}

	@Test
	public void stopQueuedBuild() throws URISyntaxException, IOException, Exception {
		String jenkinsId = JenkinsClient.createClient(jenkinsUrl, username, password);
		JenkinsClient jenkinsClient = JenkinsClient.getClient(jenkinsId);
		jenkinsClient.jobOps().createFolder(folderPath);
		try {
			jenkinsClient.jobOps().createJob(folderPath, jobName, jobXml);
		} catch (Exception e) {
			e.printStackTrace();
		}

		String queueReferenceUrl = "";

		params.put("abc", "abc");
		int idleExecutors = jenkinsClient.getIdleExecutors(labelName);
		for (int i = 0; i <= idleExecutors + 1; i++) {
			queueReferenceUrl = jenkinsClient.jobOps().triggerJob(jobPath, params);
		}
		Thread.sleep(5000);
		jenkinsClient.queueOps().stopQueuedBuild(queueReferenceUrl);
	}

	@Test
	public void stopRunningBuild() throws URISyntaxException, IOException, Exception {
		String jenkinsId = JenkinsClient.createClient(jenkinsUrl, username, password);
		JenkinsClient jenkinsClient = JenkinsClient.getClient(jenkinsId);
		jenkinsClient.jobOps().createFolder(folderPath);
		try {
			jenkinsClient.jobOps().createJob(folderPath, jobName, jobXml);
		} catch (Exception e) {
			e.printStackTrace();
		}
		params.put("abc", "abc");
		String queueReferenceUrl = jenkinsClient.jobOps().triggerJob(jobPath, params);
		while (jenkinsClient.queueOps().isInQueue(queueReferenceUrl)) {
			Thread.sleep(1000);
		}
		int buildNumber = jenkinsClient.queueOps().getBuildNumber(queueReferenceUrl);
		Thread.sleep(10000);
		jenkinsClient.buildOps().stopRunningBuild(jobPath, buildNumber);
	}

	@After
	public void pause() throws InterruptedException {
		Thread.sleep(2000);
	}

	@After
	@AfterClass
	public void last() throws URISyntaxException, IOException, Exception {
		for (String[] jenkins : jenkins) {
			try {
				String jenkinsId = JenkinsClient.createClient(jenkins[0], jenkins[1], jenkins[2]);
				JenkinsClient jenkinsClient = JenkinsClient.getClient(jenkinsId);
				jenkinsClient.jobOps().deleteFolderOrJob("/" + folderPath.split("/")[1]);
			} catch (Exception e) {
			}
		}
	}

	// Run as Java Application

	public void run() {
		try {
			triggerJob();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws URISyntaxException, IOException, Exception {
		for (int i = 1; i <= 20; i++) {
			Thread ob = new Thread(new JenkinsClientApiTest(jenkins[0][0], jenkins[0][1], jenkins[0][2]));
			ob.start();
		}
	}
}