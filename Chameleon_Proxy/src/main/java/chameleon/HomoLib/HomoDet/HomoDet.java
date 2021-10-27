/**
 * HomoDet for Linear Serach
 * Rev. 17/2/2017
 */

package chameleon.HomoLib.HomoDet;


import chameleon.HomoLib.HomoRand.HomoRand;
import chameleon.HomoLib.HomoUtils.HelpSerial;

import javax.crypto.SecretKey;
import java.io.UnsupportedEncodingException;
import java.util.Base64;

public class HomoDet {

	/**
	 * Generate an constant Initial Vector
	 * 
	 * @return
	 */
	private static byte[] generateIV(){
		try {
			return "0123456789ABCDEF".getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}
	public static SecretKey generateKey() {
		return HomoRand.generateKey();
	}
	
	public static SecretKey keyFromString(String str){
		return (SecretKey) HelpSerial.fromString(str);
	}
	
	public static String stringFromKey(SecretKey secretKey){
		return HelpSerial.toString(secretKey);
	}
	
    public static String encrypt(SecretKey key, String value) {
    	try {
			byte[] aux = encrypt(key, value.getBytes("UTF-8"));
			String str = Base64.getUrlEncoder().encodeToString(aux);
			return str;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}    	
    	return null; 
    }

    public static String decrypt(SecretKey key, String encrypted) {   	
    	try {

        	byte[] newValue = Base64.getUrlDecoder().decode(encrypted.getBytes("US-ASCII"));
			return new String(decrypt(key, newValue), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
    }
    
    public static byte[] encrypt(SecretKey key, byte[] value) {
    		byte[] firstEncryption = HomoRand.encrypt(key, generateIV(), value);
    		byte temp;
    		int size = firstEncryption.length;
    		for (int i = 0; i < size/2; i++)
    		  {
    		     temp = firstEncryption[i];
    		     firstEncryption[i] = firstEncryption[size-1 - i];
    		     firstEncryption[size-1 - i] = temp;
    		  }
    		return HomoRand.encrypt(key, generateIV(), firstEncryption);

    }

    public static byte[] decrypt(SecretKey key, byte[] encrypted) {   	

    	byte[] firstEncryption =  HomoRand.decrypt(key, generateIV(), encrypted);
		byte temp;
		int size = firstEncryption.length;
		for (int i = 0; i < size/2; i++)
		  {
		     temp = firstEncryption[i];
		     firstEncryption[i] = firstEncryption[size-1 - i];
		     firstEncryption[size-1 - i] = temp;
		  }
		return HomoRand.decrypt(key, generateIV(), firstEncryption);
    }
    
    public static boolean compare(String op1, String op2){
    	return op1.equals(op2);
    }
    
    public static boolean compare(byte[] op1, byte[] op2) throws UnsupportedEncodingException {
    	
    	return(compare(new String(op1, "UTF-8"),new String(op2, "UTF-8") ));
    }
    
}


