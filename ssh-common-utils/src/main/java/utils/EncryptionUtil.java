package utils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import sun.security.x509.*;
import java.security.SecureRandom;



public class EncryptionUtil {

    /*
    Name of the encryption algorithm
    */
    private static final String ALGORITHM = "RSA";
    private static final String CIPHER = "RSA/ECB/PKCS1Padding";
    private static final String HASH_ALGORITHM = "SHA-1";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    private static final int KEY_SIZE = 1024;

    ObjectInputStream inputStream = null;

    //Name of the private key file
    private String privateKeyPath = null;

    //Name of the public key file
    private String publicKeyPath = null;
    
    private String secretKeyPath = "keys/secretKey.key";

    public EncryptionUtil(){}

    public EncryptionUtil(String publicKeyPath, String privateKeyPath){
        this.publicKeyPath = publicKeyPath;
        this.privateKeyPath = privateKeyPath;
    }

    public void setKeyPaths(String publicKeyPath, String privateKeyPath){
        this.publicKeyPath = publicKeyPath;
        this.privateKeyPath = privateKeyPath;
    }
    
    public PrivateKey getPrivateKey() {

        PrivateKey privateKey = null;

        try {
            inputStream = new ObjectInputStream(new FileInputStream(privateKeyPath));
            privateKey = (PrivateKey) inputStream.readObject();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.err.println("Class was not found.");
            e.printStackTrace();
        }

        return privateKey;
    }

    public PublicKey getPublicKey() {

        PublicKey publicKey = null;

        try {
            inputStream = new ObjectInputStream(new FileInputStream(publicKeyPath));
            publicKey = (PublicKey) inputStream.readObject();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.err.println("Class was not found.");
            e.printStackTrace();
        }

        return publicKey;
    }
    
