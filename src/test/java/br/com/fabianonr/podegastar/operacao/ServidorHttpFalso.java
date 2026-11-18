package br.com.fabianonr.podegastar.operacao;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Servidor HTTP falso para os testes dos scripts de operação. Cada conexão recebe o próximo
 * comportamento do roteiro; o último se repete nas conexões seguintes. Usa socket puro para poder
 * simular 1xx, conexão fechada sem resposta e resposta sem TLS.
 */
final class ServidorHttpFalso implements AutoCloseable {

  private final ServerSocket socketDoServidor;
  private final List<Comportamento> roteiro;
  private final List<Atendimento> atendimentos = new CopyOnWriteArrayList<>();

  private ServidorHttpFalso(ServerSocket socketDoServidor, List<Comportamento> roteiro) {
    this.socketDoServidor = socketDoServidor;
    this.roteiro = List.copyOf(roteiro);
    Thread.ofVirtual().start(this::aceitarConexoes);
  }

  static ServidorHttpFalso comRoteiro(Comportamento... roteiro) {
    return naPorta(0, roteiro);
  }

  static ServidorHttpFalso naPorta(int porta, Comportamento... roteiro) {
    try {
      var socket = new ServerSocket(porta, 50, InetAddress.getLoopbackAddress());
      return new ServidorHttpFalso(socket, List.of(roteiro));
    } catch (IOException erro) {
      throw new UncheckedIOException(erro);
    }
  }

  /** Porta livre no momento da chamada, sem ninguém escutando nela. */
  static int portaLivre() {
    try (var socket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
      return socket.getLocalPort();
    } catch (IOException erro) {
      throw new UncheckedIOException(erro);
    }
  }

  static Comportamento responder(int status, String corpo) {
    return new Responder(Duration.ZERO, status, Map.of(), corpo);
  }

  static Comportamento responderDepoisDe(Duration atraso, int status, String corpo) {
    return new Responder(atraso, status, Map.of(), corpo);
  }

  static Comportamento redirecionarPara(int status, String destino) {
    return new Responder(Duration.ZERO, status, Map.of("Location", destino), "");
  }

  static Comportamento fecharSemResposta() {
    return new FecharSemResposta();
  }

  static Comportamento informativoEFechar() {
    return new InformativoEFechar();
  }

  static Comportamento respostaSemTls() {
    return new RespostaSemTls();
  }

  static Comportamento healthComCommit(String commit) {
    return responder(200, corpoDoHealth(commit));
  }

  static Comportamento healthComCommitDepoisDe(Duration atraso, String commit) {
    return responderDepoisDe(atraso, 200, corpoDoHealth(commit));
  }

  private static String corpoDoHealth(String commit) {
    return "{\"status\": \"UP\", \"commit\": \"" + commit + "\"}";
  }

  String endereco() {
    return "http://127.0.0.1:" + socketDoServidor.getLocalPort();
  }

  int porta() {
    return socketDoServidor.getLocalPort();
  }

  /** Pedidos na ordem em que as conexões foram aceitas, inclusive os ainda em atendimento. */
  List<PedidoRecebido> pedidos() {
    return atendimentos.stream().map(Atendimento::comoPedido).toList();
  }

  @Override
  public void close() throws IOException {
    socketDoServidor.close();
  }

  private void aceitarConexoes() {
    while (!socketDoServidor.isClosed()) {
      try {
        var conexao = socketDoServidor.accept();
        var atendimento = new Atendimento(System.nanoTime());
        var comportamento = roteiro.get(Math.min(atendimentos.size(), roteiro.size() - 1));
        atendimentos.add(atendimento);
        Thread.ofVirtual().start(() -> atender(conexao, atendimento, comportamento));
      } catch (IOException servidorFechado) {
        return;
      }
    }
  }

  private static void atender(
      Socket conexao, Atendimento atendimento, Comportamento comportamento) {
    try (conexao) {
      var saida = conexao.getOutputStream();
      if (!(comportamento instanceof RespostaSemTls)) {
        lerPedido(new BufferedInputStream(conexao.getInputStream()), atendimento);
      }
      switch (comportamento) {
        case Responder resposta -> {
          Thread.sleep(resposta.atraso());
          escrever(saida, respostaCompleta(resposta));
        }
        case InformativoEFechar informativo -> escrever(saida, "HTTP/1.1 100 Continue\r\n\r\n");
        case RespostaSemTls semTls ->
            escrever(saida, respostaCompleta(new Responder(Duration.ZERO, 200, Map.of(), "{}")));
        case FecharSemResposta fechar -> {
          // Fecha a conexão sem escrever nada.
        }
      }
    } catch (IOException conexaoEncerradaPeloCliente) {
      // O cliente desistiu (tempo limite, erro de TLS); o atendimento termina aqui.
    } catch (InterruptedException interrompido) {
      Thread.currentThread().interrupt();
    } finally {
      atendimento.fimEmNanos = System.nanoTime();
    }
  }

