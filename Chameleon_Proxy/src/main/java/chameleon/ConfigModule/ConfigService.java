package chameleon.ConfigModule;

import chameleon.EncryptionService.ScriptsAndMappings;
import chameleon.Exceptions.Exceptions;
import chameleon.HomoLib.HomoAdd.HomoAdd;
import chameleon.HomoLib.HomoDet.HomoDet;
import chameleon.HomoLib.HomoMult.HomoMult;
import chameleon.HomoLib.HomoOPE.HomoOpeInt;
import chameleon.HomoLib.HomoRand.HomoRand;
import chameleon.HomoLib.HomoSearch.HomoSearch;
import chameleon.HomoLib.HomoUtils.HelpSerial;
import chameleon.Models.KeyValues;
import chameleon.Models.StructureSchema;
import chameleon.Utils.*;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConfigService {

    public static final String CONFIG_INDEX = "logsconfig";
    public static final String CONFIG_CREATE_PATH = "/" + CONFIG_INDEX + "/_doc/%s";
    public static final String CONFIG_SOURCE_PATH = "/" + CONFIG_INDEX + "/_source/%s";

    public static final String SCRIPT_PATH = "_scripts/%s";

    private final CryptoUtils cryptoUtils;
    private final RequestBuilder requestBuilder;
    private final Map<String, StructureSchema> schemas;

    public ConfigService(RequestBuilder requestBuilder, CryptoUtils cryptoUtils) {
        this.requestBuilder = requestBuilder;
        this.cryptoUtils = cryptoUtils;
        this.schemas = new ConcurrentHashMap<>();
    }


    public Response configStructureMappings(String auth, StructureSchema schema) {


        generateHashMappings(schema);
        generateAndStoreKeys(schema);

        createIndexNumericMappings(auth, schema);
        createStoredScripts(auth);

        Request request = new Request(Constants.POST, String.format(CONFIG_CREATE_PATH, schema.getIndex()));

        return requestBuilder.performRequest(request, schema, auth);

    }

    private void createStoredScripts(String auth) {

        Request request0 = new Request(Constants.POST, String.format(SCRIPT_PATH, ScriptsAndMappings.INIT_NAME));
        Request request1 = new Request(Constants.POST, String.format(SCRIPT_PATH, ScriptsAndMappings.MAP_NAME));
        Request request2 = new Request(Constants.POST, String.format(SCRIPT_PATH, ScriptsAndMappings.COMBINE_MIN_NAME));

        Request request3 = new Request(Constants.POST, String.format(SCRIPT_PATH, ScriptsAndMappings.COMBINE_SUM_NAME));
        Request request4 = new Request(Constants.POST, String.format(SCRIPT_PATH, ScriptsAndMappings.COMBINE_AVG_NAME));
        Request request5 = new Request(Constants.POST, String.format(SCRIPT_PATH, ScriptsAndMappings.REDUCE_MIN_NAME));
        Request request6 = new Request(Constants.POST, String.format(SCRIPT_PATH, ScriptsAndMappings.REDUCE_SUM_NAME));
        Request request7 = new Request(Constants.POST, String.format(SCRIPT_PATH, ScriptsAndMappings.REDUCE_AVG_NAME));
        Request request8 = new Request(Constants.POST, String.format(SCRIPT_PATH, ScriptsAndMappings.COMBINE_MAX_NAME));
        Request request9 = new Request(Constants.POST, String.format(SCRIPT_PATH, ScriptsAndMappings.REDUCE_MAX_NAME));

        List<Response> res = new ArrayList<>(8);

        res.add(requestBuilder.performRequest(request0, String.format(ScriptsAndMappings.SCRIPT_STORAGE,
                                                                      ScriptsAndMappings.INIT_AGGS), auth));
        res.add(requestBuilder.performRequest(request1, String.format(ScriptsAndMappings.SCRIPT_STORAGE,
                                                                      ScriptsAndMappings.MAP_AGGS), auth));
        res.add(requestBuilder.performRequest(request2, String.format(ScriptsAndMappings.SCRIPT_STORAGE,
                                                                      ScriptsAndMappings.COMBINE_MIN), auth));
        res.add(requestBuilder.performRequest(request3, String.format(ScriptsAndMappings.SCRIPT_STORAGE,
                                                                      ScriptsAndMappings.COMBINE_SUM), auth));
        res.add(requestBuilder.performRequest(request4, String.format(ScriptsAndMappings.SCRIPT_STORAGE,
                                                                      ScriptsAndMappings.COMBINE_AVG), auth));
        res.add(requestBuilder.performRequest(request5, String.format(ScriptsAndMappings.SCRIPT_STORAGE,
                                                                      ScriptsAndMappings.REDUCE_MIN), auth));
        res.add(requestBuilder.performRequest(request6, String.format(ScriptsAndMappings.SCRIPT_STORAGE,
                                                                      ScriptsAndMappings.REDUCE_SUM), auth));
        res.add(requestBuilder.performRequest(request7, String.format(ScriptsAndMappings.SCRIPT_STORAGE,
                                                                      ScriptsAndMappings.REDUCE_AVG), auth));
        res.add(requestBuilder.performRequest(request8, String.format(ScriptsAndMappings.SCRIPT_STORAGE,
                                                                      ScriptsAndMappings.COMBINE_MAX), auth));
        res.add(requestBuilder.performRequest(request9, String.format(ScriptsAndMappings.SCRIPT_STORAGE,
                                                                      ScriptsAndMappings.REDUCE_MAX), auth));

        for (Response r : res) {
            if (r.getStatusLine().getStatusCode() > 204)
                throw new Exceptions.ElasticException("Couldn't create and store Scripts.");
        }


    }

    public StructureSchema getEncryption(String index, String auth) {

        StructureSchema schema = schemas.get(index);

        if (schema == null) {
            Request request = new Request(Constants.GET, String.format(CONFIG_SOURCE_PATH, index));
            Response response = requestBuilder.performRequest(request, null, auth);

            if (ResponseBuilder.documentExist(response)) {
                String responseBody = ResponseBuilder.getResponseBody(response);

                schema = (StructureSchema) JSONMapper.writeStringAsObject(responseBody, StructureSchema.class);
                schemas.put(index, schema);
            } else
                throw new Exceptions.UnsupportedOperationsException(
                        String.format("Index [%s] isn't yet configured.", index));
        }
        return schema;
    }

    public void generateAndStoreKeys(StructureSchema schema) {

        KeyValues keys = new KeyValues();

        keys.setAdd(HelpSerial.toString(HomoAdd.generateKey()));
        keys.setDet(HelpSerial.toString(HomoDet.generateKey()));
        keys.setOpe(HomoOpeInt.generateKey());
        keys.setRand(HelpSerial.toString(HomoRand.generateKey()));
        keys.setMult(HelpSerial.toString(HomoMult.generateKey()));
        keys.setSearch(HelpSerial.toString(HomoSearch.generateKey()));

        schema.setKeys(keys);

    }


    public void generateHashMappings(StructureSchema schema) {


        Map<String, String> hashMappings = new HashMap<>();
        String index = schema.getIndex();

        Set<String> uniqueTerms = new HashSet<>();


        schema.getSchema().forEach((k, v) -> {

            String[] path = k.split("[.]");
            for (String term : path) {

                if (uniqueTerms.add(term)) {
                    if (!v.contains("NONE"))
                        hashMappings.put(cryptoUtils.hashValue(index + term), term);
                    else
                        hashMappings.put(term, term);

                }
            }
        });

        schema.setHashes(hashMappings);
    }

    private void createIndexNumericMappings(String auth, StructureSchema schema) {

        Request request = new Request(Constants.PUT, schema.getIndex());

        Response r = requestBuilder.performRequest(request, ScriptsAndMappings.WHITESPACE_ANALYZER, auth);

        if (r.getStatusLine().getStatusCode() > 204)
            throw new Exceptions.ElasticException("Couldn't create Index.");
    }

    public void deleteIndex(String index, String auth) {

        Request request = new Request(Constants.DELETE, String.format(CONFIG_CREATE_PATH, index));

        Response r = requestBuilder.performRequest(request, null, auth);

        schemas.remove(index);
    }
}

