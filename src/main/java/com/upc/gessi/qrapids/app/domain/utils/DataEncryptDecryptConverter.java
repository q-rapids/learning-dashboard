package com.upc.gessi.qrapids.app.domain.utils;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.nio.charset.StandardCharsets;

@Component
@Converter
public class DataEncryptDecryptConverter implements AttributeConverter<String, String> {

    private static String key;
    private static String initVector;

    private static String algo;

    // This method add the environment key, initVector and algorithm to encrypt and decrypt data
    @Autowired
    public void setData(@Value("${database.encryption.key}") String key, @Value("${database.encryption.initvector}") String initVector, @Value("${database.encryption.algorithm}") String algo){
        DataEncryptDecryptConverter.key = key;
        DataEncryptDecryptConverter.initVector = initVector;
        DataEncryptDecryptConverter.algo = algo;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");

            Cipher cipher = Cipher.getInstance(algo);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

            byte[] encrypted = cipher.doFinal(attribute.getBytes());
            return Base64.encodeBase64String(encrypted);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");

            Cipher cipher = Cipher.getInstance(algo);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

            byte[] original = cipher.doFinal(Base64.decodeBase64(dbData));
            return new String(original);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

}