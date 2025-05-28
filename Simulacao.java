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
        
        System.out.print("Digite a quantidade inicial de elementos Azuis: ");
        int numAzuis = 5; 
        System.out.print("Digite a quantidade inicial de elementos Zumbis: ");
        int numZumbis = 5; 
        scanner.close();

        System.out.println("Iniciando simulação com " + numAzuis + " Azuis e " + numZumbis + " Zumbis.");

        List<Elemento> elementosParaIniciar = new ArrayList<>();

        // --- Posicionamento Inicial Aleatório ---

        // Posiciona Azuis (lado esquerdo)
        for (int i = 0; i < numAzuis; i++) {
            int x, y;
            do {
                x = random.nextInt(altura);
                y = 0;
            } while (tabuleiro.getPosicao(x, y) != 0); 
            Azul azul = new Azul(x, y, tabuleiro);
            tabuleiro.adicionarElemento(azul);
            elementosParaIniciar.add(azul);
        }

        // Posiciona Zumbis (lado direito)
        for (int i = 0; i < numZumbis; i++) {
            int x, y;
            do {
                x = random.nextInt(altura);
                y = 50;
            } while (tabuleiro.getPosicao(x, y) != 0); // Garante posição vazia
            Zumbi zumbi = new Zumbi(x, y, tabuleiro);
            tabuleiro.adicionarElemento(zumbi);
            elementosParaIniciar.add(zumbi);
        }

        System.out.println("Elementos posicionados. Iniciando threads...");
        tabuleiro.imprimirTabuleiro(); 

        // --- Inicia as Threads ---
        for (Elemento e : elementosParaIniciar) {
            e.start();
        }

        // --- Loop Principal da Simulação (Monitoramento) ---
        while (!tabuleiro.isJogoAcabou()) {
            try {
                Thread.sleep(1000); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Thread principal da simulação interrompida.");
                break;
            }
        }

        // --- Fim da Simulação ---
        System.out.println("Simulação encerrada.");
        System.out.println("Resultado: " + tabuleiro.getMensagemFim());

        // Esperar as threads terminarem (opcional, mas boa prática)
        for (Elemento e : elementosParaIniciar) {
            try {
                e.join(); // Espera a thread terminar
            } catch (InterruptedException ex) {
                // Ignorar ou logar
            }
        }
        System.out.println("Todas as threads de elementos finalizaram.");
    }
}

