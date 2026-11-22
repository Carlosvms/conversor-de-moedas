package br.com.alura.conversordemoedas.ui;

import br.com.alura.conversordemoedas.exception.ErroConsultaApiException;
import br.com.alura.conversordemoedas.model.Conversao;
import br.com.alura.conversordemoedas.service.ConversorService;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

/**
 * Interface de linha de comando (CLI) responsável por exibir os menus interativos,
 * capturar e validar as entradas do usuário e formatar as saídas no console.
 */
public class MenuPrincipal {

    private static final DateTimeFormatter FORMATADOR_DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final ConversorService conversorService;
    private final Scanner scanner;

    public MenuPrincipal(ConversorService conversorService) {
        this(conversorService, new Scanner(System.in));
    }

    public MenuPrincipal(ConversorService conversorService, Scanner scanner) {
        if (conversorService == null) {
            throw new IllegalArgumentException("O serviço de conversão não pode ser nulo.");
        }
        this.conversorService = conversorService;
        this.scanner = scanner != null ? scanner : new Scanner(System.in);
    }

    /**
     * Executa o loop principal da interface em terminal.
     */
    public void exibir() {
        boolean executando = true;

        exibirBoasVindas();

        while (executando) {
            exibirOpcoesMenu();

            System.out.print("Escolha uma opção válida [1-9]: ");
            String entrada = scanner.nextLine().trim();

            int opcao;
            try {
                opcao = Integer.parseInt(entrada);
            } catch (NumberFormatException e) {
                System.out.println("\n[!] Entrada inválida! Por favor, digite um número correspondente a uma das opções do menu.\n");
                continue;
            }

            // Encaminha para o fluxo correspondente à opção escolhida
            switch (opcao) {
                case 1 -> processarConversaoPredefinida("USD", "BRL", "Dólar Americano", "Real Brasileiro");
                case 2 -> processarConversaoPredefinida("BRL", "USD", "Real Brasileiro", "Dólar Americano");
                case 3 -> processarConversaoPredefinida("EUR", "BRL", "Euro", "Real Brasileiro");
                case 4 -> processarConversaoPredefinida("BRL", "EUR", "Real Brasileiro", "Euro");
                case 5 -> processarConversaoPredefinida("ARS", "BRL", "Peso Argentino", "Real Brasileiro");
                case 6 -> processarConversaoPredefinida("BRL", "ARS", "Real Brasileiro", "Peso Argentino");
                case 7 -> processarConversaoPersonalizada();
                case 8 -> exibirHistoricoSessao();
                case 9 -> {
                    executando = false;
                    exibirMensagemEncerramento();
                }
                default -> System.out.println("\n[!] Opção inexistente. Por favor, escolha um número de 1 a 9.\n");
            }
        }
    }

    private void exibirBoasVindas() {
        System.out.println("===============================================================");
        System.out.println("           BEM-VINDO AO CONVERSOR DE MOEDAS (ALURA / ONE)      ");
        System.out.println("===============================================================");
    }

    private void exibirOpcoesMenu() {
        System.out.println("---------------------------------------------------------------");
        System.out.println(" 1) Dólar (USD)          => Real Brasileiro (BRL)");
        System.out.println(" 2) Real Brasileiro (BRL)=> Dólar (USD)");
        System.out.println(" 3) Euro (EUR)           => Real Brasileiro (BRL)");
        System.out.println(" 4) Real Brasileiro (BRL)=> Euro (EUR)");
        System.out.println(" 5) Peso Argentino (ARS) => Real Brasileiro (BRL)");
        System.out.println(" 6) Real Brasileiro (BRL)=> Peso Argentino (ARS)");
        System.out.println(" 7) Conversão personalizada (informe as moedas)");
        System.out.println(" 8) Exibir histórico de conversões da sessão");
        System.out.println(" 9) Sair");
        System.out.println("---------------------------------------------------------------");
    }

    private void processarConversaoPredefinida(String origem, String destino, String nomeOrigem, String nomeDestino) {
        System.out.printf("\n>>> Conversão Selecionada: %s (%s) => %s (%s)%n", nomeOrigem, origem, nomeDestino, destino);
        Double valor = lerValorMonetario();
        if (valor == null) {
            return;
        }

        executarConversao(origem, destino, valor);
    }

    private void processarConversaoPersonalizada() {
        System.out.println("\n>>> Conversão Personalizada <<<");

        String origem = lerCodigoMoeda("Digite o código ISO da moeda de ORIGEM (ex: USD, BRL, EUR, GBP, JPY, CAD): ");
        if (origem == null) {
            return;
        }

        String destino = lerCodigoMoeda("Digite o código ISO da moeda de DESTINO (ex: USD, BRL, EUR, GBP, JPY, CAD): ");
        if (destino == null) {
            return;
        }

        Double valor = lerValorMonetario();
        if (valor == null) {
            return;
        }

        executarConversao(origem, destino, valor);
    }

