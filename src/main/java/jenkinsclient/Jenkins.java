package jenkinsclient;

public class Jenkins implements Comparable<Jenkins> {
	private String url;
	private String username;
	private String password;

	private String jenkinsId;

	Jenkins(String url, String username, String password) {
		this.url = url;
		this.username = username;
		this.password = password;
	}

	public String getUrl() {
		return url;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getJenkinsId() {
		return jenkinsId;
	}

	public void setJenkinsId(String jenkinsId) {
		this.jenkinsId = jenkinsId;
	}

	public int compareTo(Jenkins o) {
		int ur = url.compareTo(o.getUrl());
		int us = (username == null ? "" : username).compareTo(o.getUsername() == null ? "" : o.getUsername());
		int pa = (password == null ? "" : password).compareTo(o.getPassword() == null ? "" : o.getPassword());

		if (ur > 0)
			return 1;
		else if (ur < 0)
			return -1;
		else {
			if (us > 0)
				return 1;
			else if (us < 0)
				return -1;
			else {
				if (pa > 0)
					return 1;
				else if (pa < 0)
					return -1;
				else
					return 0;
			}
		}
	}

}
