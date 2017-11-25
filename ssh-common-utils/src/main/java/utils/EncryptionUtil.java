package utils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import java.io.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.Date;
import sun.security.x509.*;

public class EncryptionUtil {

    /*
    Name of the encryption algorithm
    */
    private static final String ALGORITHM = "RSA";
    private static final String CIPHER = "RSA/ECB/PKCS1Padding";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
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

    public byte[] base64Decoder(byte[] toDecode){
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