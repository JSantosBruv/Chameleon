/* Homomorphic Multiplication 
 * supported by El Gammal Alg. variant
/* Rev. 17/2/2017
*/

package chameleon.HomoLib.HomoMult;

import chameleon.HomoLib.HomoUtils.HelpSerial;

import javax.crypto.Cipher;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPublicKey;

public class HomoMultElGammal {

	public static final String ALGORITHM = "EGM";		
	
	 public static KeyPair generateKey() {
			try {
	                  KeyPairGenerator keyGen = 
			      KeyPairGenerator.getInstance(ALGORITHM);
			  keyGen.initialize(1024);
			      return keyGen.generateKeyPair();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
				return null;
			}		      
	}
	 
		public static KeyPair keyFromString(String str){
			return (KeyPair) HelpSerial.fromString(str);
		}
		
		public static String stringFromKey(KeyPair key){
			return HelpSerial.toString(key);
		}
		    
	public static BigInteger encrypt(RSAKey key, BigInteger value) {
		try {
			Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
			//Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, (Key) key);
			byte[] encrypted = cipher.doFinal(value.toByteArray());
			return new BigInteger(1, encrypted);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return null;
	}
	
	public static String encrypt( String strKey, String strValue){
		BigInteger value = new BigInteger(strValue);
		KeyPair key = keyFromString(strKey);
		return encrypt((RSAPublicKey) key.getPublic(), value).toString()+":"+HelpSerial.toString(key.getPublic());
	}
	
	public static String decrypt( String strKey, String strValue){
		String[] campos = strValue.split(":");
		BigInteger value = new BigInteger(campos[0]);
		KeyPair key = keyFromString(strKey);
		return decrypt((RSAKey) key.getPrivate(), value).toString();
	}

	public static BigInteger decrypt(RSAKey key, BigInteger encrypted) {
		byte[] auxByteArray = encrypted.toByteArray();
		byte[] byteArray = new byte[128];
		int i = 0;
		if(auxByteArray.length > 128) i = 1;
		for(int j = 0; j < 128; j++){
			byteArray[j] = auxByteArray[j+i];
		}
		try {
			Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
			//Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, (Key) key);
			byte[] original = cipher.doFinal(byteArray);
			return new BigInteger(original);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static BigInteger multiply(BigInteger op1, BigInteger op2, RSAPublicKey publicKey) {
		BigInteger modulo = publicKey.getModulus();
		BigInteger produto = op1.multiply(op2); // cifrado*cifrado
		produto = produto.mod(modulo);
		return produto;
		
	}
}

