package com.mycompany.trabalho_redes;

import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;

public class CryptoAES {

    public static String criptografar(String texto, SecretKey chave) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, chave);
        byte[] cifrado = cipher.doFinal(texto.getBytes());
        return Base64.getEncoder().encodeToString(cifrado);
    }

    public static String descriptografar(String texto, SecretKey chave) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, chave);
        byte[] bytes = Base64.getDecoder().decode(texto);
        return new String(cipher.doFinal(bytes));
    }

}