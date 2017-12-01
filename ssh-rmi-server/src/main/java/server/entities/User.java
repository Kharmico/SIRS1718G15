package server.entities;

import java.security.Key;
import java.security.PublicKey;
import java.util.UUID;
import org.joda.time.DateTime;
import utils.EncryptionUtil;

public class User {
	
	private EncryptionUtil encUtils;
	private String username;
	private String password;
	private String type;
	private String loginUuid;
	private DateTime loginDate;
	private PublicKey pubKey;
	private String authCode;

	public User(String username, String password, String type){
		this.username = username;
		this.setPassword(password);
		this.type = type;
		this.encUtils = new EncryptionUtil();
		this.pubKey = null;
	}
	
	public User(String username, String password, String type, Key pubKey){
		this.username = username;
		this.setPassword(password);
		this.type = type;
		this.encUtils = new EncryptionUtil();
		encUtils.setPublicKey(pubKey, username+"User");
		this.pubKey = this.encUtils.getPublicKey();
	}
	
	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
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

	public DateTime getLoginDate() {
		return loginDate;
	}

	public void setLoginDate(DateTime loginDate) {
		this.loginDate = loginDate;
	}

	public EncryptionUtil getEncUtils() {
		return encUtils;
	}
	
	public String getAuthCode() {
		return authCode;
	}
	
	public String generateToken(){
		DateTime now = new DateTime().now();
		this.loginDate = now;
		this.loginUuid = UUID.randomUUID().toString();
		String token = this.username + this.type + this.loginUuid + this.loginDate.toString();
		return token;
	}
	
	public String lastToken(){
		String token = this.username + this.type + this.loginUuid + this.loginDate.toString();
		return token;
	}
	
	public PublicKey getPubKey(){
		if(this.pubKey == null)
			this.pubKey = this.encUtils.getPublicKey();
		return this.pubKey;
	}
}