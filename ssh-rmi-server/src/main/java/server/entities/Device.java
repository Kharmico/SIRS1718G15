package server.entities;

public class Device {
	private String Name;
	private String Status;
	private String Type;
	
	public Device(String n, String s, String t){
		Name = n;
		Status = s;
		Type = t;
	}

	public String getName() {
		return Name;
	}

	public void setName(String name) {
		Name = name;
	}

	public String getStatus() {
		return Status;
	}

	public void setStatus(String status) {
		Status = status;
	}

	public String getType() {
		return Type;
	}

	public void setType(String type) {
		Type = type;
	}
}
