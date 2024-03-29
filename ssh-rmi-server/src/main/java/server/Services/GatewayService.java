package server.Services;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

public interface GatewayService extends Remote {
	//Admin Requests
	public List<byte[]> RegisterUser(byte[] adminUsername, byte[] adminPassword, byte[] name, byte[] password, byte[] authCode, byte[] nonce, byte[] signature, byte[] token) throws RemoteException;
	
	public List<byte[]> DeleteUser(byte[] adminUsername, byte[] adminPassword, byte[] name, byte[] nonce, byte[] signature, byte[] token) throws RemoteException;

	public List<byte[]> AcceptDevice(byte[] deviceName, byte[] code, byte[] nonce, byte[] signature, byte[] token) throws RemoteException;
	
	public List<byte[]> RemoveDevice(byte[] deviceName, byte[] nonce, byte[] signature, byte[] token) throws RemoteException;
	
	public List<ArrayList<byte[]>> GetUsers(byte[] nonce, byte[] signature, byte[] token) throws RemoteException;
	
	//General Requests
	public List<ArrayList<byte[]>> GetDeviceStatus(byte[] nonce, byte[] signature, byte[] token) throws RemoteException;
	
	public List<byte[]> GetDeviceCommands(byte[] deviceName, byte[] nonce, byte[] signature, byte[] token) throws RemoteException;
	
	public List<byte[]> SendCommand(byte[] deviceName, byte[] command, byte[] nonce, byte[] signature, byte[] token) throws RemoteException;
	
	public List<byte[]> ReplenishLogin(byte[] userPublicKey, byte[] username, byte[] password, byte[] authString, byte[] nonce, byte[] signature) throws RemoteException;
	
	public List<byte[]> Login(byte[] username, byte[] password, byte[] nonce, byte[] signature) throws RemoteException;
	
	public byte[] GetPublicKey() throws RemoteException;
	
}