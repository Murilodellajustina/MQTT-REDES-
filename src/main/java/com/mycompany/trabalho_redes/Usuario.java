/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.trabalho_redes;

import com.mycompany.trabalho_redes.ClienteHandler;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
public class Usuario {

    private String nome;
    private boolean online;
    private PublicKey chavePublica;
    private Map<String, List<String>> historicoTopicos = new ConcurrentHashMap<>();
    private ClienteHandler conexao;

    public Usuario(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public ClienteHandler getConexao() {
        return conexao;
    }

    public void setConexao(ClienteHandler conexao) {
        this.conexao = conexao;
    }

    public PublicKey getChavePublica() {
        return chavePublica;
    }

    public void setChavePublica(PublicKey chavePublica) {
        this.chavePublica = chavePublica;
    }

    public void adicionarMensagemPendente(String topico, String mensagem) {
        historicoTopicos.computeIfAbsent(topico, k -> new ArrayList<>()).add(mensagem);
    }

    public Map<String, List<String>> getHistoricoTopicos() {
        return historicoTopicos;
    }
    
}
