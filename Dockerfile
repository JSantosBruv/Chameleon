### DockerFile used to generate Chameleon Proxy ###
FROM adoptopenjdk/openjdk11
ARG JAR_FILE=Chameleon_Proxy/target/*.jar
COPY ${JAR_FILE} app.jar
COPY Chameleon_Proxy/src/main/resources/Keys/elastic.crt $JAVA_HOME/lib/security
ENTRYPOINT ["java","-jar","/app.jar"]
EXPOSE 8443
RUN \
    cd $JAVA_HOME/lib/security \
    && keytool -keystore cacerts -storepass changeit -noprompt -trustcacerts -importcert -alias elastic -file elastic.crt