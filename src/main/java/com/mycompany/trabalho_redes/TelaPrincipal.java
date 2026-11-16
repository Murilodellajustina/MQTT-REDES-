/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.mycompany.trabalho_redes;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 *
 * @author MURILO
 */
public class TelaPrincipal extends javax.swing.JFrame {

    private DefaultListModel<String> modeloTopicos = new DefaultListModel<>();
    private DefaultListModel<String> modeloInscritos = new DefaultListModel<>();
    private HashMap<String, StringBuilder> mensagensPorTopico = new HashMap<>();
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String topicoAtual = null;
    private Map<String, SecretKey> chavesTopicos = new ConcurrentHashMap<>();
    String nome;

    public void adicionarChaveTopico(String topico, SecretKey chave) {
        chavesTopicos.put(topico, chave);
    }

    public SecretKey getChaveTopico(String topico) {
        return chavesTopicos.get(topico);
    }

    public TelaPrincipal(Socket socket, BufferedReader in, PrintWriter out, String nome) {
        initComponents();
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.nome = nome;
        AllTopicos.setModel(modeloTopicos);
        AllTopicos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String topico = AllTopicos.getSelectedValue();
                    if (topico != null && !topico.contains(nome)) {
                        out.println("SUB " + topico);
                        System.out.println("Inscrito em: " + topico);
                    }
                    SubTopicos.setModel(modeloInscritos);
                    if (!modeloInscritos.contains(topico)) {
                        modeloInscritos.addElement(topico);
                    }
                }
            }
        });
        SubTopicos.setModel(modeloInscritos);
        SubTopicos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    topicoAtual = SubTopicos.getSelectedValue();
                    NomeDoTopico.setText(topicoAtual);
                    StringBuilder historico = mensagensPorTopico.get(topicoAtual);
                    System.out.println("Historico encontrado " + mensagensPorTopico.get(topicoAtual));
                    if (historico != null) {
                        jTextArea1.setText(historico.toString());
                    } else {
                        jTextArea1.setText("");
                    }
                }
            }
        });
        iniciarLeituraServidor();
        out.println("LIST");
    }

    private void receberChaveTopico(String resposta) {

        try {
            String[] partes = resposta.split(" ", 3);
            String topico = partes[1];

            String chaveCriptografada = partes[2];
            String caminho = "C:\\Users\\MURILO\\OneDrive\\Desktop\\Fase 5\\redes 2\\Trabalho_Redes\\src\\main\\java\\keys\\" + nome + "Pvd.key";
            PrivateKey minhaChave = ClienteHandler.carregarChavePrivada(caminho);

            byte[] chaveAES = CryptoRSA.descriptografar(chaveCriptografada, minhaChave);

            SecretKey chave = new SecretKeySpec(chaveAES, "AES");

            chavesTopicos.put(topico, chave);

            System.out.println("Chave recebida do tópico: " + topico);
            System.out.println("Recebida: " + resposta);
            System.out.println(chavesTopicos.keySet());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void iniciarLeituraServidor() {

        new Thread(() -> {

            try {

                String resposta;
                while ((resposta = in.readLine()) != null) {
                    System.out.println("Recebido " + resposta);
                    if (resposta.startsWith("TOPICOS:")) {
                        atualizarTopicos(resposta);
                    } else if (resposta.startsWith("TOPIC_KEY")) {

                        receberChaveTopico(resposta);
                    } else if (resposta.startsWith("INSCRICOES:")) {
                        atualizarInscricoes(resposta);
                    } else if (resposta.startsWith("[MSG]")) {
                        mostrarMensagemDoTopico(resposta);
                    } else {
                        System.out.println(resposta);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        ).start();
    }

    public void mostrarMensagemDoTopico(String msg) {

        try {

            int inicio = msg.indexOf('[', 5);
            int fim = msg.indexOf(']', inicio);

            if (inicio == -1 || fim == -1) {
                return;
            }
            String cabecalho = msg.substring(inicio + 1, fim);
            String[] partes = cabecalho.split(" ");
            String remetente = partes[0];
            String topico = partes[1];
            String criptografada = msg.substring(fim + 1).trim();
            SecretKey chave = chavesTopicos.get(topico);
            if (chave == null) {
                System.out.println("Sem chave para o tópico: " + topico);
                return;
            }
            String texto = CryptoAES.descriptografar(criptografada, chave);

            String mensagemFinal= "[MSG][" + remetente + " " + topico + "] " + texto;

            armazenarMensagens(topico, mensagemFinal);

            if (topico.equals(topicoAtual)) {

                SwingUtilities.invokeLater(() -> {
                    jTextArea1.append(mensagemFinal + "\n");
                });

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void atualizarInscricoes(String resposta) {

        SwingUtilities.invokeLater(() -> {
            modeloInscritos.clear();
            String dados = resposta.substring("INSCRICOES:".length());

            if (dados.isBlank()) {
                return;
            }

            String[] topicos = dados.split(",");

            for (String topico : topicos) {
                modeloInscritos.addElement(topico.trim());

            }

        });
    }

    private void atualizarTopicos(String resposta) {
        System.out.println("Atualizando lista: " + resposta);
        SwingUtilities.invokeLater(() -> {
            modeloTopicos.clear();
            String dados = resposta.substring(8);
            if (dados.isBlank()) {
                return;
            }
            String[] topicos = dados.split(",");
            for (String topico : topicos) {
                modeloTopicos.addElement(topico.trim());
            }

        });
    }

    private void armazenarMensagens(String topicoAtual, String msg) {
        System.out.println("Armazenando: " + topicoAtual + msg);
        mensagensPorTopico.putIfAbsent(topicoAtual, new StringBuilder());
        mensagensPorTopico.get(topicoAtual).append(msg).append("\n");

    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane4 = new javax.swing.JScrollPane();
        jTextPane2 = new javax.swing.JTextPane();
        jScrollBar1 = new javax.swing.JScrollBar();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        AllTopicos = new javax.swing.JList<>();
        jScrollPane2 = new javax.swing.JScrollPane();
        SubTopicos = new javax.swing.JList<>();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextPaneMensagem = new javax.swing.JTextPane();
        jScrollPane5 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jButton1 = new javax.swing.JButton();
        NomeDoTopico = new javax.swing.JLabel();
        Recarregar1 = new javax.swing.JButton();
        sair = new javax.swing.JButton();
        criarTopico = new javax.swing.JButton();

        jScrollPane4.setViewportView(jTextPane2);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jScrollPane1.setViewportView(AllTopicos);

        jScrollPane2.setViewportView(SubTopicos);

        jLabel1.setText("TOPICOS DISPONÍVEIS");

        jLabel2.setText("TOPICOS INSCRITOS");

        jScrollPane3.setViewportView(jTextPaneMensagem);

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane5.setViewportView(jTextArea1);

        jButton1.setText("ENVIAR");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        NomeDoTopico.setFont(new java.awt.Font("DialogInput", 1, 24)); // NOI18N
        NomeDoTopico.setText(" ");

        Recarregar1.setText("R");
        Recarregar1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Recarregar1ActionPerformed(evt);
            }
        });

        sair.setText("SAIR DO TOPICO");
        sair.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sairActionPerformed(evt);
            }
        });

        criarTopico.setText("CRIAR TOPICO");
        criarTopico.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                criarTopicoActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(Recarregar1))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(48, 48, 48))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addGap(24, 24, 24)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(sair, javax.swing.GroupLayout.PREFERRED_SIZE, 145, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jScrollPane5)
                        .addComponent(NomeDoTopico, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                            .addGap(0, 0, Short.MAX_VALUE)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 345, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                    .addComponent(criarTopico)
                                    .addGap(22, 22, 22)))))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(criarTopico, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel1)
                        .addComponent(Recarregar1)
                        .addComponent(sair)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(17, 17, 17)
                        .addComponent(NomeDoTopico)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 357, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(7, 7, 7)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jButton1)
                            .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 201, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(9, 9, 9)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 201, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(16, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        if (topicoAtual == null) {
            return;
        }

        try {

            SecretKey chave = chavesTopicos.get(topicoAtual);

            if (chave == null) {
                JOptionPane.showMessageDialog(this,
                        "Você ainda não recebeu a chave do tópico.");
                return;
            }
            String mensagem = jTextPaneMensagem.getText();

            String mensagemCriptografada = CryptoAES.criptografar(mensagem, chave);

            out.println("PUB " + topicoAtual + " " + mensagemCriptografada);

            jTextPaneMensagem.setText("");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }//GEN-LAST:event_jButton1ActionPerformed

    private void Recarregar1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_Recarregar1ActionPerformed
        out.println("LIST");
    }//GEN-LAST:event_Recarregar1ActionPerformed

    private void sairActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sairActionPerformed

        String topico = SubTopicos.getSelectedValue();

        if (topico != null) {
            out.println("UNSUB " + topico);

            DefaultListModel<String> modelo = (DefaultListModel<String>) SubTopicos.getModel();

            modelo.removeElement(topico);

            NomeDoTopico.setText("Nome do Topico");

            jTextArea1.setText("");
        }


    }//GEN-LAST:event_sairActionPerformed

    private void criarTopicoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_criarTopicoActionPerformed
        TelaCriarTopico tela = new TelaCriarTopico(out);
        tela.setVisible(true);
    }//GEN-LAST:event_criarTopicoActionPerformed

    /**
     * @param args the command line arguments
     */

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JList<String> AllTopicos;
    private javax.swing.JLabel NomeDoTopico;
    private javax.swing.JButton Recarregar1;
    private javax.swing.JList<String> SubTopicos;
    private javax.swing.JButton criarTopico;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollBar jScrollBar1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextPane jTextPane2;
    private javax.swing.JTextPane jTextPaneMensagem;
    private javax.swing.JButton sair;
    // End of variables declaration//GEN-END:variables
}
