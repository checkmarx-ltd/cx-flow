package com.checkmarx.flow.utils;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;

import javax.annotation.PostConstruct;
import java.util.Base64;
import java.util.Properties;

@Configuration
public class JasyptConfig {

    @Value("${jasypt.encryptor.algorithm:PBEWithMD5AndDES}")
    private String algorithm;

    @Value("${jasypt.encryptor.password:XXX}")
    private String password;

    @Value("${jasypt.encryptor.isBase64:false}") // Boolean flag to determine if password is Base64 encoded
    private boolean isBase64;

    private final ConfigurableEnvironment environment;

    public JasyptConfig(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void configureJasyptPasskey() {
        String decodedPasskey;

        // Check if the password is Base64 encoded or plain text based on the boolean flag
        if (isBase64) {
            decodedPasskey = new String(Base64.getDecoder().decode(password));
        } else {
            decodedPasskey = password; // Use the plain text password
        }

        // Set the passkey (whether decoded or plain) in the environment
        Properties props = new Properties();
        props.setProperty("jasypt.encryptor.password", decodedPasskey);
        MutablePropertySources propertySources = environment.getPropertySources();
        propertySources.addFirst(new PropertiesPropertySource("jasyptProperties", props));

        // Optionally print the passkey to verify
        //System.out.println("Final Jasypt Passkey: " + decodedPasskey);
    }

    @Bean(name = "jasyptStringEncryptor")
    public StringEncryptor stringEncryptor() {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(environment.getProperty("jasypt.encryptor.password")); // Use the (possibly decoded) passkey
        encryptor.setAlgorithm(algorithm); // You can change the algorithm if needed
        return encryptor;
    }
}



