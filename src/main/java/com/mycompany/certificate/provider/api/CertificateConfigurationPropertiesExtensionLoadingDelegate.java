/*
 * (c) 2003-2018 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package com.mycompany.certificate.provider.api;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.meta.Category.SELECT;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclarer;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionLoadingDelegate;

/**
 * Declares extension for Secure Properties Configuration module
 *
 * @since 1.0
 */
public class CertificateConfigurationPropertiesExtensionLoadingDelegate implements ExtensionLoadingDelegate {

    public static final String EXTENSION_NAME = "certificate-provider";
    public static final String CONFIG_ELEMENT = "config";


    public CertificateConfigurationPropertiesExtensionLoadingDelegate() {

	}

  @Override
  public void accept(ExtensionDeclarer extensionDeclarer, ExtensionLoadingContext context) {
    ConfigurationDeclarer configurationDeclarer = extensionDeclarer.named(EXTENSION_NAME)
        .describedAs(String.format("Crafted %s Extension", EXTENSION_NAME))
        .withCategory(SELECT)
        .onVersion("1.0.0")
        .fromVendor("mycompany")
        .withConfig(CONFIG_ELEMENT);

    ParameterGroupDeclarer defaultParameterGroup = configurationDeclarer.onDefaultParameterGroup();
    
    defaultParameterGroup
        .withRequiredParameter("AWSAccessKey").ofType(BaseTypeBuilder.create(JAVA).stringType().build())
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("AWS S3 Access key");

    defaultParameterGroup
        .withRequiredParameter("AWSSecretKey").ofType(BaseTypeBuilder.create(JAVA).stringType().build())
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("AWS S3 Secret Key encrypted");

    defaultParameterGroup
        .withRequiredParameter("AWSRegion").ofType(BaseTypeBuilder.create(JAVA).stringType().build())
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("AWS S3 Region");
    
    defaultParameterGroup
    .withRequiredParameter("MuleKey").ofType(BaseTypeBuilder.create(JAVA).stringType().build())
    .withExpressionSupport(NOT_SUPPORTED)
    .describedAs("Use mule.key property");

    defaultParameterGroup
        .withRequiredParameter("APICertificateLocation").ofType(BaseTypeBuilder.create(JAVA).stringType().build())
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("API certification location");

    defaultParameterGroup
        .withRequiredParameter("AWSCertificateLocation").ofType(BaseTypeBuilder.create(JAVA).stringType().build())
        .withExpressionSupport(NOT_SUPPORTED)
        .describedAs("bucketName/S3-folder-location");


  }

}