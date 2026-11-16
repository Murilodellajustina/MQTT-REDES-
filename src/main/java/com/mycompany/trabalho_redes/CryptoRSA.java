package com.mycompany.trabalho_redes;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import javax.crypto.Cipher;

public class CryptoRSA {

    public static String criptografar(byte[] dados, PublicKey chave) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, chave);
        return Base64.getEncoder().encodeToString(cipher.doFinal(dados));
    }

    public static byte[] descriptografar(String texto,PrivateKey chave) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, chave);
        return cipher.doFinal(Base64.getDecoder().decode(texto));
    }

}