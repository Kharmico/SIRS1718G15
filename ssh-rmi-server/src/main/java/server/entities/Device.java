package server.entities;

import java.util.ArrayList;
import java.util.Arrays;

import server.Controllers.Helper;

public class Device {
	private String Name;
	private String Status;
	private String Type;
	private ArrayList<String> Commands;
	private Boolean accepted;
	private Helper h;

	public Device(String n, String s, String t){
		Name = n;
		Status = s;
		Type = t;
		Commands = new ArrayList<String>();
		accepted = false;
		
	}
	public Device(String n, String s, String t, Helper h){
		Name = n;
		Status = s;
		Type = t;
		Commands = new ArrayList<String>();
		//Commands.add("GETSTATUS");
		Commands.add("RENEWSESSIONKEY");
		Commands.add("SWITCHSTATE");
		Commands.add("SETSTATE");
		accepted = false;
		this.h = h;
		
	}
	public String getName() {
		return Name;
	}

	public void setName(String name) {
		Name = name;
	}

	public String getStatus() {
		if(h!= null){
			Status = h.getDeviceState();
		}
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
	
	public void sendCommand(String com){
		if(com.equals("SWITCHSTATE")){
			h.switchDeviceState();
		}
		else if (com.equals("RENEW")){
			h.renewSessionKey();
		}
		
		else if (com.contains("SETSTATE")){
			h.setCustomState(com.split(" ",2)[1]);
		}
		
	}
	
	public ArrayList<String> getInfo() {
		getStatus();
		return new ArrayList<String>(Arrays.asList(Name, Status, Type));
	}
}
