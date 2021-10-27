
package chameleon.HomoLib.HomoAdd;

/*
 * The Paillier Alg. Implementation
 * adapted from a previous implementation by Omar Hasan 
 * (omar.hasan@insa-lyon.fr)
 * available from: 
 * http://liris.cnrs.fr/~ohasan/pprs/paillierdemo/Paillier.java

 * Rev 17/2/2017
 */

import chameleon.HomoLib.HomoUtils.HelpSerial;

import java.math.BigInteger;
import java.util.Random;


public class HomoAdd
{
    private static final int CERTAINTY = 64;
    // certainty value with which primes are generated: 1-2^(-CERTAINTY)
    private static int modLength = 2048;
    // length in bits of the modulus n

    public static PaillierKey generateKey()
    {
        BigInteger p = new BigInteger(modLength / 2, CERTAINTY, new Random());         // returning the big p random prime
        BigInteger q;
        do
        {
            q = new BigInteger(modLength / 2, CERTAINTY, new Random());
            // a random prime (distinct from p)
        }
        while (q.compareTo(p) == 0);

        // lambda = lcm(p-1, q-1) = (p-1)*(q-1)/gcd(p-1, q-1)
        BigInteger lambda = 
	    (p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE)).divide(p.subtract(BigInteger.ONE).gcd(q.subtract(BigInteger.ONE))));
        
        BigInteger n = p.multiply(q);       // n = p*q
        BigInteger nsquare = n.multiply(n); // nsquare = n*n
        BigInteger g;
        do
        {
            // generate g, a random integer in Z*_{n^2}
            g = randomZStarNSquare(nsquare);
        }
        // verify g, the following must hold: 
        // gcd(L(g^lambda mod n^2), n) = 1, where L(u) = (u-1)/n

        while (g.modPow(lambda, nsquare).subtract(BigInteger.ONE).divide(n).gcd(n).intValue() != 1);
        
        // mu = (L(g^lambda mod n^2))^{-1} mod n, where L(u) = (u-1)/n
        BigInteger mu = g.modPow(lambda, nsquare).subtract(BigInteger.ONE).divide(n).modInverse(n);
        return new PaillierKey(p, q, lambda, n, nsquare, g, mu);
    }
    
	public static PaillierKey keyFromString(String str){
		return (PaillierKey) HelpSerial.fromString(str);
	}
	
	public static String stringFromKey(PaillierKey key){
		return HelpSerial.toString(key);
	}
    
        /* Function encrypt below .... */
	public static String encrypt(String chave, String valor) {
		try {
			PaillierKey paillierKey = HomoAdd.keyFromString(chave);
			String nsquare = paillierKey.getNsquare().toString();
			return encrypt(new BigInteger(valor), keyFromString(chave)).toString()+":"+nsquare;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
 
        /* Function decrypt below .... */
	public static String decrypt(String chave, String valor) {
		//System.out.println("HomoAdd - valor: "+valor);
		String[] campos = valor.split(":");
		
		try {
			return decrypt(new BigInteger(campos[0]), keyFromString(chave)).toString();
		} catch (Exception e) {
			System.out.println("Valor: "+valor);
			System.out.println("Chave: "+chave);
			e.printStackTrace();
			return null;
		}
	}

    /* ---------------------------------------------------
     * Core encryption and decryption functions
     * ---------------------------------------------------
     */    
 
     /* 
     * Encryption Function
     */
 
    public static BigInteger encrypt(BigInteger m, PaillierKey pk) throws Exception
    {
    	BigInteger n = pk.getN();
    	BigInteger nsquare = pk.getNsquare();
    	BigInteger g = pk.getG();
        // if m is not in Z_n
        if (m.compareTo(BigInteger.ZERO) < 0 || m.compareTo(n) >= 0)
        {
            throw new Exception("Paillier.encrypt(BigInteger m): plaintext m is not in Z_n");
        }
        
        // generate r, a random integer in Z*_n
        BigInteger r = randomZStarN(n);
        
        // c = g^m * r^n mod n^2
        return (g.modPow(m, nsquare).multiply(r.modPow(n, nsquare))).mod(nsquare);
    }

     /* 
     * Decryption Function
     */

    public static BigInteger decrypt(BigInteger c, PaillierKey pk) throws Exception
    {
    	BigInteger nsquare = pk.getNsquare();
    	BigInteger lambda = pk.getLambda();
    	BigInteger n = pk.getN();
    	BigInteger mu = pk.getMu();
        // if c is not in Z*_{n^2}
        if (c.compareTo(BigInteger.ZERO) < 0 || c.compareTo(nsquare) >= 0 || c.gcd(nsquare).intValue() != 1)
        {
            throw new Exception("Paillier.decrypt(BigInteger c): ciphertext c is not in Z*_{n^2}");
        }
        
        // m = L(c^lambda mod n^2) * mu mod n, where L(u) = (u-1)/n
        return c.modPow(lambda, nsquare).subtract(BigInteger.ONE).divide(n).multiply(mu).mod(n);
    }
    
    public static BigInteger sum(BigInteger a, BigInteger b, BigInteger nsquare) {
    	return a.multiply(b).mod(nsquare);
    }
 
    public static BigInteger dif(BigInteger a, BigInteger b, BigInteger nsquare) {
		BigInteger bMinus = b.modPow(new BigInteger("-1"), nsquare);	
    	return a.multiply(bMinus).mod(nsquare);
    }
    
    public static BigInteger mult(BigInteger a, int prod, BigInteger nsquare){
    	return a.modPow(new BigInteger(String.valueOf(prod)), nsquare);
    }

    // return a random integer in Z*_n
    private static BigInteger randomZStarN(BigInteger n)
    {
        BigInteger r;
        
        do
        {
            r = new BigInteger(modLength, new Random());
        }
        while (r.compareTo(n) >= 0 || r.gcd(n).intValue() != 1);
        
        return r;
    }
    
    // return a random integer in Z*_{n^2}
    private static BigInteger randomZStarNSquare(BigInteger nsquare)
    {
        BigInteger r;
        
        do
        {
            r = new BigInteger(modLength * 2, new Random());
        }
        while (r.compareTo(nsquare) >= 0 || r.gcd(nsquare).intValue() != 1);
        
        return r;
    }
    

}
