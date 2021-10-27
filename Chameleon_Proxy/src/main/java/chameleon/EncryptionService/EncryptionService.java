package chameleon.EncryptionService;

import chameleon.Exceptions.Exceptions;
import chameleon.HomoLib.HomoAdd.HomoAdd;
import chameleon.HomoLib.HomoDet.HomoDet;
import chameleon.HomoLib.HomoMult.HomoMult;
import chameleon.HomoLib.HomoOPE.HomoOpeInt;
import chameleon.HomoLib.HomoRand.HomoRand;
import chameleon.HomoLib.HomoSearch.HomoSearch;
import chameleon.HomoLib.HomoUtils.HelpSerial;
import chameleon.Models.*;
import chameleon.Utils.CryptoUtils;
import chameleon.Utils.JSONMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;
import java.util.regex.Pattern;

@Component
@SuppressWarnings("unchecked")
public class EncryptionService {

    private final CryptoUtils cryptoUtils;
    private static final Pattern EMPTY_SPACES = Pattern.compile("\\s");

    public EncryptionService(CryptoUtils cryptoUtils) {
        this.cryptoUtils = cryptoUtils;
    }

    public String encryptDet(String key, String element) {

        return HomoDet.encrypt(HomoRand.keyFromString(key), element);
    }

    public String decryptDet(String key, String element) {

        return HomoDet.decrypt(HomoRand.keyFromString(key), element);
    }

    public String encryptRand(String key, String element) {

        byte[] iv = HomoRand.generateIV();

        return HelpSerial.toString(iv) + ":" + HomoRand.encrypt(HomoRand.keyFromString(key), iv, element);
    }

    public String decryptRand(String key, String element) {

        String[] ivCipher = element.split(":");

        byte[] iv = (byte[]) HelpSerial.fromString(ivCipher[0]);

        return HomoRand.decrypt(HomoRand.keyFromString(key), iv, ivCipher[1]);
    }

    public long encryptOPE(long key, int element) {

        HomoOpeInt opeInt = new HomoOpeInt(key);
        return opeInt.encrypt(element);
    }

    public int decryptOPE(long key, long element) {

        HomoOpeInt opeInt = new HomoOpeInt(key);

        return opeInt.decrypt(element);
    }

    public String encryptSearch(String key, String element, boolean scramble) {

        return HomoSearch.encrypt(HomoRand.keyFromString(key), element, scramble);
    }

    private String decryptSearch(String key, String element) {

        return HomoSearch.decrypt(HomoRand.keyFromString(key), element);
    }

    public BigInteger encryptMult(String key, String element) {

        return HomoMult.encrypt((RSAPublicKey) HomoMult.keyFromString(key).getPublic(), new BigInteger(element));
    }

    private long decryptMult(String key, BigInteger element) {

        return HomoMult.decrypt((RSAPrivateKey) HomoMult.keyFromString(key).getPrivate(), element).longValue();
    }

    public BigInteger encryptAdd(String key, String element) {


        try {

            return new BigInteger(HomoAdd.encrypt(key, element).split("[:]")[0]);

        } catch (Exception e) {
            throw new Exceptions.ElasticException("Something went wrong");
        }

    }

    public long decryptAdd(String key, BigInteger element) {

        return new BigInteger(HomoAdd.decrypt(key, element.toString()).split("[:]")[0]).longValue();
    }


    public String encryptDecryptJson(StructureSchema schema, String json, boolean encrypt)
            throws JsonProcessingException {

        Map<String, Object> map = (Map<String, Object>) JSONMapper.readJSON(json, Map.class);

        String checkSum = "";

        if (!encrypt) {

            checkSum = (String) map.remove("checkSum");

            if (checkSum == null)
                throw new Exceptions.ElasticException("Message has been tampered with");
        }

        Map<String, Object> newMap = analyzeMap(schema, map, encrypt, "");


        if (encrypt) {

            String stripJson = EMPTY_SPACES.matcher(json.replace("\"", "")).replaceAll("");

            checkSum = cryptoUtils.doIntegrityCheck(stripJson);

            newMap.put("checkSum", checkSum);

            return JSONMapper.writeValueAsString(newMap);

        } else {

            String decryptedJSON = JSONMapper.writeValueAsString(newMap);

            String stripJson = EMPTY_SPACES.matcher(decryptedJSON.replace("\"", "")).replaceAll("");

            boolean notTampered = cryptoUtils.compareIntegrityCheck(stripJson, checkSum);

            if (!notTampered) {
                throw new Exceptions.ElasticException("Message has been tampered with");
            }

            return decryptedJSON;
        }


    }


    private Map<String, Object> analyzeMap(StructureSchema schema, Map<String, Object> map, boolean encrypt,
                                           String previousKey) {

        Map<String, Object> newMap = new LinkedHashMap<>(map.size());

        map.forEach((k, v) -> {


            String field = k;
            Object value = v;

            if (!encrypt) {


                if (value instanceof List && !(((List<?>) value).get(0) instanceof Map) &&
                        !(((List<?>) value).get(0) instanceof List)) {

                    field = field.substring(field.indexOf(":") + 1);
                } else if (!(value instanceof Map)) {
                    field = field.substring(field.indexOf(":") + 1);
                }

                field = previousKey + schema.getHashes().get(field);

            } else
                field = previousKey + k;

            String preField = field.replaceFirst(previousKey, "");

            if (!newMap.containsKey(preField)) {

                value = inspectStructure(value, field, schema, encrypt);


                if (encrypt) {

                    field = cryptoUtils.hashValue(schema.getIndex() + preField);

                    if (value instanceof LeafWrapper) {

                        List<Object> leafValues = ((LeafWrapper) value).getEncryptedValues();

                        for (Object leafValue : leafValues) {

                            String alg = "";

                            if (leafValue instanceof LeafSingleValue) {

                                alg = ((LeafSingleValue) leafValue).getAlg() + ":";

                                if (alg.equals("NONE:"))
                                    newMap.put(preField, ((LeafSingleValue) leafValue).getEncryptedValue());
                                else
                                    newMap.put(alg + field, ((LeafSingleValue) leafValue).getEncryptedValue());

                            } else if (leafValue instanceof LeafArrayValue) {

                                alg = ((LeafArrayValue) leafValue).getAlg() + ":";
                                if (alg.equals("NONE:"))
                                    newMap.put(preField, ((LeafArrayValue) leafValue).getEncryptedValues());
                                else
                                    newMap.put(alg + field, ((LeafArrayValue) leafValue).getEncryptedValues());
                            }
                        }

                    } else
                        newMap.put(field, value);

                } else {

                    newMap.put(preField, value);
                }
            }
        });

        return newMap;
    }


