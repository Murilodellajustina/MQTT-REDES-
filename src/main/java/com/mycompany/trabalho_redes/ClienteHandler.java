/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.trabalho_redes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClienteHandler extends Thread {

    private Socket socket;
    private BrokerCore broker;
    private BufferedReader in;
    private PrintWriter out;
    private String nome;
    private boolean online = false;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public ClienteHandler(Socket socket, BrokerCore broker) throws IOException {
        this.socket = socket;
        this.broker = broker;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);

    }

    public void send(String msg) {
        out.println(msg);
    }

    public void entregarPendentes(Usuario usuario) {

        Map<String, List<String>> pendentes = usuario.getHistoricoTopicos();
        System.out.println("Entregando pendentes para: " + usuario.getNome());
        for (String topico : pendentes.keySet()) {
            for (String msg : pendentes.get(topico)) {
                send(msg);
            }
        }

        pendentes.clear();
    }

    public static PublicKey carregarChavePublica(String caminho) throws Exception {
        String chaveBase64 = Files.readString(Paths.get(caminho)).trim();
        byte[] keyBytes = Base64.getDecoder().decode(chaveBase64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    public static PrivateKey carregarChavePrivada(String caminho) throws Exception {
        String chaveBase64 = Files.readString(Paths.get(caminho)).trim();
        byte[] keyBytes = Base64.getDecoder().decode(chaveBase64);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    public void run() {
        try {
            String linha;
            while ((linha = in.readLine()) != null) {
                String[] partes = linha.split(" ", 4);
                String comando = partes[0];
                System.out.println("Recebido: " + linha);
                switch (comando) {
                    case "LOGIN":
                        String nomeLogin = partes[1];
                        String certBase64 = partes[2];

                        byte[] certBytes = Base64.getDecoder().decode(certBase64);
                        Path cert = Paths.get("certificados_recebidos", nomeLogin + ".crt");
                        Files.createDirectories(cert.getParent());
                        Files.write(cert, certBytes);
                        if (!broker.validarCertificado(cert.toString())) {
                            out.println("LOGIN RECUSADO!");
                            break;
                        } else {
                            Usuario usuario = broker.registrarUsuario(nomeLogin);
                            usuario.setOnline(true);
                            usuario.setConexao(this);
                            this.nome = nomeLogin;
                            this.online = true;
                            out.println("LOGIN OK");

                        }
                        break;

                    case "CREATE":
                        broker.CriarTopicos(partes[1]);
                        out.println("Tópico criado");
                        break;

                    case "LIST":
                        String lista = String.join(",", broker.listarTopicos());
                        out.println("TOPICOS:" + lista);
                        break;

                    case "SUB":
                        broker.subscribe(partes[1], this);
                        broker.enviarChaveTopico(this, partes[1]);
                        out.println(this.nome + " inscrito em " + partes[1]);
                        break;

                    case "UNSUB":
                        broker.unsubscribe(partes[1], this);
                        out.println(this.nome + " desinscrito de " + partes[1]);
                        break;

                    case "PUB":
                        broker.publish(this, partes[1], partes[2]);
                        break;
                    case "ONLINE":
                        Usuario usuarioOnline = broker.getUsuario(this.nome);
                        if (usuarioOnline != null) {
                            usuarioOnline.setOnline(true);
                            entregarPendentes(usuarioOnline);
                            out.println("Você está ONLINE");
                        }
                        break;
                    case "OFFLINE":
                        Usuario usuarioOffline = broker.getUsuario(this.nome);
                        if (usuarioOffline != null) {
                            usuarioOffline.setOnline(false);
                            out.println("Você está OFFLINE");
                        }
                        break;
                    case "CERTIFICADO":
                        String nomeCert = partes[1];
                        String chavePub = partes[2];
                        String assinaturaUsuario = partes[3];
                        String dados = nomeCert + ";" + chavePub;

                        byte[] chaveBytes = Base64.getDecoder().decode(chavePub);
                        KeyFactory kf = KeyFactory.getInstance("RSA");
                        PublicKey chavePublicaUsuario = kf.generatePublic(new X509EncodedKeySpec(chaveBytes));
                        Signature verify = Signature.getInstance("SHA256withRSA");
                        verify.initVerify(chavePublicaUsuario);
                        verify.update(dados.getBytes(StandardCharsets.UTF_8));
                        boolean valido = verify.verify(Base64.getDecoder().decode(assinaturaUsuario));
                        if (!valido) {
                            out.println("CSR INVALIDO");
                            break;
                        }

                        broker.criarCertificado(nomeCert, chavePub, dados);
                        out.println("CERTIFICADO GERADO COM SUCESSO!!");
                        break;
                    case "VALID_BROKER":
                        String nomeLogin2 = partes[1];
                        byte[] certB = Files.readAllBytes(Paths.get("C:\\Users\\MURILO\\OneDrive\\Desktop\\Fase 5\\redes 2\\Trabalho_Redes\\src\\main\\java\\certificados\\broker.cer"));

                        String base64 = Base64.getEncoder().encodeToString(certB);

                        out.println("BROKER_CERT " + base64);

                        Set<String> inscritos = broker.getTopicosDoUsuario(nomeLogin2);
                        for (String topico : inscritos) {
                            broker.enviarChaveTopico(this, topico);
                            List<String> historico = broker.getHistoricoTopico(topico);
                            for (String msg : historico) {
                                out.println(msg);
                            }
                        }
                        out.println("INSCRICOES:" + String.join(",", inscritos)
                        );

                        break;

                    default:
                        out.println("Comando desconhecido");
                        break;
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(ClienteHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(ClienteHandler.class.getName()).log(Level.SEVERE, null, ex);
        } finally {

            if (nome != null) {

                Usuario usuario = broker.getUsuario(nome);

                if (usuario != null) {
                    usuario.setOnline(false);
                    usuario.setConexao(null);
                }

                System.out.println(nome + " desconectou.");
            }
        }
    }
}
