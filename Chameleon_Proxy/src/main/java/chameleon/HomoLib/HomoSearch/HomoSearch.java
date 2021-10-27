/*
 * Homomorphic deterministic encryption for linear search
 * based on SHA256 digest as default
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

public class HomoSearch {

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

    public static String wordDigest64(SecretKey chave, String texto) {
        byte[] digest = wordDigest(chave, texto);
        return Base64.getUrlEncoder().encodeToString(digest);
    }

    public static SecretKey generateKey() {
        return HomoRand.generateKey();
    }

    public static SecretKey keyFromString(String str) {
        return (SecretKey) HelpSerial.fromString(str);
    }

    public static String stringFromKey(SecretKey secretKey) {
        return HelpSerial.toString(secretKey);
    }

    public static String encrypt(SecretKey chave, String texto, String splitter, boolean scramble) {
       // System.out.println("Encrypt Text");
        StringBuilder cifra = new StringBuilder();
        //System.out.println("Generate IV");
        byte[] iv = HomoRand.generateIV();
        //System.out.println("Base64 encode");
        cifra = new StringBuilder(Base64.getUrlEncoder().encodeToString(iv));
        cifra.append(":");
        //System.out.println("Rand Encrypt");
        cifra.append(HomoRand.encrypt(chave, iv, texto));
        //System.out.println("End Rand Encrypt");
        texto = texto.replace("\n", "").replace("\r", "");
        String[] palavras = texto.split(splitter);

        //System.out.println("Scramble?");
        if(scramble) {
          //  System.out.println("Scramble1");
            SortedSet<String> mySet = new TreeSet<>();
            Collections.addAll(mySet, palavras);
            //System.out.println("Scramble2");
            for(String word:mySet)
                cifra.append(" ").append(wordDigest64(chave, word));
            //System.out.println("Scramble3");
        }else

            for(String palavra: palavras)
                cifra.append(" ").append(wordDigest64(chave, palavra));

        return cifra.toString();
    }

    public static String encrypt(SecretKey chave, String texto, boolean scramble) {
        return encrypt(chave, texto, " ", scramble);
    }

    public static String encrypt(String chaveStr, String texto, boolean scramble) {
        SecretKey chave = keyFromString(chaveStr);
        return encrypt(chave, texto, scramble);
    }

    public static String decrypt(String chaveStr, String texto) {
        SecretKey chave = keyFromString(chaveStr);
        return decrypt(chave, texto);
    }

    public static String decrypt(SecretKey chave, String texto) {
        String[] campos = texto.split(":");

        byte[] iv = Base64.getUrlDecoder().decode(campos[0]);

        String cifrado = new String(campos[1].split(" ")[0]);
        return HomoRand.decrypt(chave, iv, cifrado);


    }

    public static boolean pesquisa(String palavra, String texto) {
        String[] splitted = texto.split(":");

        for (int i = 2; i < splitted.length; i++) {
            //System.out.println("Compara "+palavra+" com "+splitted[i]);
            if (palavra.equals(splitted[i]))
                return true;
        }

        return false;

    }


    public static boolean searchAll(String subTexto, String texto) {
        String[] splitted = subTexto.split(":");

        for (int i = 2; i < splitted.length; i++) {

            if (!pesquisa(splitted[i], texto))
                return false;
        }

        return true;

    }


}