    private Object inspectStructure(Object value, String field, StructureSchema schema, boolean encrypt) {

        if (value instanceof Map)

            value = analyzeMap(schema, (Map<String, Object>) value, encrypt, field + ".");

        else if (value instanceof Set || value instanceof List) {

            List<List<Object>> encryptedArrays = new ArrayList<>();

            Set<String> algs = schema.getSchema().get(field);

            int nrArrays = 1;

            if (algs != null)
                nrArrays = algs.size();

            for (int i = 0; i < nrArrays; i++) {
                encryptedArrays.add(new ArrayList<>());
            }

            boolean isSingleValue = false;

            for (Object el : (List<Object>) value) {

                Object result = inspectStructure(el, field, schema, encrypt);

                if (result instanceof LeafWrapper) {

                    isSingleValue = true;
                    List<Object> leafValues = ((LeafWrapper) result).getEncryptedValues();

                    for (int i = 0; i < leafValues.size(); i++) {
                        encryptedArrays.get(i).add(((LeafSingleValue) leafValues.get(i)).getEncryptedValue());
                    }

                } else {
                    encryptedArrays.get(0).add(result);
                }

            }

            if (isSingleValue) {
                LeafWrapper arraysWrapper = new LeafWrapper();
                List<Object> leafArrays = new ArrayList<>(encryptedArrays.size());

                assert algs != null;
                Object[] algsArray = algs.toArray();

                for (int i = 0; i < encryptedArrays.size(); i++)
                    leafArrays.add(new LeafArrayValue((String) algsArray[i], encryptedArrays.get(i)));

                arraysWrapper.setEncryptedValues(leafArrays);

                return arraysWrapper;
            } else
                return encryptedArrays.get(0);
        } else {

            Set<String> algs = schema.getSchema().get(field);

            if (algs != null) {

                if (encrypt) {

                    List<Object> encryptedValues = new ArrayList<>(algs.size());

                    for (String alg : algs) {
                        encryptedValues.add(new LeafSingleValue(alg, encryptValue(schema.getKeys(), alg, value)));

                    }

                    return new LeafWrapper(encryptedValues);

                } else
                    return decryptValue(schema.getKeys(), (String) algs.toArray()[0], value);
            }

        }
        return value;
    }

    public Object encryptValue(KeyValues keys, String algName, Object value) {


        if (algName.matches(StructureSchema.Operations.OPE.name() + "|" + StructureSchema.Operations.MULT.name() + "|" +
                                    StructureSchema.Operations.ADDITION.name()))
            try {

                Integer.valueOf(String.valueOf(value));
            } catch (NumberFormatException e) {
                throw new Exceptions.UnsupportedOperationsException(
                        "Value provided [" + value + "] must be an " + "Integer,");
            }
        else if (algName
                .matches(StructureSchema.Operations.MULT.name() + "|" + StructureSchema.Operations.ADDITION.name()))
            try {

                Long.valueOf(String.valueOf(value));
            } catch (NumberFormatException e) {
                throw new Exceptions.UnsupportedOperationsException(
                        "Value provided [" + value + "] must be in long's" + " value range" + ".");
            }

        switch (StructureSchema.Operations.valueOf(algName)) {
            case ADDITION:
                return encryptAdd(keys.getAdd(), String.valueOf(value));
            case SUBTRACTION:
                return null;
            case MULT:
                return encryptMult(keys.getMult(), String.valueOf(value));
            case DET:
                return encryptDet(keys.getDet(), String.valueOf(value));
            case RAND:
                return encryptRand(keys.getRand(), String.valueOf(value));
            case OPE:
                return encryptOPE(keys.getOpe(), Integer.parseInt(String.valueOf(value)));
            case SEARCH:
                return encryptSearch(keys.getSearch(), String.valueOf(value), true);
            case SEARCH_STATIC:
                return encryptSearch(keys.getSearch(), String.valueOf(value), false);
            default:
                return value;
        }
    }

    public Object decryptValue(KeyValues keys, String alg, Object value) {

        switch (StructureSchema.Operations.valueOf(alg)) {
            case ADDITION:
                return decryptAdd(keys.getAdd(), new BigInteger(String.valueOf(value)));
            case SUBTRACTION:
                return null;
            case MULT:
                return decryptMult(keys.getMult(), new BigInteger(String.valueOf(value)));
            case DET:
                return decryptDet(keys.getDet(), String.valueOf(value));
            case RAND:
                return decryptRand(keys.getRand(), String.valueOf(value));
            case OPE:
                return decryptOPE(keys.getOpe(), Long.parseLong(String.valueOf(value)));
            case SEARCH:
                return decryptSearch(keys.getSearch(), String.valueOf(value));
            default:
                return value;
        }
    }
}
