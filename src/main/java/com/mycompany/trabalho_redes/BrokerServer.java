/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.trabalho_redes;

import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import org.json.JSONObject;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

/**
 *
 * @author MURILO
 */
public class BrokerServer {

    public static void main(String args[]) throws Exception {
//        String caminhoArquivoPub = "C:\\Users\\MURILO\\OneDrive\\Desktop\\Fase 5\\redes 2\\Trabalho_Redes\\src\\main\\java\\keys\\brokerPub.key";
//        String caminhoArquivoPvd = "C:\\Users\\MURILO\\OneDrive\\Desktop\\Fase 5\\redes 2\\Trabalho_Redes\\src\\main\\java\\keys\\brokerPvd.key";
//
//        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
//        kpg.initialize(2048);
//
//        KeyPair kp = kpg.generateKeyPair();
//
//        PrivateKey privateKey = kp.getPrivate();
//        PublicKey publicKey = kp.getPublic();
//        String chavePublica = Base64.getEncoder().encodeToString(publicKey.getEncoded());
//        String chavePrivada = Base64.getEncoder().encodeToString(privateKey.getEncoded());
//        
//        try (FileWriter file = new FileWriter(caminhoArquivoPub)) {
//            file.write(chavePublica);
//            file.flush();
//        }
//        try (FileWriter file = new FileWriter(caminhoArquivoPvd)) {
//            file.write(chavePrivada);
//            file.flush();
//        }

        ServerSocket serverSocket = new ServerSocket(9090);
        BrokerCore broker = new BrokerCore();

        System.out.println("Broker rodando na porta 9090");

        while (true) {
            Socket socket = serverSocket.accept();
            new ClienteHandler(socket, broker).start();
        }
    }
}
