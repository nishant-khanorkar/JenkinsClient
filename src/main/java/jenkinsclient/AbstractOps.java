package jenkinsclient;

public abstract class AbstractOps {
	protected JenkinsClient client;

	public AbstractOps(JenkinsClient client) {
		super();
		this.client = client;
	}

	protected String generateUrl(String[] folders) {
		StringBuilder folderUrl = new StringBuilder("/");
		for (String folder : folders) {
			if (folder.isEmpty())
				continue;
			folderUrl.append("job/").append(folder).append("/");
		}
		return folderUrl.toString();
	}
}
