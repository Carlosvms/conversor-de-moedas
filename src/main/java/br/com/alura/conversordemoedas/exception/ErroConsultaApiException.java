package br.com.alura.conversordemoedas.exception;

/**
 * Exceção personalizada para representar falhas ocorridas durante
 * a comunicação com a API de cotação (rede, autenticação, deserialização ou respostas de erro).
 */
public class ErroConsultaApiException extends RuntimeException {

    public ErroConsultaApiException(String message) {
        super(message);
    }

    public ErroConsultaApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
