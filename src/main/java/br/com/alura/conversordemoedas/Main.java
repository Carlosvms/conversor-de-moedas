package br.com.alura.conversordemoedas;

import br.com.alura.conversordemoedas.service.ConsultaCotacaoService;
import br.com.alura.conversordemoedas.service.ConversorService;
import br.com.alura.conversordemoedas.ui.MenuPrincipal;

/**
 * Ponto de entrada da aplicação.
 * Responsável por instanciar os serviços e iniciar a interface com o usuário.
 */
public class Main {

    public static void main(String[] args) {
        // Inicialização e injeção de dependências dos serviços e interface
        ConsultaCotacaoService consultaCotacaoService = new ConsultaCotacaoService();
        ConversorService conversorService = new ConversorService(consultaCotacaoService);
        MenuPrincipal menuPrincipal = new MenuPrincipal(conversorService);

        // Inicializa o menu interativo no console
        menuPrincipal.exibir();
    }
}
