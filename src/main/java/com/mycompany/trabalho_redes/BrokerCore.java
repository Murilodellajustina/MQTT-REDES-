/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.trabalho_redes;

import static com.mycompany.trabalho_redes.ClienteHandler.carregarChavePublica;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import javax.crypto.SecretKey;

public class BrokerCore {

    private Map<String, Set<String>> topicos = new ConcurrentHashMap<>();
    private Map<String, Usuario> usuarios = new ConcurrentHashMap<>();
    private Map<String, List<String>> historicoTopicos = new ConcurrentHashMap<>();
    private Map<String, SentinelaTopico> sentinelas = new ConcurrentHashMap<>();

    public Usuario registrarUsuario(String nome) {
        return usuarios.computeIfAbsent(nome, Usuario::new);
    }

    public Usuario getUsuario(String nome) {
        return usuarios.get(nome);
    }

    public void CriarTopicos(String topico) {
        topicos.putIfAbsent(topico, ConcurrentHashMap.newKeySet());
        try {
            sentinelas.putIfAbsent(topico, new SentinelaTopico(topico));
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Topico criado");
    }

    public SentinelaTopico getSentinela(String topico) {
        return sentinelas.get(topico);
    }

    public Set<String> listarTopicos() {
        return topicos.keySet();
    }

    public void subscribe(String topico, ClienteHandler usuario) {
        topicos.putIfAbsent(topico, ConcurrentHashMap.newKeySet());
        topicos.get(topico).add(usuario.getNome());
        SentinelaTopico s = sentinelas.get(topico);
        if (s != null) {
            s.adicionarMembro(usuario.getNome());
        }
    }

    public void unsubscribe(String topico, ClienteHandler usuario) {
        topicos.get(topico).remove(usuario.getNome());
    }

    public void publish(ClienteHandler remetente, String topico, String mensagem) {
        Set<String> grupos = topicos.get(topico);
        if (grupos == null) {
            return;
        }
        if (!grupos.contains(remetente.getNome())) {
            remetente.send("Você não esta inscrito neste topico");
            return;
        }
        String pacote = "[MSG][" + remetente.getNome() + " " + topico + "] " + mensagem;
        historicoTopicos.computeIfAbsent(topico, k -> new CopyOnWriteArrayList<>()).add(pacote);
        for (String nomeUsuario : grupos) {
            Usuario usuario = usuarios.get(nomeUsuario);
            if (usuario == null) {
                continue;
            }
            ClienteHandler conexao = usuario.getConexao();
            System.out.println("Usuario " + usuario.getNome() + " conexao " + usuario.getConexao());
            if (conexao != null) {
                conexao.send(pacote);
            } else {
                System.out.println("SALVANDO OFFLINE " + usuario.getNome() + " : " + pacote);

            }
        }
    }

    public static PrivateKey carregarChavePrivada(String caminho) throws Exception {
        String chaveBase64 = new String(Files.readAllBytes(Paths.get(caminho))).trim();
        byte[] keyBytes = Base64.getDecoder().decode(chaveBase64);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    public void criarCertificado(String nomeCert, String chavePublica, String dados) throws Exception {

        String conteudoCertificado = nomeCert + "\n" + chavePublica + "\n" + LocalDate.now();

        PrivateKey brokerChavePrivada = carregarChavePrivada("C:\\Users\\MURILO\\OneDrive\\Desktop\\Fase 5\\redes 2\\Trabalho_Redes\\src\\main\\java\\keys\\brokerPvd.key");

        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(brokerChavePrivada);
        sig.update(conteudoCertificado.getBytes(StandardCharsets.UTF_8));
        String assinaturaBroker = Base64.getEncoder().encodeToString(sig.sign());
        conteudoCertificado = conteudoCertificado + "\n" + assinaturaBroker;

        String corpoBase64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(conteudoCertificado.getBytes());

        String certificado
                = "-----BEGIN CERTIFICATE-----\n"
                + corpoBase64
                + "\n-----END CERTIFICATE-----";

        String caminhoCertificado = "C:\\Users\\MURILO\\OneDrive\\Desktop\\Fase 5\\redes 2\\Trabalho_Redes\\src\\main\\java\\certificados\\";
        try (FileWriter file = new FileWriter(caminhoCertificado + nomeCert + ".crt")) {
            file.write(certificado);
            file.flush();
        }

    }

    public boolean validarCertificado(String caminhocertificado) {
        try {
            String pem = Files.readString(Paths.get(caminhocertificado));
            pem = pem.replace("-----BEGIN CERTIFICATE-----", "")
                    .replace("-----END CERTIFICATE-----", "")
                    .replaceAll("\\s+", "");
            String conteudo = new String(Base64.getDecoder().decode(pem), StandardCharsets.UTF_8);
            String[] linhas = conteudo.split("\n");
            if (linhas.length != 4) {
                return false;
            }

            String nome = linhas[0];
            String chavePublica = linhas[1];
            String data = linhas[2];
            String assinaturaBroker = linhas[3];
            String dadosAssinados = nome + "\n" + chavePublica + "\n" + data;

            PublicKey chaveBroker = carregarChavePublica("C:\\Users\\MURILO\\OneDrive\\Desktop\\Fase 5\\redes 2\\Trabalho_Redes\\src\\main\\java\\keys\\brokerPub.key");

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(chaveBroker);
            signature.update(dadosAssinados.getBytes(StandardCharsets.UTF_8));

            return signature.verify(Base64.getDecoder().decode(assinaturaBroker));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Set<String> getTopicosDoUsuario(String nomeUsuario) {

        Set<String> inscritos = new HashSet<>();

        for (String topico : topicos.keySet()) {

            if (topicos.get(topico).contains(nomeUsuario)) {

                inscritos.add(topico);

            }
        }

        return inscritos;
    }

    public void enviarChaveTopico(ClienteHandler cliente,String topico) {
        try {
            SentinelaTopico sentinela = sentinelas.get(topico);
            if (sentinela == null) {
                cliente.send("ERRO Topico inexistente");
                return;
            }
            String pacote = sentinela.gerarChaveParaUsuario(cliente.getNome());
            cliente.send("TOPIC_KEY " + topico + " " + pacote);

        } catch (Exception e) {

            e.printStackTrace();

        }

    }

    public List<String> getHistoricoTopico(String topico) {

        return historicoTopicos.getOrDefault(topico, Collections.emptyList());
    }

}
