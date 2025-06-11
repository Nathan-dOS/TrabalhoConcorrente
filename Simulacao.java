import javafx.application.Application;
import java.util.Scanner;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

public class Simulacao {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Random random = new Random();

        // --- Configuração Inicial ---
        int altura = 50;
        int largura = 50;
        Tabuleiro tabuleiro = new Tabuleiro(altura, largura);

        System.out.println("--- Configuração da Simulação ---");
        System.out.print("Digite a quantidade inicial de elementos Azuis (máx " + altura + "): ");
        int numAzuis = lerInteiro(scanner, 5, altura);
        
        System.out.print("Digite a quantidade inicial de elementos Zumbis (máx " + altura + "): ");
        int numZumbis = lerInteiro(scanner, 5, altura);
        scanner.close();

        System.out.println("Configurando simulação com " + numAzuis + " Azuis e " + numZumbis + " Zumbis.");

        List<Elemento> elementosParaIniciar = new ArrayList<>();

        // --- Posicionamento Inicial ---
        // Posiciona Azuis (coluna y = 0)
        for (int i = 0; i < numAzuis; i++) {
            int x;
            int y = 0;
            do {
                x = random.nextInt(altura);
            } while (tabuleiro.getPosicao(x, y) != 0); // Garante posição vazia
            Azul azul = new Azul(x, y, tabuleiro);
            tabuleiro.adicionarElementoInicial(azul);
            elementosParaIniciar.add(azul);
        }

        // Posiciona Zumbis (coluna y = largura - 1)
        for (int i = 0; i < numZumbis; i++) {
            int x;
            int y = largura - 1;
            do {
                x = random.nextInt(altura);
            } while (tabuleiro.getPosicao(x, y) != 0); // Garante posição vazia
            Zumbi zumbi = new Zumbi(x, y, tabuleiro);
            tabuleiro.adicionarElementoInicial(zumbi);
            elementosParaIniciar.add(zumbi);
        }

        System.out.println("Elementos posicionados. Passando dados para a GUI e iniciando...");

        // --- Passa os dados para a classe GUI e lança --- 
        SimulacaoGUI.setTabuleiro(tabuleiro);
        SimulacaoGUI.setElementos(elementosParaIniciar);
        
        // Lança a aplicação JavaFX. Isso bloqueará até a GUI ser fechada.
        Application.launch(SimulacaoGUI.class, args);
        
        // Código após o fechamento da GUI (se necessário)
        System.out.println("\nSimulação (e GUI) encerrada.");
        if (tabuleiro.isJogoAcabou()) {
            System.out.println("Resultado final: " + tabuleiro.getMensagemFim());
            System.out.println(tabuleiro.getEstatisticas());
        } else {
            System.out.println("Simulação interrompida antes do fim.");
        }
    }
    
    // Helper para ler inteiro com valor padrão e limite
    private static int lerInteiro(Scanner scanner, int padrao, int maximo) {
        int valor;
        try {
            valor = scanner.nextInt();
            if (valor < 0) {
                System.out.println("Valor negativo inválido. Usando padrão: " + padrao);
                valor = padrao;
            } else if (valor > maximo) {
                System.out.println("Valor excede o máximo (" + maximo + "). Reduzindo para " + maximo);
                valor = maximo;
            }
        } catch (Exception e) {
            valor = padrao; 
            System.out.println("Entrada inválida. Usando valor padrão: " + padrao);
            // Limpa o buffer do scanner em caso de erro de tipo
            if (scanner.hasNextLine()) scanner.nextLine(); 
        }
        return valor;
    }
}