    public void setPrivateKey(Key key, String name) {
    	File fileKey = new File("keys/"+name+"PrivateKey.key");
    	
    	if(fileKey.getParentFile() != null){
    		fileKey.getParentFile().mkdirs();
        }

    	try {
			fileKey.createNewFile();

			ObjectOutputStream fileKeyOutputStream = new ObjectOutputStream(new FileOutputStream(fileKey));
			fileKeyOutputStream.writeObject(key);
			fileKeyOutputStream.close();
            
            privateKeyPath = fileKey.getPath();
			
    	} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
       
    public void setPublicKey(Key key, String name) {
    	File fileKey = new File("keys/"+name+"PublicKey.key");
    	
    	if(fileKey.getParentFile() != null){
    		fileKey.getParentFile().mkdirs();
        }

    	try {
			fileKey.createNewFile();

			ObjectOutputStream fileKeyOutputStream = new ObjectOutputStream(new FileOutputStream(fileKey));
			fileKeyOutputStream.writeObject(key);
			fileKeyOutputStream.close();
            
            publicKeyPath = fileKey.getPath();
			
    	} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public SecretKey getSecretKey(){
        SecretKey secretKey = null;
 
        try{
            inputStream = new ObjectInputStream(new FileInputStream(secretKeyPath));
            secretKey = (SecretKey) inputStream.readObject();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.err.println("Class was not found.");
            e.printStackTrace();
        }
 
        return secretKey;
    }

    public void generateKeys(String name){
        try{

            final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
            final SecureRandom random = SecureRandom.getInstanceStrong();
            keyPairGenerator.initialize(KEY_SIZE,random);
            final KeyPair key = keyPairGenerator.generateKeyPair();

            File privateKey = new File("keys/"+name+"PrivateKey.key");
            File publicKey = new File("keys/"+name+"PublicKey.key");

            //Creates files to store public and private key of the Library
            if(privateKey.getParentFile() != null){
                privateKey.getParentFile().mkdirs();
            }

            privateKey.createNewFile();

            if(publicKey.getParentFile() != null){
                publicKey.getParentFile().mkdirs();
            }

            publicKey.createNewFile();

            //Saving the Public Key in a file - Library
            ObjectOutputStream publicKeyOutputStream = new ObjectOutputStream(new FileOutputStream(publicKey));
            publicKeyOutputStream.writeObject(key.getPublic());
            publicKeyOutputStream.close();

            //Saving the Private Key in a file - Library
            ObjectOutputStream privateKeyOutputStream = new ObjectOutputStream(new FileOutputStream(privateKey));
            privateKeyOutputStream.writeObject(key.getPrivate());
            privateKeyOutputStream.close();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("There is no such Algorithm.");
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            System.err.println("The file was not found.");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        privateKeyPath = "keys/"+name+"PrivateKey.key";
        publicKeyPath = "keys/"+name+"PublicKey.key";
    }
    
    public void generateSecret(){
    	 
        File secretKeyFile = new File("keys/secretKey.key");
        KeyGenerator keyGenerator = null;
        try {
            keyGenerator = KeyGenerator.getInstance("HmacSHA256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        SecretKey secretKey = keyGenerator.generateKey();
 
        try{
            if(secretKeyFile.getParentFile() != null){
                secretKeyFile.getParentFile().mkdirs();
            }
 
            secretKeyFile.createNewFile();
 
            ObjectOutputStream secretKeyOutputStream = new ObjectOutputStream(new FileOutputStream(secretKeyFile));
            secretKeyOutputStream.writeObject(secretKey);
            secretKeyOutputStream.close();
 
        } catch (IOException e) {
            e.printStackTrace();
        }
 
    }
    
    public byte[] generateMac(byte[] message) {
    	 
        byte[] response = null;
 
        byte[] encodedResponse = null;
        try {
            Mac authenticator = Mac.getInstance(getSecretKey().getAlgorithm());
            authenticator.init(getSecretKey());
            response = authenticator.doFinal(message);
            encodedResponse = base64Encoder(response);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
 
        return encodedResponse;
    }

    public byte[] pubKeyToByteArray(){
        byte[] publicKeyBytes;

        publicKeyBytes = getPublicKey().getEncoded();

        return publicKeyBytes;
    }

    public byte[] privKeyToByteArray() {
        byte[] privKeyBytes;

        privKeyBytes = getPrivateKey().getEncoded();

        return privKeyBytes;
    }

    public Key byteArrayToPrivKey(byte[] privateKey){
        PrivateKey privKey = null;

        try {
            privKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKey));
        } catch (InvalidKeySpecException e) {
            System.err.println("The key is invalid.");
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("There is no such Algorithm.");
            e.printStackTrace();
        }

        return privKey;
    }

    public Key byteArrayToPubKey(byte[] publicKey){

        PublicKey pubKey = null;
        try {
            pubKey = KeyFactory.getInstance(ALGORITHM).generatePublic(new X509EncodedKeySpec(publicKey));
        } catch (InvalidKeySpecException e) {
            System.err.println("The key is invalid.");
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("There is no such Algorithm.");
            e.printStackTrace();
        }

        return pubKey;
    }
    
    public byte[] hash(byte[] textToHash){
        MessageDigest messageDigest = null;
        byte[] hash = null;

        try{
            messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
            messageDigest.update(textToHash);
            hash = messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("There's no such Hash Algorithm.");
            e.printStackTrace();
        }

        return hash;
    }

	public String calculateHMAC(String data, String key)
		throws SignatureException, NoSuchAlgorithmException, InvalidKeyException
	{
		SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA1_ALGORITHM);
		Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
		mac.init(signingKey);
		return BufferUtil.toHexString(mac.doFinal(data.getBytes()));
	}
	
	public String decryptAES(String key, byte[] initVector, String encrypted) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector);
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/NOPADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

            byte[] original = cipher.doFinal(base64SDecoder(encrypted));
            
            original = BufferUtil.removePad(original);
            
            return new String(original);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }
	
	
	public String decryptAES(byte[] key, byte[] initVector, String encrypted) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector);
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/NOPADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

            byte[] original = cipher.doFinal(base64SDecoder(encrypted));
            
            original = BufferUtil.removePad(original);

