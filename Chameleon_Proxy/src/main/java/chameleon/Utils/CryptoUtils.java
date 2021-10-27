package chameleon.Utils;

import chameleon.Exceptions.Exceptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Base64;

@Component
public class CryptoUtils {

    @Value("${server.ssl.key-store-password}")
    private String pwdStatic;
    @Value("${chameleon.key.nature}")
    private String signatureKey;
    @Value("${chameleon.signature.instance}")
    private String signatureInstance;
    @Value("${server.ssl.key-store-type}")
    private String keyStoreType;
    @Value("${chameleon.hash.instance}")
    private String hashInstance;

    private static PublicKey key;
    private static PrivateKey pKey;
    private static final Base64.Encoder base64Enconder = Base64.getUrlEncoder();
    private static final Base64.Decoder base64Decoder = Base64.getUrlDecoder();


    @PostConstruct
    public void init() {

        KeyStore ks;
        try {

            ks = KeyStore.getInstance(keyStoreType);

            ks.load(new ClassPathResource("Keys/chameleon.p12").getInputStream(), pwdStatic.toCharArray());

            key = ks.getCertificate(signatureKey).getPublicKey();
            pKey = (PrivateKey) ks.getKey(signatureKey, pwdStatic.toCharArray());


        } catch (IOException | CertificateException | NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException e) {
            e.printStackTrace();
        }
    }

    public String hashValue(String toHash) {

        try {
            MessageDigest digest = MessageDigest.getInstance(hashInstance);
            byte[] encodedHash = digest.digest(toHash.getBytes(StandardCharsets.UTF_8));

            return bytesToHex(encodedHash);

        } catch (NoSuchAlgorithmException e) {
            throw new Exceptions.ElasticException("Error performing Hash Function.");
        }

    }

    public String doIntegrityCheck(String message) {
        try {

            Signature signature = Signature.getInstance(signatureInstance);
            signature.initSign(pKey);
            signature.update(message.getBytes(StandardCharsets.UTF_8));

            byte[] digitalSignature = signature.sign();

            return base64Enconder.encodeToString(digitalSignature);

        } catch (SignatureException | InvalidKeyException | NoSuchAlgorithmException e) {

            throw new Exceptions.ElasticException("Error performing Integrity Check.");
        }

    }

    public boolean compareIntegrityCheck(String message, String digitalSignature) {
        try {

            Signature signature = Signature.getInstance(signatureInstance);

            signature.initVerify(key);

            signature.update(message.getBytes(StandardCharsets.UTF_8));

            return signature.verify(base64Decoder.decode(digitalSignature));

        } catch (SignatureException | InvalidKeyException | NoSuchAlgorithmException e) {

            throw new Exceptions.ElasticException("Error performing Integrity Check.");
        }

    }

    private static String bytesToHex(byte[] hash) {

        StringBuilder hexString = new StringBuilder(2 * hash.length);

        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1)
                hexString.append('0');

            hexString.append(hex);
        }
        return hexString.toString();
    }
}
