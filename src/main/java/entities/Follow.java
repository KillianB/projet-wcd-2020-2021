package entities;

public class Follow {
	private User user;
	private User target;

	public Follow() {
	}

	public Follow(User user, User target) {
		this.user = user;
		this.target = target;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public User getTarget() {
		return target;
	}

	public void setTarget(User target) {
		this.target = target;
	}
}
