/*
 * (c) 2003-2018 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package com.mycompany.certificate.provider.api;


import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

import static com.mycompany.certificate.provider.api.CertificateConfigurationPropertiesExtensionLoadingDelegate.CONFIG_ELEMENT;
import static com.mycompany.certificate.provider.api.CertificateConfigurationPropertiesExtensionLoadingDelegate.EXTENSION_NAME;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.extension.api.util.NameUtils.defaultNamespace;

import org.apache.commons.io.FileUtils;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.config.api.dsl.model.ConfigurationParameters;
import org.mule.runtime.config.api.dsl.model.ResourceProvider;
import org.mule.runtime.config.api.dsl.model.properties.ConfigurationPropertiesProvider;
import org.mule.runtime.config.api.dsl.model.properties.ConfigurationPropertiesProviderFactory;
import org.mule.runtime.config.api.dsl.model.properties.ConfigurationProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

/**
 * Builds the provider for a custom-properties-provider:config element.
 *
 * @since 1.0
 */
public class CertificateConfigurationPropertiesProviderFactory implements ConfigurationPropertiesProviderFactory {

  public static final String EXTENSION_NAMESPACE = defaultNamespace(EXTENSION_NAME);
  private static final ComponentIdentifier CUSTOM_PROPERTIES_PROVIDER =
      builder().namespace(EXTENSION_NAMESPACE).name(CONFIG_ELEMENT).build();
  // TODO change to meaningful prefix
  private final static String CUSTOM_PROPERTIES_PREFIX = "secure::";
  private static final String TEST_KEY = "testKey";
  private static final String DECRYPT_MODE = "decrypt";
  private static final String ENCRYPT_MODE = "encrypt";
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationPropertiesProvider.class);


  @Override
  public ComponentIdentifier getSupportedComponentIdentifier() {
    return CUSTOM_PROPERTIES_PROVIDER;
  }

  @Override
  public ConfigurationPropertiesProvider createProvider(ConfigurationParameters parameters,
                                                        ResourceProvider externalResourceProvider) {

    // This is how you can access the configuration parameter of the <custom-properties-provider:config> element.
    String muleKey = parameters.getStringParameter("MuleKey");
    String accessKey = decryptedKey(parameters.getStringParameter("AWSAccessKey"),muleKey);
    String secretKey = decryptedKey(parameters.getStringParameter("AWSSecretKey"),muleKey);
    String region = parameters.getStringParameter("AWSRegion");
    String location = parameters.getStringParameter("APICertificateLocation");
    String certificate = parameters.getStringParameter("AWSCertificateLocation");
    
    LOGGER.info("Creating AWS Connection Access Key: {} AWS Region: {}",accessKey,region);

    AmazonS3 s3Client = createAWSClient(accessKey,secretKey,region);
    try {
      createKeyFiles(location,certificate,s3Client);
    } catch (IOException e) {
      e.printStackTrace();
    }


    return new ConfigurationPropertiesProvider() {

      @Override
      public Optional<ConfigurationProperty> getConfigurationProperty(String configurationAttributeKey) {
        // TODO change implementation to discover properties values from your custom source
        if (configurationAttributeKey.startsWith(CUSTOM_PROPERTIES_PREFIX)) {
          String effectiveKey = configurationAttributeKey.substring(CUSTOM_PROPERTIES_PREFIX.length());
          if (effectiveKey.equals(TEST_KEY)) {
            return Optional.of(new ConfigurationProperty() {

              @Override
              public Object getSource() {
                return "custom provider source";
              }

              @Override
              public Object getRawValue() {
                return accessKey;
              }

              @Override
              public String getKey() {
                return TEST_KEY;
              }
            });
          }
        }
        return Optional.empty();
      }

      @Override
      public String getDescription() {
        // TODO change to a meaningful name for error reporting.
        return "Custom properties provider";
      }
    };
  }


  public AmazonS3 createAWSClient(String accessKey, String secretKey, String region) {
     
      LOGGER.info("createAwsClient: region={}, accessKey={}", region, accessKey);
      AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
      AmazonS3 s3Client = AmazonS3ClientBuilder
              .standard()
              .withRegion(region)
              .withCredentials(new AWSStaticCredentialsProvider(credentials))
              .build();
          
      LOGGER.info("AWS connection created");
      return s3Client;
  }

  public void createKeyFiles(String location,String certificate, AmazonS3 s3Client) throws IOException{
    	
    	  	LOGGER.info("Creating Certificate for: {}", location);
    		int firstSlashIndex = certificate.indexOf("/");
    		
    		String certPath = certificate.substring(firstSlashIndex + 1);
    		String bucket = certificate.substring(0,firstSlashIndex);

    		LOGGER.info("Bucket: {} and Certificate path : {}", certPath, bucket);
    		
    		GetObjectRequest getObjectRequest = new GetObjectRequest(bucket,certPath);
    		S3Object s3Object = s3Client.getObject(getObjectRequest);
    		
    		S3ObjectInputStream objectInputStream = s3Object.getObjectContent();
    		FileUtils.copyInputStreamToFile(objectInputStream, new File(location));

    		LOGGER.info("file Succesfully placed at location  {}",location);
    	
    }
  
  public String decryptedKey(String key,String muleKey) {
	  if(key.contains(CUSTOM_PROPERTIES_PREFIX)) {	  
		  key = getKey(key.replace(CUSTOM_PROPERTIES_PREFIX, ""),muleKey,DECRYPT_MODE);
		  return key;  
	  }
	  else {
		  return key;
	  }
  }
  
  
  public String encryptedKey(String key,String muleKey) {
		  key = getKey(key.replace(CUSTOM_PROPERTIES_PREFIX, ""),muleKey,ENCRYPT_MODE);
		  return key;
  }
  
  // get decrypted or encrypted keys
  public String getKey(String key, String muleKey, String mode){ 
	  try {
		URL url = new URL("https://secure-properties-api.us-e1.cloudhub.io/api/string");
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        
        // Create an HttpPost request
        connection.setRequestMethod("POST");

        // Enable input/output streams
        connection.setDoOutput(true);
        connection.setDoInput(true);

        // Set the content type to multipart/form-data
        String boundary = "---------------------------" + System.currentTimeMillis();
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
    
        // Create the multipart/form-data body

        try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())){
        	addFormField(outputStream, "operation", mode ,boundary);
            addFormField(outputStream, "algorithm", "AES",boundary);
            addFormField(outputStream, "mode", "CBC",boundary);
            addFormField(outputStream, "key", muleKey,boundary);
            addFormField(outputStream, "value", key,boundary);
            addFormField(outputStream, "method", "string",boundary);

           // outputStream.write(textData.getBytes(StandardCharsets.UTF_8));
           outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
           outputStream.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        StringBuilder response = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        // parsing file "JSONExample.json" 
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(response.toString());
        key = jsonNode.get("property").asText();
        connection.disconnect();

	} catch (Exception e) {
		e.printStackTrace();
	}  
	  return key;
  }

  
  // create form data 
  private static void addFormField(DataOutputStream outputStream, String fieldName, String value, String boundary) throws IOException {
      outputStream.writeBytes("--" + boundary + "\r\n");
      outputStream.writeBytes("Content-Disposition: form-data; name=\"" + fieldName + "\"\r\n\r\n");
      
      outputStream.writeBytes(value + "\r\n");
  }
  

}