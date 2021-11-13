deploy_WebApp(){

  printf "\nBuilding and Deploying Chameleon Proxy Kubernetes Cluster...\n\n"

  printf "\nInstalling ECK Operator in Kubernetes Cluster\n\n"
  kubectl apply -f https://download.elastic.co/downloads/eck/1.6.0/all-in-one.yaml
  printf "\nInstalling ECK Operator in Kubernetes Cluster Done.\n\n"

  printf "\nGiving some time for ECK Operator to load...\n\n"
  sleep 30

  printf "\nDeploying ElasticSearch Cluster to Kubernetes Cluster...\n\n"
  kubectl apply -f Kubernetes_Deployment/ECK.yaml
  printf "\nDeploying Kubernetes App Done.\n\n"

  printf "\nImporting ElasticSearch Cluster Certificate to Chameleon Proxy Truststore...\n\n"

  (cd Chameleon_Proxy/src/main/resources/Keys;

    SECRET=$(kubectl get secret chameleon-es-http-certs-public -o name)

    while [ -z "$SECRET" ]
    do
      printf "\nWaiting for ES cluster to be created...\n"
      printf "\nRetrying in 15 seconds...\n\n"
      sleep 15
      SECRET=$(kubectl get secret chameleon-es-http-certs-public -o name)
    done

    rm elastic.crt
    kubectl get secret chameleon-es-http-certs-public -o go-template='{{index .data "tls.crt" | base64decode }}' > elastic.crt
    keytool -keystore chameleonTS -delete -alias public -storepass chameleon 
    keytool -import -alias public -file elastic.crt -keystore chameleonTS -storepass chameleon -noprompt
  )

  printf "\nImporting ElasticSearch Cluster Certificate to Chameleon Proxy Truststore Done...\n\n"

  mvn -f Chameleon_Proxy/ package -U

  printf "\nDeploying Chameleon Proxy to Kubernetes Cluster...\n\n"
  kubectl apply -f Kubernetes_Deployment/chameleon.yaml
  kubectl rollout restart deployment chameleon-proxy
  printf "\nDeploying Chameleon Proxy to Kubernetes Cluster Done.\n\n"

  PWD=$(kubectl get secret chameleon-es-elastic-user -o go-template='{{.data.elastic | base64decode }}')

  printf "\nElastic user password for testing purposes: $PWD" 
  
  printf "\nDeployment of Chameleon Proxy Kubernetes Done.\n\n"
  printf "Hopefully...\n"

}


IMAGE="jsantosbruv/chameleon-proxy"

deploy_WebApp 
