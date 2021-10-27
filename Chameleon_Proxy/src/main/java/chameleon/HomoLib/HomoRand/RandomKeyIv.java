
/* 
 * RandomKeyIV generation
 * rev. 17/2/2017
 */

package chameleon.HomoLib.HomoRand;

import javax.crypto.SecretKey;

public class RandomKeyIv implements java.io.Serializable{
	private static final long serialVersionUID = 1L;
	private SecretKey key;
	private byte[] iV;
	public RandomKeyIv(){
		this.key = HomoRand.generateKey();
		this.iV = HomoRand.generateIV();
	}
	public SecretKey getKey() {
		return key;
	}
	public byte[] getiV() {
		return iV;
	}
	

}
