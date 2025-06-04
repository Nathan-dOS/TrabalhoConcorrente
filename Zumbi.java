import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa um elemento do tipo Zumbi.
 * Lógica de movimento com bias progressivo implementada.
 */
public class Zumbi extends Elemento {
    private final Random random = new Random();
    // Constantes de Bias (iguais ao Azul, mas direção preferida é Esquerda)
    private static final double PROB_INICIAL_DIRECAO = 1.0 / 8.0; 
    private static final double INCREMENTO_BIAS_POR_SEGUNDO = 0.005; 
    private static final double MAX_PROB_BIAS = 0.40; 

    public Zumbi(int x, int y, Tabuleiro tabuleiro) {
        super(x, y, tabuleiro, 2); // Tipo 2 para Zumbi
    }

    @Override
    public void run() {
        while (!tabuleiro.isJogoAcabou() && !Thread.currentThread().isInterrupted()) {
            Lock currentLock = null;
            try {
                // Tempo aleatório entre movimentos
                Thread.sleep(random.nextInt(901) + 100);

                if (tabuleiro.isJogoAcabou() || Thread.currentThread().isInterrupted()) break;

                int currentX = this.x;
                int currentY = this.y;

                // 1. Adquirir lock da posição atual
                currentLock = tabuleiro.getLock(currentX, currentY);
                if (currentLock == null) continue;
                currentLock.lock();

                // Verificar se ainda estamos na célula
                if (tabuleiro.getPosicao(currentX, currentY) != this.tipo || Thread.currentThread().isInterrupted()) {
                    break; // Fomos removidos ou interrompidos
                }

                // --- Tentar Mover com Bias --- 
                boolean moved = tentarMoverComBias(currentX, currentY);
                
                // --- Verificar Vizinhos Pós-Movimento (ou se não moveu) ---
                int checkX = this.x;
                int checkY = this.y;
                
                if (!tabuleiro.isJogoAcabou() && !Thread.currentThread().isInterrupted()) {
                    verificarVizinhosParaConversao(checkX, checkY);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Thread Zumbi ID " + getId() + " interrompida.");
                break;
            } finally {
                // Liberar lock da posição atual (se adquirido)
                if (currentLock != null && ((ReentrantLock)currentLock).isHeldByCurrentThread()) {
                    currentLock.unlock();
                }
            }
        }
        System.out.println("Thread Zumbi ID " + getId() + " terminando.");
    }

    // Tenta mover o elemento com bias progressivo. Retorna true se moveu, false caso contrário.
    // Assume que o lock da célula atual (currentX, currentY) JÁ ESTÁ ADQUIRIDO.
    private boolean tentarMoverComBias(int currentX, int currentY) {
        long segundosPassados = tabuleiro.getSegundosPassados();
        // Direção preferida do Zumbi é Esquerda (dy = -1)
        double probEsquerda = Math.min(MAX_PROB_BIAS, PROB_INICIAL_DIRECAO + INCREMENTO_BIAS_POR_SEGUNDO * segundosPassados);
        double probOutras = (1.0 - probEsquerda) / 7.0; 

        int dx = 0;
        int dy = 0;
        boolean direcaoPreferidaEscolhida = false;

        // Tenta a direção preferida (esquerda)
        if (random.nextDouble() < probEsquerda) {
            dx = 0;
            dy = -1; // Direção preferida do Zumbi
            direcaoPreferidaEscolhida = true;
            int novoX = currentX + dx;
            int novoY = currentY + dy;
            if (tabuleiro.tentarMoverElemento(currentX, currentY, novoX, novoY, this)) {
                return true; // Moveu na direção preferida
            }
            // Se não conseguiu mover na direção preferida, tentará outra aleatória abaixo
        }

        // Se não escolheu/conseguiu mover na direção preferida, tenta uma das outras 7
        List<int[]> outrasDirecoes = new ArrayList<>();
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) continue; // Ignora ficar parado
                if (i == 0 && j == -1 && direcaoPreferidaEscolhida) continue; // Já tentou a preferida (esquerda)
                outrasDirecoes.add(new int[]{i, j});
            }
        }
        Collections.shuffle(outrasDirecoes, random); // Randomiza a ordem das outras direções

        for (int[] dir : outrasDirecoes) {
            dx = dir[0];
            dy = dir[1];
            int novoX = currentX + dx;
            int novoY = currentY + dy;
            if (tabuleiro.tentarMoverElemento(currentX, currentY, novoX, novoY, this)) {
                return true; // Moveu em outra direção
            }
        }

        return false; // Não conseguiu mover em nenhuma direção
    }
    
    // Verifica vizinhos em busca de Azuis para requisitar conversão
    private void verificarVizinhosParaConversao(int currentX, int currentY) {
        int[] dx = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dy = {-1, 0, 1, -1, 1, -1, 0, 1};

        for (int i = 0; i < 8; i++) {
            int nx = currentX + dx[i];
            int ny = currentY + dy[i];

            if (tabuleiro.isDentroDosLimites(nx, ny)) {
                if (tabuleiro.getPosicao(nx, ny) == 1) {
                    Elemento azulAlvo = encontrarElementoEm(nx, ny, 1);
                    if (azulAlvo != null) {
                         System.out.println("Zumbi ID " + getId() + " detectou Azul em (" + nx + "," + ny + ") e requisitará conversão.");
                         tabuleiro.requisitarConversao(azulAlvo);
                         return; // Requisita para o primeiro encontrado e sai
                    } else {
                         System.err.println("Zumbi ID " + getId() + " detectou Azul em (" + nx + "," + ny + ") mas não encontrou o objeto Elemento!");
                    }
                }
            }
        }
    }
    
    // Método auxiliar para encontrar um elemento específico em uma posição
    private Elemento encontrarElementoEm(int x, int y, int tipo) {
        synchronized (tabuleiro.elementos) { 
            for (Elemento e : tabuleiro.elementos) {
                if (e.getXPos() == x && e.getYPos() == y && e.getTipo() == tipo && e.isAlive()) {
                    return e;
                }
            }
        }
        return null;
    }
}