  private static void lerPedido(InputStream entrada, Atendimento atendimento) throws IOException {
    var linhaDoPedido = lerLinha(entrada).split(" ");
    var cabecalhos = new HashMap<String, String>();
    for (var linha = lerLinha(entrada); !linha.isEmpty(); linha = lerLinha(entrada)) {
      var separador = linha.indexOf(':');
      cabecalhos.put(
          linha.substring(0, separador).trim().toLowerCase(Locale.ROOT),
          linha.substring(separador + 1).trim());
    }
    var tamanhoDoCorpo = Integer.parseInt(cabecalhos.getOrDefault("content-length", "0"));
    atendimento.metodo = linhaDoPedido[0];
    atendimento.caminho = linhaDoPedido[1];
    atendimento.cabecalhos = Map.copyOf(cabecalhos);
    atendimento.corpo = new String(entrada.readNBytes(tamanhoDoCorpo), StandardCharsets.UTF_8);
  }

  private static String lerLinha(InputStream entrada) throws IOException {
    var linha = new ByteArrayOutputStream();
    for (var caractere = entrada.read(); caractere != '\n'; caractere = entrada.read()) {
      if (caractere == -1) {
        throw new IOException("conexão encerrada antes do fim do pedido");
      }
      if (caractere != '\r') {
        linha.write(caractere);
      }
    }
    return linha.toString(StandardCharsets.ISO_8859_1);
  }

  private static String respostaCompleta(Responder resposta) {
    var bytesDoCorpo = resposta.corpo().getBytes(StandardCharsets.UTF_8);
    var texto = new StringBuilder("HTTP/1.1 " + resposta.status() + " Teste\r\n");
    texto.append("Content-Type: application/json\r\n");
    texto.append("Content-Length: ").append(bytesDoCorpo.length).append("\r\n");
    texto.append("Connection: close\r\n");
    resposta
        .cabecalhos()
        .forEach((nome, valor) -> texto.append(nome).append(": ").append(valor).append("\r\n"));
    return texto.append("\r\n").append(resposta.corpo()).toString();
  }

  private static void escrever(OutputStream saida, String conteudo) throws IOException {
    saida.write(conteudo.getBytes(StandardCharsets.UTF_8));
    saida.flush();
  }

  sealed interface Comportamento
      permits Responder, FecharSemResposta, InformativoEFechar, RespostaSemTls {}

  private record Responder(
      Duration atraso, int status, Map<String, String> cabecalhos, String corpo)
      implements Comportamento {}

  private record FecharSemResposta() implements Comportamento {}

  private record InformativoEFechar() implements Comportamento {}

  private record RespostaSemTls() implements Comportamento {}

  /** Estado de uma conexão; preenchido pela thread que a atende. */
  private static final class Atendimento {
    private final long inicioEmNanos;
    private volatile String metodo = "";
    private volatile String caminho = "";
    private volatile Map<String, String> cabecalhos = Map.of();
    private volatile String corpo = "";
    private volatile long fimEmNanos;

    private Atendimento(long inicioEmNanos) {
      this.inicioEmNanos = inicioEmNanos;
    }

    private PedidoRecebido comoPedido() {
      return new PedidoRecebido(metodo, caminho, cabecalhos, corpo, inicioEmNanos, fimEmNanos);
    }
  }

  /**
   * Pedido recebido, com os instantes (System.nanoTime) do aceite da conexão e do fim do
   * atendimento. O fim vale zero enquanto o atendimento não termina.
   */
  record PedidoRecebido(
      String metodo,
      String caminho,
      Map<String, String> cabecalhos,
      String corpo,
      long inicioEmNanos,
      long fimEmNanos) {

    String cabecalho(String nome) {
      return cabecalhos.get(nome.toLowerCase(Locale.ROOT));
    }
  }
}
