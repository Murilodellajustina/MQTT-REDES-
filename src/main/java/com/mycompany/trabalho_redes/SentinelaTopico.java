package com.mycompany.trabalho_redes;

import java.security.PublicKey;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class SentinelaTopico {

    private final String nomeTopico;

    private final SecretKey chaveAES;

    private final Set<String> membros;

    public SentinelaTopico(String nomeTopico) throws Exception {

        this.nomeTopico = nomeTopico;

        KeyGenerator kg = KeyGenerator.getInstance("AES");

        kg.init(256);

        this.chaveAES = kg.generateKey();

        this.membros = ConcurrentHashMap.newKeySet();

        System.out.println("Sentinela criado para " + nomeTopico);

    }

    public String gerarChaveParaUsuario(String nomeUsuario) throws Exception {

        String caminho="C:\\Users\\MURILO\\OneDrive\\Desktop\\Fase 5\\redes 2\\Trabalho_Redes\\src\\main\\java\\keys\\" + nomeUsuario + "Pub.key";

        PublicKey chavePublica = ClienteHandler.carregarChavePublica(caminho);

        return CryptoRSA.criptografar(chaveAES.getEncoded(),chavePublica);
    }

    public String getNomeTopico() {
        return nomeTopico;
    }

    public SecretKey getChaveAES() {
        return chaveAES;
    }

    public void adicionarMembro(String nome) {
        membros.add(nome);
    }

    public void removerMembro(String nome) {
        membros.remove(nome);
    }

    public boolean contem(String nome) {
        return membros.contains(nome);
    }

    public Set<String> getMembros() {
        return membros;
    }

}
