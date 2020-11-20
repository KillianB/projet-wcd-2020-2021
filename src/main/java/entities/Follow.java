package entities;

public class Follow {
	private String user;
	private String target;

	public Follow() {
	}

	public Follow(String user, String target) {
		this.user = user;
		this.target = target;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}
}
