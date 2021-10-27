/**
 * Utilities for serializable objects
 * using Base 64 stringified reresentations
 * Rev. 17/2/2017
 */

package chameleon.HomoLib.HomoUtils;

import java.io.*;
import java.util.Base64;

public class HelpSerial {

    /**
     * Read the object from Base64 string.
     */
    public static Object fromString(String s) {
        byte[] data = Base64.getUrlDecoder().decode(s);
        ObjectInputStream ois;
        try {
            ois = new ObjectInputStream(new ByteArrayInputStream(data));
            Object o = ois.readObject();
            ois.close();
            return o;
        } catch (IOException | ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;

    }

    /**
     * Write the object to a Base64 string.
     */
    public static String toString(Serializable o) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(baos);
            oos.writeObject(o);
            oos.close();
            return Base64.getUrlEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;

    }

}