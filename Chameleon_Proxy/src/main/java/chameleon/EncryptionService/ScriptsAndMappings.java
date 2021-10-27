package chameleon.EncryptionService;

public class ScriptsAndMappings {

    public static final String SCRIPT_STORAGE = "{\n" + "    \"script\":{\n" + "        \"lang\":\"painless\",\n" +
            "        \"source\":\"%s\"\n" + "    }\n" + "}";
    public static final String WHITESPACE_ANALYZER =
            "{\n" + "\"mappings\": {\n" + "    \"dynamic_templates\": [\n" + "      {\n" + "        \"integers\": {\n" +
                    "          \"match\": \"ADDITION*\",\n" + "          \"mapping\": {\n" +
                    "            \"type\": \"keyword\"\n" + "          }\n" + "        }\n" + "      }\n" + "     ]\n" +
                    "  },\n" + "  \"settings\": {\n" + "    \"analysis\": {\n" + "      \"analyzer\": {\n" +
                    "        \"default\": {\n" + "          \"type\": \"whitespace\"\n" + "        }\n" + "      }\n" +
                    "    }\n" + "  }\n" + "}";

    public static final String AGG_BODY =
            "{\n" + "  \"scripted_metric\": {\n" + "    \"init_script\": {\n" + "      \"id\": \"%s\"\n" +
                    "    },\n" + "    \"map_script\": {\n" + "      \"id\": \"%s\"\n" + "    },\n" +
                    "    \"combine_script\": {\n" + "      \"id\": \"%s\"\n" + "    },\n" +
                    "    \"reduce_script\": {\n" + "      \"id\": \"%s\"\n" + "    },\n" +
                    "    \"params\": {\n" + "      %s           \n" + "    }\n" + "  }\n" + "}";

    public static final String INIT_AGGS = "state.transactions = []";
    public static final String MAP_AGGS = "state.transactions.add(doc[params['field']].value)";
    public static final String INIT_NAME= "init";
    public static final String MAP_NAME= "map";


    public static final String COMBINE_MAX = "long max = params['initialValue']; for (t in state.transactions)  { " +
            "if (t > max) {max = t}} return max";
    public static final String REDUCE_MAX = "long max = params['initialValue']; for (a in states ) { if (a " +
            "> max) {max = a}} return max";
    public static final String COMBINE_MAX_NAME= "combine-min-max";
    public static final String REDUCE_MAX_NAME= "reduce-min-max";
    public static final String COMBINE_MIN = "long max = params['initialValue']; for (t in state.transactions)  { " +
            "if (t < max) {max = t}} return max";
    public static final String REDUCE_MIN = "long max = params['initialValue']; for (a in states ) { if (a " +
            "< max) {max = a}} return max";
    public static final String COMBINE_MIN_NAME= "combine-min";
    public static final String REDUCE_MIN_NAME= "reduce-min";

    public static final String COMBINE_SUM =

            "long n = System.currentTimeMillis();BigInteger sum = BigInteger.ONE; for (t in state.transactions) { sum = sum" +
                    ".multiply(new BigInteger" +
                    "(t))" +
                    ".mod(new BigInteger(params['nSquare'])) } return sum";
    public static final String REDUCE_SUM =
            "BigInteger sum = BigInteger.ONE; for (a in states) { sum = sum.multiply(a).mod(new BigInteger" +
                    "(params['nSquare']))" + " } return sum";
    public static final String COMBINE_SUM_NAME= "combine-sum";
    public static final String REDUCE_SUM_NAME= "reduce-sum";

    public static final String COMBINE_AVG =
            "BigInteger sum = BigInteger.ONE;BigInteger[] x = new BigInteger[2];long count = state.transactions" +
                    ".length;for (t in state.transactions) {sum = sum.multiply(new BigInteger(t)).mod(new BigInteger" +
                    "(params['nSquare'])) } x[0] = sum;x[1] = BigInteger.valueOf(count);return x";
    public static final String REDUCE_AVG =
            "BigInteger sum = BigInteger.ONE;BigInteger[] x = new BigInteger[2];BigInteger totalCount = BigInteger" +
                    ".ZERO;for (a in states) {sum = sum.multiply(a[0]).mod(new BigInteger(params['nSquare'])); " +
                    "totalCount = " + "totalCount.add(a[1]) } x[0] = sum;x[1] = totalCount;return x";
    public static final String COMBINE_AVG_NAME= "combine-avg";
    public static final String REDUCE_AVG_NAME= "reduce-avg";

}
