package br.com.alura.conversordemoedas.service;

import br.com.alura.conversordemoedas.exception.ErroConsultaApiException;
import br.com.alura.conversordemoedas.model.MoedaResposta;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Serviço responsável pela comunicação HTTP com a ExchangeRate-API,
 * validação de autenticação e desserialização das respostas JSON.
 */
public class ConsultaCotacaoService {

    private static final String BASE_URL = "https://v6.exchangerate-api.com/v6/";
    private static final String NOME_VAR_AMBIENTE = "EXCHANGE_API_KEY";

    private final String apiKey;
    private final HttpClient httpClient;
    private final Gson gson;

    public ConsultaCotacaoService() {
        this(System.getenv(NOME_VAR_AMBIENTE));
    }

    public ConsultaCotacaoService(String apiKey) {
        this(
                apiKey,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build(),
                new Gson()
        );
    }

    public ConsultaCotacaoService(String apiKey, HttpClient httpClient, Gson gson) {
        this.apiKey = apiKey != null ? apiKey.trim() : null;
        this.httpClient = httpClient != null ? httpClient : HttpClient.newHttpClient();
        this.gson = gson != null ? gson : new Gson();
    }

    /**
     * Consulta as taxas de câmbio em tempo real para a moeda base informada.
     *
     * @param moedaBase Código ISO da moeda base (ex: USD, BRL)
     * @return MoedaResposta contendo status e taxas de conversão
     */
    public MoedaResposta consultarCotacao(String moedaBase) {
        validarChaveApi();

        if (moedaBase == null || moedaBase.isBlank()) {
            throw new ErroConsultaApiException("O código da moeda base não pode ser nulo ou vazio.");
        }

        String codigoMoedaFormatado = moedaBase.trim().toUpperCase();
        String urlFinal = BASE_URL + this.apiKey + "/latest/" + codigoMoedaFormatado;

        // Configuração da requisição HTTP GET com timeout e cabeçalho Accept
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(urlFinal))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
        } catch (IllegalArgumentException e) {
            throw new ErroConsultaApiException("URI de requisição malformada: " + e.getMessage(), e);
        }

        try {
            // Executa a chamada HTTP e obtém a resposta como String
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();

            // Mapeamento e tratamento de status de erro HTTP
            if (statusCode == 401 || statusCode == 403) {
                throw new ErroConsultaApiException(
                        "Falha de autenticação na API (HTTP " + statusCode + "). " +
                        "A chave informada na variável '" + NOME_VAR_AMBIENTE + "' é inválida ou não possui permissões."
                );
            }

            if (statusCode == 404) {
                throw new ErroConsultaApiException(
                        "Endpoint ou recurso não encontrado (HTTP 404). Verifique se o código da moeda '" +
                        codigoMoedaFormatado + "' é válido."
                );
            }

            if (statusCode >= 500) {
                throw new ErroConsultaApiException(
                        "O servidor da ExchangeRate-API apresentou instabilidade (HTTP " + statusCode + "). " +
                        "Tente novamente em instantes."
                );
            }

            String corpoResposta = response.body();
            if (corpoResposta == null || corpoResposta.isBlank()) {
                throw new ErroConsultaApiException("A resposta recebida da API está vazia.");
            }

            // Desserialização do JSON para o record MoedaResposta
            MoedaResposta moedaResposta = this.gson.fromJson(corpoResposta, MoedaResposta.class);

            if (moedaResposta == null) {
                throw new ErroConsultaApiException("Não foi possível desserializar a resposta da API.");
            }

            // Validação de erro informado no próprio payload da resposta da API
            if (!moedaResposta.isSucesso()) {
                String motivoErro = moedaResposta.errorType() != null ? moedaResposta.errorType() : "erro desconhecido";
                throw new ErroConsultaApiException("Erro retornado pela ExchangeRate-API: " + traduzirMotivoErro(motivoErro));
            }

            return moedaResposta;

        } catch (IOException e) {
            throw new ErroConsultaApiException(
                    "Erro de comunicação de rede com o serviço de cotação: " + e.getMessage(), e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ErroConsultaApiException("A requisição à API foi interrompida.", e);
        } catch (JsonSyntaxException e) {
            throw new ErroConsultaApiException("Falha na interpretação do JSON retornado pela API: " + e.getMessage(), e);
        }
    }

    /**
     * Valida se a chave de autenticação da API foi fornecida via variável de ambiente ou construtor.
     */
    private void validarChaveApi() {
        if (this.apiKey == null || this.apiKey.isBlank()) {
            throw new ErroConsultaApiException(
                    """
                    Chave da API não encontrada!
                    Por favor, configure a variável de ambiente 'EXCHANGE_API_KEY' com sua chave da ExchangeRate-API.
                    
                    Instruções:
                      PowerShell: $env:EXCHANGE_API_KEY = "sua_chave_aqui"
                      CMD:        set EXCHANGE_API_KEY=sua_chave_aqui
                      Linux/macOS: export EXCHANGE_API_KEY="sua_chave_aqui"
                      IntelliJ:   Run > Edit Configurations > Environment variables: EXCHANGE_API_KEY=sua_chave_aqui
                    """
            );
        }
    }

    /**
     * Traduz os códigos de erro conhecidos da ExchangeRate-API para mensagens amigáveis em português.
     */
    private String traduzirMotivoErro(String errorType) {
        return switch (errorType) {
            case "unsupported-code" -> "Código de moeda não suportado ou inexistente.";
            case "malformed-request" -> "A requisição enviada à API possui formato inválido.";
            case "invalid-key" -> "A chave da API fornecida é inválida.";
            case "inactive-account" -> "A conta associada a esta chave de API está inativa.";
            case "quota-reached" -> "A cota limite de requisições do seu plano foi atingida.";
            default -> errorType;
        };
    }
}
