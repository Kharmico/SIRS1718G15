package server.entities;

import java.util.ArrayList;
import java.util.Arrays;

public class Device {
	private String Name;
	private String Status;
	private String Type;
	private ArrayList<String> Commands;
	private Boolean accepted;

	public Device(String n, String s, String t){
		Name = n;
		Status = s;
		Type = t;
		Commands = new ArrayList<String>();
		accepted = false;
		
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

	public ArrayList<String> getCommands() {
		return Commands;
	}

	public Boolean getAccepted() {
		return accepted;
	}

	public void setAccepted(Boolean accepted) {
		this.accepted = accepted;
	}
	public void setCommands(ArrayList<String> commands) {
		Commands = commands;
	}
	
	public ArrayList<String> getInfo() {
		return new ArrayList<String>(Arrays.asList(Name, Status, Type));
	}
}
