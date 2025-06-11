import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class Azul extends Elemento {
    private final Random random = new Random();
    private static final double PROB_INICIAL_DIRECAO = 1.0 / 8.0; // Chance igual inicial
    private static final double INCREMENTO_BIAS_POR_SEGUNDO = 0.005; // 0.5% por segundo
    private static final double MAX_PROB_BIAS = 0.40; // Máximo de 40%

    public Azul(int x, int y, Tabuleiro tabuleiro) {
        super(x, y, tabuleiro, 1); // Tipo 1 para Azul
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

                // Adquire lock da posição atual
                currentLock = tabuleiro.getLock(currentX, currentY);
                if (currentLock == null) continue;
                currentLock.lock();

                // Verificar se ainda estão na célula 
                if (tabuleiro.getPosicao(currentX, currentY) != this.tipo || Thread.currentThread().isInterrupted()) {
                    break; // Fomos convertidos ou interrompidos
                }

                // --- Tentar Mover com Bias --- 
                boolean moved = tentarMoverComBias(currentX, currentY);
                
                // --- Verificar Vizinhos Pós-Movimento (ou se não moveu) ---
                int checkX = this.x;
                int checkY = this.y;
                
                if (!tabuleiro.isJogoAcabou() && !Thread.currentThread().isInterrupted()) {
                    verificarVizinhosParaAutoConversao(checkX, checkY);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Thread Azul ID " + getId() + " interrompida.");
                break;
            } finally {
                // Liberar lock da posição atual (se adquirido)
                if (currentLock != null && ((ReentrantLock)currentLock).isHeldByCurrentThread()) {
                    currentLock.unlock();
                }
            }
        }
        System.out.println("Thread Azul ID " + getId() + " terminando.");
    }

    // Tenta mover o elemento com bias progressivo.
    private boolean tentarMoverComBias(int currentX, int currentY) {
        long segundosPassados = tabuleiro.getSegundosPassados();
        double probDireita = Math.min(MAX_PROB_BIAS, PROB_INICIAL_DIRECAO + INCREMENTO_BIAS_POR_SEGUNDO * segundosPassados);
        double probOutras = (1.0 - probDireita) / 7.0; // Probabilidade para cada uma das outras 7 direções

        int dx = 0;
        int dy = 0;
        boolean direcaoPreferidaEscolhida = false;

        // Tenta a direção preferida (direita)
        if (random.nextDouble() < probDireita) {
            dx = 0;
            dy = 1;
            direcaoPreferidaEscolhida = true;
            int novoX = currentX + dx;
            int novoY = currentY + dy;
            if (tabuleiro.tentarMoverElemento(currentX, currentY, novoX, novoY, this)) {
                if (this.y == tabuleiro.getLargura() - 1) {
                    tabuleiro.terminarJogo("Azul ID " + getId() + " venceu! Chegou à borda direita.");
                }
                return true; // Moveu na direção preferida
            }
            // Se não conseguiu mover na direção preferida, tentará outra aleatória abaixo
        }

        // Se não escolheu/conseguiu mover na direção preferida, tenta uma das outras 7
        List<int[]> outrasDirecoes = new ArrayList<>();
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) continue; // Ignora ficar parado
                if (i == 0 && j == 1 && direcaoPreferidaEscolhida) continue; // Já tentou a preferida
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
                 if (this.y == tabuleiro.getLargura() - 1) {
                    tabuleiro.terminarJogo("Azul ID " + getId() + " venceu! Chegou à borda direita.");
                }
                return true; // Moveu em outra direção
            }
        }

        return false; // Não conseguiu mover em nenhuma direção
    }
    
    // Verifica vizinhos em busca de Zumbis para requisitar auto-conversão
    private void verificarVizinhosParaAutoConversao(int currentX, int currentY) {
        int[] dx = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dy = {-1, 0, 1, -1, 1, -1, 0, 1};

        for (int i = 0; i < 8; i++) {
            int nx = currentX + dx[i];
            int ny = currentY + dy[i];

            if (tabuleiro.isDentroDosLimites(nx, ny)) {
                if (tabuleiro.getPosicao(nx, ny) == 2) {
                    System.out.println("Azul ID " + getId() + " detectou Zumbi em (" + nx + "," + ny + ") e requisitará auto-conversão.");
                    tabuleiro.requisitarAutoConversao(this);
                    return; 
                }
            }
        }
    }
}
