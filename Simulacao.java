import java.util.Scanner;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe principal que inicia e gerencia a simulação.
 */
public class Simulacao {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Random random = new Random();

        // --- Configuração Inicial ---
        int altura = 50;
        int largura = 50;
        Tabuleiro tabuleiro = new Tabuleiro(altura, largura);

        System.out.println("--- Configuração da Simulação ---");
        System.out.print("Digite a quantidade inicial de elementos Azuis: ");
        int numAzuis;
        try {
            numAzuis = scanner.nextInt();
            if (numAzuis > altura) {
                System.out.println("Aviso: Número de Azuis maior que a altura do tabuleiro. Reduzindo para " + altura);
                numAzuis = altura;
            }
        } catch (Exception e) {
            numAzuis = 5; // Valor padrão se houver erro de entrada
            System.out.println("Entrada inválida. Usando valor padrão: " + numAzuis);
        }
        
        System.out.print("Digite a quantidade inicial de elementos Zumbis: ");
        int numZumbis;
        try {
            numZumbis = scanner.nextInt();
            if (numZumbis > altura) {
                System.out.println("Aviso: Número de Zumbis maior que a altura do tabuleiro. Reduzindo para " + altura);
                numZumbis = altura;
            }
        } catch (Exception e) {
            numZumbis = 5; // Valor padrão se houver erro de entrada
            System.out.println("Entrada inválida. Usando valor padrão: " + numZumbis);
        }
        scanner.close(); // Fechar scanner após uso

        System.out.println("Iniciando simulação com " + numAzuis + " Azuis e " + numZumbis + " Zumbis.");

        List<Elemento> elementosParaIniciar = new ArrayList<>();

        // --- Posicionamento Inicial (Nova Regra) ---

        // Posiciona Azuis (coluna y = 0)
        System.out.println("Posicionando Azuis na coluna 0...");
        for (int i = 0; i < numAzuis; i++) {
            int x;
            int y = 0; // Coluna inicial dos Azuis
            do {
                x = random.nextInt(altura); // Linha aleatória
            } while (tabuleiro.getPosicao(x, y) != 0); // Garante posição vazia na coluna 0
            Azul azul = new Azul(x, y, tabuleiro);
            tabuleiro.adicionarElemento(azul);
            elementosParaIniciar.add(azul);
            System.out.println("Azul " + (i+1) + " posicionado em (" + x + "," + y + ")");
        }

        // Posiciona Zumbis (coluna y = largura - 1)
        System.out.println("Posicionando Zumbis na coluna " + (largura - 1) + "...");
        for (int i = 0; i < numZumbis; i++) {
            int x;
            int y = largura - 1; // Coluna inicial dos Zumbis
            do {
                x = random.nextInt(altura);
            } while (tabuleiro.getPosicao(x, y) != 0); // Garante posição vazia na última coluna
            Zumbi zumbi = new Zumbi(x, y, tabuleiro);
            tabuleiro.adicionarElemento(zumbi);
            elementosParaIniciar.add(zumbi);
            System.out.println("Zumbi " + (i+1) + " posicionado em (" + x + "," + y + ")");
        }

        System.out.println("Elementos posicionados. Iniciando threads...");
        tabuleiro.imprimirTabuleiro(); // Mostra estado inicial

        // --- Inicia as Threads ---
        for (Elemento e : elementosParaIniciar) {
            e.start();
        }

        // --- Loop Principal da Simulação (Monitoramento) ---
        int iteracao = 0;
        while (!tabuleiro.JogoAcabou()) {
            try {
                Thread.sleep(500); // Pausa para verificar o estado (a cada meio segundo)

                // Imprimir tabuleiro e estatísticas periodicamente
                if (iteracao % 5 == 0 && iteracao > 0) { // A cada 5 segundos (exceto no início)
                    System.out.println("\n--- Iteração " + iteracao + " ---");
                    tabuleiro.imprimirTabuleiro();
                    System.out.println(tabuleiro.getEstatisticas());
                }
                
                iteracao++;
                
                // Verificar se a simulação está demorando muito (opcional)
                if (iteracao > 300) { // 5 minutos
                    System.out.println("Simulação atingiu tempo limite (300s).");
                    tabuleiro.terminarJogo("Tempo limite atingido.");
                    break;
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Thread principal da simulação interrompida.");
                break;
            }
        }

        // --- Fim da Simulação ---
        System.out.println("\nSimulação encerrada após " + iteracao + " segundos.");
        System.out.println("Resultado: " + tabuleiro.getMensagemFim());
        tabuleiro.imprimirTabuleiro(); // Mostra estado final
        System.out.println(tabuleiro.getEstatisticas());

        // Esperar as threads terminarem (opcional, mas boa prática)
        System.out.println("Aguardando finalização das threads...");
        for (Elemento e : elementosParaIniciar) {
            try {
                e.join(1000); // Espera a thread terminar com timeout de 1 segundo
            } catch (InterruptedException ex) {
                // Ignorar ou logar
            }
        }
        System.out.println("Todas as threads de elementos finalizaram ou timeout atingido.");
    }
}
