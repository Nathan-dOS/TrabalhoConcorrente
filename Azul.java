import java.util.Random;

/**
 * Representa um elemento do tipo Azul.
 */
public class Azul extends Elemento {
    private final Random random = new Random();

    public Azul(int x, int y, Tabuleiro tabuleiro) {
        super(x, y, tabuleiro, 1); // Tipo 1 para Azul
    }

    @Override
    public void run() {
        // Loop principal da thread Azul
        while (!tabuleiro.JogoAcabou()) {
            try {
                // Tempo aleatório entre movimentos (100-1000ms) para criar ambiente caótico
                Thread.sleep(random.nextInt(901) + 100);
                
                if (tabuleiro.JogoAcabou()) break;
                
                // 1. Tentar adquirir semáforo da posição atual
                tabuleiro.acquireSemaphore(x, y);
                
                try {
                    // Verificar se o jogo acabou antes de continuar
                    if (tabuleiro.JogoAcabou()) break;
                    
                    // 2. Escolher direção aleatória
                    int dx, dy;
                    do {
                        dx = random.nextInt(3) - 1; // -1, 0 ou 1
                        dy = random.nextInt(3) - 1; // -1, 0 ou 1
                    } while (dx == 0 && dy == 0); // Evita ficar parado
                    
                    // 3. Calcular nova posição
                    int novoX = x + dx;
                    int novoY = y + dy;
                    
                    // 4. Verificar limites do tabuleiro
                    if (tabuleiro.DentroDosLimites(novoX, novoY)) {
                        // 5. Tentar adquirir semáforo da nova posição
                        boolean semaphoreAcquired = tabuleiro.tryAcquireSemaphore(novoX, novoY);
                        
                        if (semaphoreAcquired) {
                            try {
                                // 6. Verificar se a nova posição está vazia
                                if (tabuleiro.getPosicao(novoX, novoY) == 0) {
                                    // 7. Mover o elemento
                                    tabuleiro.moverElemento(x, y, novoX, novoY, this);
                                    this.x = novoX;
                                    this.y = novoY;
                                    
                                    // 8. Verificar condição de vitória (chegou à direita)
                                    if (this.y == tabuleiro.getLargura() - 1) {
                                        // Sinalizar fim de jogo (vitória azul)
                                        tabuleiro.terminarJogo("Azul venceu! Chegou à borda direita do tabuleiro.");
                                        break; // Sai do loop da thread
                                    }
                                }
                            } finally {
                                // 9. Liberar semáforo da nova posição (se não moveu ou se moveu e já terminou)
                                tabuleiro.releaseSemaphore(novoX, novoY);
                            }
                        }
                    }
                } finally {
                    // 10. Liberar semáforo da posição antiga
                    tabuleiro.releaseSemaphore(x, y);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restaura o status de interrupção
                System.out.println("Thread Azul em (" + x + "," + y + ") interrompida.");
                break; // Sai do loop se interrompida
            }
        }
    }
}