    /**
     * Aciona o serviço de conversão tratando exceções para não interromper a execução do programa.
     */
    private void executarConversao(String origem, String destino, double valor) {
        try {
            System.out.println("Consultando cotação em tempo real na ExchangeRate-API...");
            Conversao conversao = conversorService.converter(origem, destino, valor);
            exibirResultadoConversao(conversao);
        } catch (ErroConsultaApiException e) {
            System.out.println("\n[ERRO NA CONSULTA]");
            System.out.println(e.getMessage());
            System.out.println();
        } catch (IllegalArgumentException e) {
            System.out.println("\n[ERRO DE VALIDAÇÃO] " + e.getMessage() + "\n");
        } catch (Exception e) {
            System.out.println("\n[ERRO INESPERADO] Ocorreu uma falha ao realizar a conversão: " + e.getMessage() + "\n");
        }
    }

    private void exibirResultadoConversao(Conversao conversao) {
        System.out.println("\n===============================================================");
        System.out.println("              CONVERSÃO REALIZADA COM SUCESSO                  ");
        System.out.println("===============================================================");
        System.out.printf("  Valor Original:    %s [%s]%n", formatarNumero(conversao.valorOriginal()), conversao.moedaOrigem());
        System.out.printf("  Taxa Utilizada:    1 %s = %.6f %s%n", conversao.moedaOrigem(), conversao.taxaUtilizada(), conversao.moedaDestino());
        System.out.printf("  Valor Convertido:  %s [%s]%n", formatarNumero(conversao.valorConvertido()), conversao.moedaDestino());
        System.out.printf("  Data e Hora:       %s%n", conversao.dataHora().format(FORMATADOR_DATA_HORA));
        System.out.println("===============================================================\n");
    }

    private void exibirHistoricoSessao() {
        System.out.println("\n===============================================================");
        System.out.println("              HISTÓRICO DE CONVERSÕES DA SESSÃO                ");
        System.out.println("===============================================================");

        List<Conversao> historico = conversorService.getHistorico();

        if (historico.isEmpty()) {
            System.out.println("  Nenhuma conversão foi realizada nesta sessão até o momento.");
            System.out.println("===============================================================\n");
            return;
        }

        System.out.printf("  Total de registros: %d conversão(ões)%n", historico.size());
        System.out.println("---------------------------------------------------------------");

        for (int i = 0; i < historico.size(); i++) {
            Conversao c = historico.get(i);
            System.out.printf("  %d) [%s] %s [%s] => %s [%s] (Taxa: %.6f)%n",
                    (i + 1),
                    c.dataHora().format(FORMATADOR_DATA_HORA),
                    formatarNumero(c.valorOriginal()),
                    c.moedaOrigem(),
                    formatarNumero(c.valorConvertido()),
                    c.moedaDestino(),
                    c.taxaUtilizada()
            );
        }
        System.out.println("===============================================================\n");
    }

    private void exibirMensagemEncerramento() {
        System.out.println("\n===============================================================");
        System.out.println("  Obrigado por utilizar o Conversor de Moedas!");
        System.out.println("  Programa finalizado com sucesso.");
        System.out.println("===============================================================");
    }

    /**
     * Lê e valida a quantia monetária inserida pelo usuário, aceitando ponto ou vírgula decimal.
     */
    private Double lerValorMonetario() {
        while (true) {
            System.out.print("Digite o valor a ser convertido (ou 'C' para cancelar): ");
            String entrada = scanner.nextLine().trim();

            if (entrada.equalsIgnoreCase("C")) {
                System.out.println("[Operação cancelada pelo usuário.]\n");
                return null;
            }

            if (entrada.isEmpty()) {
                System.out.println("[!] O valor não pode ficar em branco.");
                continue;
            }

            // Normaliza vírgula para ponto decimal
            String normalizada = entrada.replace(",", ".");

            try {
                double valor = Double.parseDouble(normalizada);
                if (valor <= 0) {
                    System.out.println("[!] O valor deve ser maior que zero. Tente novamente.");
                    continue;
                }
                return valor;
            } catch (NumberFormatException e) {
                System.out.println("[!] Valor inválido! Digite apenas números válidos (ex: 150.50 ou 150,50).");
            }
        }
    }

    /**
     * Lê e valida o código ISO da moeda garantindo o formato padrão de 3 letras alfabéticas.
     */
    private String lerCodigoMoeda(String prompt) {
        while (true) {
            System.out.print(prompt);
            String codigo = scanner.nextLine().trim().toUpperCase();

            if (codigo.equalsIgnoreCase("C")) {
                System.out.println("[Operação cancelada pelo usuário.]\n");
                return null;
            }

            // Validação de formato ISO 4217 (3 letras)
            if (codigo.length() < 3 || !codigo.matches("[A-Z]{3}")) {
                System.out.println("[!] Código inválido! O código ISO da moeda deve conter exatamente 3 letras (ex: USD, BRL, EUR).");
                continue;
            }

            return codigo;
        }
    }

    private String formatarNumero(double valor) {
        return String.format("%,.2f", valor);
    }
}