            return new String(original);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }
	
	public String decryptAESwithPadding(byte[] key, byte[] initVector, String encrypted) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector);
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/NOPADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

            byte[] original = cipher.doFinal(base64SDecoder(encrypted));

            return new String(original);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }
	public byte[] encryptAESwithPadding(byte[] key, byte[] initVector, String encrypted) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector);
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/NOPADDING");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

            byte[] paddedEnc = base64SDecoder(encrypted);
            byte[] message = BufferUtil.pad(paddedEnc, paddedEnc.length %16);
            
            byte[] original = cipher.doFinal(message);

            return original;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }
	
	public String calculateHMAC(byte[] data, byte[] key)
			throws SignatureException, NoSuchAlgorithmException, InvalidKeyException
		{
			SecretKeySpec signingKey = new SecretKeySpec(key, HMAC_SHA1_ALGORITHM);
			Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
			mac.init(signingKey);
			return BufferUtil.toHexString(mac.doFinal(data));
		}
	
	public byte[] calculateHMACb(byte[] data, byte[] key)
			throws SignatureException, NoSuchAlgorithmException, InvalidKeyException
		{
			SecretKeySpec signingKey = new SecretKeySpec(key, HMAC_SHA1_ALGORITHM);
			Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
			mac.init(signingKey);
			return mac.doFinal(data);
		}
	
	public byte[] toSHA1(byte[] convertme) {
	    MessageDigest md = null;
	    try {
	        md = MessageDigest.getInstance("SHA-1");
	    }
	    catch(NoSuchAlgorithmException e) {
	        e.printStackTrace();
	    } 
	    return (md.digest(convertme));
	}
	
	public byte[] secureRandom(int nrBytes){
	    SecureRandom random = new SecureRandom();
	    byte bytes[] = new byte[nrBytes];
	    random.nextBytes(bytes);
	    return bytes;
	}
	
    public byte[] generateSignature(byte[] dataToBeSigned) throws SignatureException {
        Signature rsaForSign = null;

        try{
            rsaForSign = Signature.getInstance(SIGNATURE_ALGORITHM);
            rsaForSign.initSign(getPrivateKey());
            rsaForSign.update(dataToBeSigned);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("There's no such algorithm.");
            e.printStackTrace();
        } catch (SignatureException e) {
            System.err.println("Exception with the Signature");
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            System.err.println("Key is invalid.");
            e.printStackTrace();
        }

        return rsaForSign.sign();
    }

    public boolean verifySignature(byte[] data, byte[] signature) throws SignatureException {
        Signature rsaForVerify = null;

        try{
            rsaForVerify = Signature.getInstance(SIGNATURE_ALGORITHM);
            rsaForVerify.initVerify(getPublicKey());
            rsaForVerify.update(data);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            System.err.println("There is no Signature.");
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            System.err.println("The key is not valid.");
            e.printStackTrace();
        }

        return rsaForVerify.verify(signature);
    }

    public byte[] base64Encoder(byte[] toEncode){
        byte[] response = Base64.getEncoder().encode(toEncode);

        return response;
    }
    
    public String base64SEncoder(byte[] toEncode){
        String response = Base64.getEncoder().encodeToString(toEncode);

        return response;
    }

    public byte[] base64Decoder(byte[] toDecode){
        byte[] response = Base64.getDecoder().decode(toDecode);

        return response;
    }
    

    public byte[] base64SDecoder(String toDecode){
        byte[] response = Base64.getDecoder().decode(toDecode);

        return response;
    }

    public byte[] encrypt(byte[] toEncrypt){
        byte[] cipheredData;
        byte[] encodedCipheredData = null;

        try{
            final Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, getPublicKey());
            cipheredData = cipher.doFinal(toEncrypt);
            encodedCipheredData = base64Encoder(cipheredData);
        } catch (NoSuchPaddingException e) {
            System.err.println("There is no such padding.");
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("There is no such Algorithm.");
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            System.err.println("The key is invalid.");
            e.printStackTrace();
        } catch (BadPaddingException e) {
            System.err.println("Bad padding.");
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            System.err.println("Block size not accepted.");
            e.printStackTrace();
        }

        return encodedCipheredData;
    }

    public byte[] decrypt(byte[] toDecrypt){
        byte[] decodedCipherText = base64Decoder(toDecrypt);
        byte[] decipheredData = null;

        try{
            final Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE,getPrivateKey());
            decipheredData = cipher.doFinal(decodedCipherText);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            System.err.println("The key is invalid.");
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            System.err.println("There is no such padding.");
            e.printStackTrace();
        } catch (BadPaddingException e) {
            System.err.println("Bad padding.");
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            System.err.println("Block size not accepted.");
            e.printStackTrace();
        }

        return decipheredData;
    }
    
}