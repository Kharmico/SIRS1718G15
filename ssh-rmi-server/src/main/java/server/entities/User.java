package server.entities;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

import utils.EncryptionUtil;

public class User {
	
	private EncryptionUtil encUtils;
	private String username;
	private String password;
	private String type;
	private String loginUuid;
	private Date loginDate;
	
	public User(String username, String password, String type, Key pubKey){
		this.username = username;
		this.password = password;
		this.type = type;
		this.encUtils = new EncryptionUtil();
		encUtils.setPublicKey(pubKey, username+"User");
	}
	
	public String getUsername() {
		return username;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getLoginUuid() {
		return loginUuid;
	}

	public void setLoginUuid(String loginUuid) {
		this.loginUuid = loginUuid;
	}

	public Date getLoginDate() {
		return loginDate;
	}

	public void setLoginDate(Date loginDate) {
		this.loginDate = loginDate;
	}

	public EncryptionUtil getEncUtils() {
		return encUtils;
	}
	
	public String generateToken(){
		Date now = new Date();
		this.loginDate = now;
		this.loginUuid = UUID.randomUUID().toString();
		String token = this.username + this.type + this.loginUuid + this.loginDate.toString();
		return token;
	}
	
	public String lastToken(){
		String token = this.username + this.type + this.loginUuid + this.loginDate.toString();
		return token;
	}
}