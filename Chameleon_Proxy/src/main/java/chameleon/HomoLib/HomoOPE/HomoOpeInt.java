/* Order Preserved Encryption 
 * for Integers
 * Rev. 17/2/2017
 */

package chameleon.HomoLib.HomoOPE;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class HomoOpeInt {

	long chave;
	Map<Long, Long> dGapCache;
	//MessageDigest md;
	
	public boolean compare(long long1, long long2){
		return (long1 > long2);
	}
	
	public static long generateKey(){
		
		Random random = new Random();
		return Math.abs(random.nextLong() / 2);
	}
	
	public long generateKey(String frase){
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return -1;
		}
		md.update(frase.getBytes());
		byte[] digest = md.digest();
		//System.out.println("Numero de bytes: "+digest.length);
		long numero1 = digest[0];
		long numero2 = digest[8];
		for(int i = 1; i < 7; i++){
			numero1 = numero1* 256 + digest[i];
			numero2 = numero2* 256 + digest[i +8];
		}
		return numero1 + numero2;
	}
	
	public HomoOpeInt(String frase) {
		this.chave = generateKey(frase);
		this.dGapCache = new HashMap<Long, Long>();
	}
	
	public HomoOpeInt() {
		this.chave = generateKey();
		this.dGapCache = new HashMap<Long, Long>();
	}
	
	public HomoOpeInt(long chave) {
		this.chave = chave;
		this.dGapCache = new HashMap<Long, Long>();
	}
	
	OpeDomainRange lazySample(long dLow, long dHigh, long rLow, long rHigh, long valor, boolean cifra){
		
	long numDomain = dHigh-dLow +1;
	long numRange = rHigh - rLow + 1;
        if (numDomain == 1)
           return new OpeDomainRange(dLow, rLow, rHigh);
       	Random r = new Random(chave);
        // semeia o random sempre da mesma maneira a partir da chave
	long rGap = numRange/2;
	long dGap;
	Long ci = dGapCache.get(new Long(rLow+rGap));
	if(ci == null){
	   dGap = (long) hgd(numRange/2, numDomain, numRange-numDomain, r);
           dGapCache.put(rLow + rGap, dGap);
	   } else dGap = ci.longValue();
	boolean goLow = true;
	if (cifra && valor >= dLow + dGap) goLow = false;
	if (!cifra && valor >= rLow + rGap) goLow = false;
        if (goLow)
	   return lazySample(dLow, dLow + dGap - 1, rLow, rLow + rGap - 1, valor, cifra);
	   else
	   return lazySample(dLow + dGap, dHigh, rLow + rGap, rHigh, valor, cifra);
		
	}
	

	
	public long encrypt(long plano) {
		long cifra = 0;
		long plano2 = (long) plano;
		plano2 -= Integer.MIN_VALUE; // transforma negativos em positivos
		long numDomain = Integer.MAX_VALUE;
		numDomain++;
		numDomain = 2* numDomain;
	    OpeDomainRange dr = lazySample((long) 0, numDomain, (long) 0, Long.MAX_VALUE/2,  plano2, true);
	    //System.out.println("rLow "+dr.rLow+" rHigh "+dr.rHigh);
	    long rRange = dr.rHigh-dr.rLow+1;
	    rRange = (long) Math.random()*rRange;
	    cifra = rRange + dr.rLow;
		return cifra;
	}
	
	public int decrypt(long cifra) {
		long numDomain = Integer.MAX_VALUE;
		numDomain++;
		numDomain = 2* numDomain;		
	    OpeDomainRange dr = lazySample((long) 0, numDomain, (long) 0, Long.MAX_VALUE/2, cifra, false);
	    long retorno = dr.d;
	    retorno += Integer.MIN_VALUE;
		return (int) retorno;
	}
	
	
	    
	
	// Fun��es adaptadas de https://github.com/masih/sina/blob/master/src/main/java/DistLib/hypergeometric.java
	// Creditos Masih H. Derkani
	
    private long ks = -1;
	private long n1s = -1;
	private long n2s = -1;
    static private double con = 57.56462733;
    static private double deltal = 0.0078;
    static private double deltau = 0.0034;
    static private double scale = 1e25;
     private double a;
     private double d, e, f, g;
     private long i, k, m;
     private double p;
     private double r, s, t;
     private double u, v, w;
     private double lamdl, y, lamdr;
     private long minjx, maxjx, n1, n2;
     private double p1, p2, p3, y1, de, dg;
     private boolean setup1, setup2;
     private double gl, kl, ub, nk, dr, nm, gu, kr, ds, dt;
     private long ix;
     private double tn;
     private double xl;
     private double ym, yn, yk, xm;
     private double xr;
     private double xn;
     private boolean reject;
     private double xk;
     private double alv;
	
	public double hgd(long kk, long nn1, long nn2,  Random rand) {


		//System.out.println(nn1+" "+nn2+" "+kk);
        if (nn1 < 0 || nn2 < 0 || kk < 0 || kk > nn1 + nn2) { throw new ArithmeticException("Math Error: DOMAIN");
        //  	return Double.NaN;
        }
        /* if new parameter values, initialize */

        reject = true;
        setup1 = false;
        setup2 = false;
        if (nn1 != n1s || nn2 != n2s) {
            setup1 = true;
            setup2 = true;
        }
        else if (kk != ks) {
            setup2 = true;
        }
        if (setup1) {
            n1s = nn1;
            n2s = nn2;
            tn = nn1 + nn2;
            if (nn1 <= nn2) {
                n1 = nn1;
                n2 = nn2;
            }
            else {
                n1 = nn2;
                n2 = nn1;
            }
        }
        if (setup2) {
            ks = kk;
            if (kk + kk >= tn) {
                k = (long) (tn) - kk;
            }
            else {
                k = kk;
            }
        }
        if (setup1 || setup2) {
            //System.out.println("n1 "+n1+" n2 "+n2+" k "+k);
            m = (long) ((k + 1.0) * (n1 + 1.0) / (tn + 2.0));
            /*!* 	minjx = imax2(0, k - n2); *!*/
            minjx = Math.max(0, k - n2);
            /*!* 	maxjx = Math.min(n1, k); *!*/
            maxjx = Math.min(n1, k);
            //System.out.println("m "+m+" minjx "+minjx+" maxjx "+maxjx);
        }
        /* generate random variate */

        if (minjx == maxjx) {
            /* degenerate distribution */
        	//System.out.println("Degenerate");
            ix = maxjx;
            /* return ix;
               No, need to unmangle <TSL>*/
            /* return appropriate variate */

            if (kk + kk >= tn) {
                if (nn1 > nn2) {
                    ix = kk - nn2 + ix;
                }
                else {
                    ix = nn1 - ix;
                }
            }
            else {
                if (nn1 > nn2) ix = kk - ix;
            }
            return ix;

        }
        else if (m - minjx < 10) {
        	//System.out.println("Inverse");
            /* inverse transformation */
            if (setup1 || setup2) {
                if (k < n2) {
                    /*!* 		w = exp(con + afc(n2) + afc(n1 + n2 - k) *!*/
                    w = Math.exp(con + afc(n2) + afc(n1 + n2 - k) - afc(n2 - k) - afc(n1 + n2));
                }
                else {
                    /*!* 		w = exp(con + afc(n1) + afc(k) *!*/
                    w = Math.exp(con + afc(n1) + afc(k) - afc(k - n2) - afc(n1 + n2));
                }
            }
            L10: while (true) {
                p = w;
                ix = minjx;
                u = rand.nextDouble() * scale;
                L20: while (true) {
                    if (u > p) {
                        u = u - p;
                        p = p * (n1 - ix) * (k - ix);
                        ix = ix + 1;
                        p = p / ix / (n2 - k + ix);
                        if (ix > maxjx) continue L10;
                        continue L20;
                    }
                    break L10;
                }
            }
        }
        else {
            /* h2pe */
        	//System.out.println("H2PE");
            if (setup1 || setup2) {
                /*!* 	    s = sqrt((tn - k) * k * n1 * n2 / (tn - 1) / tn / tn); *!*/
            	double aux;
            	aux = (tn - k)/(tn-1);
            	aux = aux*n2/tn;
            	aux = aux*k/tn;
            	aux = aux*n1;
                s = Math.sqrt(aux);
                //System.out.println("H2PE, o s �: "+s);
                /* remark: d is defined in reference without int. */
                /* the truncation centers the cell boundaries at 0.5 */

                d = (int) (1.5 * s) + .5;
                xl = m - d + .5;
                xr = m + d + .5;
                //System.out.println("H2PE, o m � "+m+" o d � "+d+" o xl �: "+xl+" xr �: "+xr);
                a = afc(m) + afc(n1 - m) + afc(k - m) + afc(n2 - k + m);
                /*!* 	    kl = exp(a - afc((int) (xl)) - afc((int) (n1 - xl)) *!*/
                double conta = a - afc((long) (xl)) - afc((long) (n1 - xl)) - afc((long) (k - xl)) - afc((long) (n2 - k + xl));
                //System.out.println("Conta: "+conta);
                if(conta > 5) conta = 5;//limita��o da exponencial
                kl = Math.exp(conta);
                /*!* 	    kr = exp(a - afc((int) (xr - 1)) *!*/
                conta = a - afc((long) (xr - 1)) - afc((long) (n1 - xr + 1)) - afc((long) (k - xr + 1)) - afc((long) (n2 - k + xr - 1));
                //System.out.println("Conta: "+conta);
                if(conta > 5) conta = 5;//limita��o da exponencial
                kr = Math.exp(conta);
                /*!* 	    lamdl = -log(xl * (n2 - k + xl) / (n1 - xl + 1) *!*/
                lamdl = -Math.log(xl * (n2 - k + xl) / (n1 - xl + 1) / (k - xl + 1));
                /*!* 	    lamdr = -log((n1 - xr + 1) * (k - xr + 1) *!*/
                lamdr = -Math.log((n1 - xr + 1) * (k - xr + 1) / xr / (n2 - k + xr));
                p1 = d + d;
                p2 = p1 + kl / lamdl;
                p3 = p2 + kr / lamdr;
                //System.out.println("a "+a+" kl "+kl+" kr "+kr+" lamdl "+lamdl+" lamdr "+lamdr);
                //System.out.println("O p1 e "+p1+" o p2 e "+p2+" o p3 e "+p3);
            }
            //System.out.println("H2PE, cheguei ao while, p3 "+p3+" p2 "+p2+" kl "+kl+" kr "+kr);
            L30: while (true) {
                u = rand.nextDouble() * p3;
                v = rand.nextDouble();
                if (u < p1) {
                    /* rectangular region */
                    ix = (long) (xl + u);
                }
                else if (u <= p2) {
                    /* left tail */
                    /*!* 	    ix = xl + log(v) / lamdl; *!*/
                    ix = (long) (xl + Math.log(v) / lamdl);
                    if (ix < minjx) continue L30;
                    //System.out.println("v "+v+" u "+u+" p1 "+p1+" lamdl "+lamdl);
                    v = v * (u - p1) * lamdl;
                }
                else {
                    /* right tail */
                    /*!* 	    ix = xr - log(v) / lamdr; *!*/
                    ix = (long) (xr - Math.log(v) / lamdr);
                    if (ix > maxjx) continue L30;
                    //System.out.println("v "+v+" u "+u+" p2 "+p2+" lamdr "+lamdr);
                    v = v * (u - p2) * lamdr;
                }

                /* acceptance/rejection test */

                if (m < 100 || ix <= 50) {
                    /* explicit evaluation */
                    f = 1.0;
                    if (m < ix) {
                        for (i = m + 1; i <= ix; i++)
                            f = f * (n1 - i + 1) * (k - i + 1) / (n2 - k + i) / i;
                    }
                    else if (m > ix) {
                        for (i = ix + 1; i <= m; i++)
                            f = f * i * (n2 - k + i) / (n1 - i) / (k - i);
                    }
                    if (v <= f) {
                        //System.out.println("H2PE, vim ao reject1");
                        reject = false;
                    }
                }
                else {
                    /* squeeze using upper and lower bounds */
                    y = ix;
                    y1 = y + 1.0;
                    ym = y - m;
                    yn = n1 - y + 1.0;
                    yk = k - y + 1.0;
                    nk = n2 - k + y1;
                    r = -ym / y1;
                    s = ym / yn;
                    t = ym / yk;
                    e = -ym / nk;
                    g = yn * yk / (y1 * nk) - 1.0;
                    dg = 1.0;
                    if (g < 0.0) dg = 1.0 + g;
                    gu = g * (1.0 + g * (-0.5 + g / 3.0));
                    gl = gu - .25 * (g * g * g * g) / dg;
                    xm = m + 0.5;
                    xn = n1 - m + 0.5;
                    xk = k - m + 0.5;
                    nm = n2 - k + xm;
                    ub = y * gu - m * gl + deltau + xm * r * (1. + r * (-0.5 + r / 3.0)) + xn * s * (1. + s * (-0.5 + s / 3.0)) + xk * t * (1. + t * (-0.5 + t / 3.0)) + nm * e * (1. + e * (-0.5 + e / 3.0));
                    /* test against upper bound */
                    /*!* 	    alv = log(v); *!*/
                    //System.out.println("v = "+v);
                    alv = Math.log(v);
                    //System.out.println("Alv = "+alv);
                    if (alv > ub) {
                        reject = true;
                    }
                    else {
                        /* test against lower bound */
                        dr = xm * (r * r * r * r);
                        if (r < 0.0) dr = dr / (1.0 + r);
                        ds = xn * (s * s * s * s);
                        if (s < 0.0) ds = ds / (1.0 + s);
                        dt = xk * (t * t * t * t);
                        if (t < 0.0) dt = dt / (1.0 + t);
                        de = nm * (e * e * e * e);
                        if (e < 0.0) de = de / (1.0 + e);
                        if (alv < ub - 0.25 * (dr + ds + dt + de) + (y + m) * (gl - gu) - deltal) {
                        	//System.out.println("H2PE, vim ao reject2");
                            reject = false;
                        }
                        else {
                            /*
                             * stirling's formula to machine
                             * accuracy
                             */
                            if (alv <= (a - afc(ix) - afc(n1 - ix) - afc(k - ix) - afc(n2 - k + ix))) {
                            	//System.out.println("H2PE, vim ao reject3");
                                reject = false;
                            }
                            else {
                                reject = true;
                            }
                        }
                    }
                }
                if (reject) continue L30;
                break L30;
            }
        }
        /* return appropriate variate */

        if (kk + kk >= tn) {
            if (nn1 > nn2) {
                ix = kk - nn2 + ix;
            }
            else {
                ix = nn1 - ix;
            }
        }
        else {
            if (nn1 > nn2) ix = kk - ix;
        }
		//System.out.println("kk "+kk+" nn1 "+nn1+" nn2 "+nn2+" ix "+ix);
        return ix;
	}

	double afc(double i) {

		double di, value;
		if (i < 0) {
			System.out.println("rhyper.c: afc(i)+ i=%d < 0 -- SHOULD NOT HAPPEN!\n" + i);
			return -1;/* unreached (Wall) */
		}
		else if (i <= 7) {
			value = al[(int) i + 1];
		}
		else {
			di = i;
			/*!* 	value = (di + 0.5) * log(di) - di + 0.08333333333333 / di *!*/
			//value = (di + 0.5) * java.lang.Math.log(di) - di + 0.08333333333333 / di - 0.00277777777777 / di / di / di + 0.9189385332;
			value = (di + 0.5) * Math.log(di) - di + 0.399089934;

		}
		//System.out.println("Entrada: "+i+" Sa�da: "+value);	
		return value;
	}

	static private double al[] = {0.0, 0.0,/*ln(0!)=ln(1)*/
			0.0,/*ln(1!)=ln(1)*/
			0.69314718055994530941723212145817,/*ln(2) */
			1.79175946922805500081247735838070,/*ln(6) */
			3.17805383034794561964694160129705,/*ln(24)*/
			4.78749174278204599424770093452324, 6.57925121201010099506017829290394, 8.52516136106541430016553103634712
			/*, 10.60460290274525022841722740072165*/
	};

}

