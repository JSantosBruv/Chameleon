/*
 * Homomorphic deterministic encryption for linear search
 * based on SHA512 digest as default
 * Rev. 17/2/2017
 *
 */

package chameleon.HomoLib.HomoSearch;

import chameleon.HomoLib.HomoDet.HomoDet;
import chameleon.HomoLib.HomoRand.HomoRand;
import chameleon.HomoLib.HomoUtils.HelpSerial;

import javax.crypto.SecretKey;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

public class HomoSearchSHA512 {
	
	public static byte[] wordDigest(SecretKey chave, String texto) {

		try {
			byte[] cifra = HomoDet.encrypt(chave, texto.getBytes("UTF-8"));

			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(cifra);
			return md.digest();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static String wordDigest64(SecretKey chave, String texto){
		byte[] digest = wordDigest(chave, texto);
		return Base64.getUrlEncoder().encodeToString(digest);
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
	
	public static String encrypt(SecretKey chave, String texto, String splitter){
		//System.out.println("Encrypt Text");
		String cifra = "";
		//System.out.println("Generate IV");
		byte[] iv = HomoRand.generateIV();
		//System.out.println("Base64 encode");
		cifra = Base64.getUrlEncoder().encodeToString(iv);
		cifra += ":";
		//System.out.println("Rand Encrypt");
		cifra += HomoRand.encrypt(chave, iv, texto);
		//System.out.println("End Rand Encrypt");
		texto = texto.replace("\n", "").replace("\r", "");
		String[] palavras = texto.split(splitter);
		SortedSet<String> mySet = new TreeSet<String>();
		Collections.addAll(mySet, palavras);
		java.util.Iterator<String> itr = mySet.iterator();
	    while(itr.hasNext()) {
	          String element = itr.next();
	          //System.out.println("Call word digest "+element);
	          cifra += ":"+wordDigest64(chave, element);
	    }
		return cifra;
	}
	
	public static String encrypt(SecretKey chave, String texto){
		return encrypt(chave, texto, " ");
	}
	
	public static String encrypt(String chaveStr, String texto){
		SecretKey chave = keyFromString(chaveStr);
		return encrypt(chave, texto);
	}
	
	public static String decrypt(String chaveStr, String texto){
		SecretKey chave = keyFromString(chaveStr);
		return decrypt(chave, texto);
	}
	
	public static String decrypt(SecretKey chave, String texto){
		String[] campos = texto.split(":");
		byte[] iv = Base64.getUrlDecoder().decode(campos[0]);
		String cifrado = new String(campos[1]);
		return HomoRand.decrypt(chave, iv, cifrado);
		
		
	}
	
	public static boolean pesquisa(String palavra, String texto) {
		String[] splitted = texto.split(":");
		
		for (int i = 2; i < splitted.length; i++) {
			//System.out.println("Compara "+palavra+" com "+splitted[i]);
			if(palavra.equals(splitted[i])) return true;
		}
		
		return false;
		
	}
	
	
	public static boolean searchAll(String subTexto, String texto) {
		String[] splitted = subTexto.split(":");
		
		for (int i = 2; i < splitted.length; i++) {

			if(!pesquisa(splitted[i], texto)) return false;
		}
		
		return true;
		
	}
	

}
