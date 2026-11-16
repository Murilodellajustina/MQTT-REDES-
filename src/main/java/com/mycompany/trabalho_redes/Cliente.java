package com.mycompany.trabalho_redes;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;

public class Cliente {

    public static PrivateKey carregarChavePrivada(String caminho) throws Exception {
        String chaveBase64 = new String(Files.readAllBytes(Paths.get(caminho))).trim();
        byte[] keyBytes = Base64.getDecoder().decode(chaveBase64);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    public static void main(String args[]) throws InvalidKeyException, SignatureException, Exception {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);

            KeyPair kp = kpg.generateKeyPair();
            PrivateKey privada = kp.getPrivate();
            PublicKey publica = kp.getPublic();
            String publicaBase64 = Base64.getEncoder().encodeToString(publica.getEncoded());
            String privadaBase64 = Base64.getEncoder().encodeToString(privada.getEncoded());

            Socket socket = new Socket("192.168.2.112", 9090);
            Scanner scanner = new Scanner(System.in);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("Insira seu nome: ");
            String nome = scanner.nextLine();
            String caminhoArquivo = "C:\\Users\\MURILO\\OneDrive\\Desktop\\Fase 5\\redes 2\\Trabalho_Redes\\src\\main\\java\\keys\\";
            try (FileWriter file = new FileWriter(caminhoArquivo + nome + "Pvd.key")) {
                file.write(privadaBase64);
                file.flush();
            }
            try (FileWriter file = new FileWriter(caminhoArquivo + nome + "Pub.key")) {
                file.write(publicaBase64);
                file.flush();
            }
            String caminhoPrivada = "C:\\Users\\MURILO\\OneDrive\\Desktop\\Fase 5\\redes 2\\Trabalho_Redes\\src\\main\\java\\keys\\" + nome +"Pvd.key";
            String dadosCSR = nome + ";" + publicaBase64;
            Signature sig = Signature.getInstance("SHA256withRSA");
            PrivateKey chavePrivadaUsuario = carregarChavePrivada(caminhoPrivada);
            sig.initSign(chavePrivadaUsuario);
            sig.update(dadosCSR.getBytes(StandardCharsets.UTF_8));

            String assinaturaUsuario = Base64.getEncoder().encodeToString(sig.sign());

            out.println("CERTIFICADO " + nome + " " + publicaBase64 + " " + assinaturaUsuario);
        } catch (IOException | NoSuchAlgorithmException ex) {
            Logger.getLogger(Cliente.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
