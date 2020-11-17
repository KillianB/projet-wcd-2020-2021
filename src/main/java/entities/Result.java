package entities;

public class Result {
	private int code;
	private Object object;

	public Result(int code, Object object) {
		this.code = code;
		this.object = object;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}
}
