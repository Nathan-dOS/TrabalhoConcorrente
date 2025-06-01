import java.util.Random;

/**
 * Representa um elemento do tipo Zumbi.
 */
public class Zumbi extends Elemento {
    private final Random random = new Random();

    public Zumbi(int x, int y, Tabuleiro tabuleiro) {
        super(x, y, tabuleiro, 2); // Tipo 2 para Zumbi
    }

    @Override
    public void run() {
        // Loop principal da thread Zumbi
        while (!tabuleiro.JogoAcabou()) {
            try {
                // Tempo aleatório entre movimentos (100-1000ms)
                Thread.sleep(random.nextInt(901) + 100);
                
                if (tabuleiro.JogoAcabou()) break;
                
                // 1. Tentar adquirir semáforo da posição atual
                tabuleiro.acquireSemaphore(x, y);
                
                try {
                    // Verificar se o jogo acabou antes de continuar
                    if (tabuleiro.JogoAcabou()) break;
                    
                    // 2. Verificar vizinhos em busca de Azuis ("frente a")
                    Elemento vizinhoAzul = tabuleiro.verificarVizinhoAzul(x, y);
                    
                    if (vizinhoAzul != null) {
                        // Tentar adquirir semáforo do vizinho Azul
                        int azulX = vizinhoAzul.getXPos();
                        int azulY = vizinhoAzul.getYPos();
                        
                        if (tabuleiro.tryAcquireSemaphore(azulX, azulY)) {
                            try {
                                // Verificar se o Azul ainda está na posição (pode ter se movido)
                                if (tabuleiro.getPosicao(azulX, azulY) == 1) {
                                    // Converter Azul para Zumbi
                                    tabuleiro.converterParaZumbi(vizinhoAzul);
                                }
                            } finally {
                                tabuleiro.releaseSemaphore(azulX, azulY);
                            }
                        }
                    } else {
                        // Se não converteu, tenta mover
                        // 3. Escolher direção aleatória
                        int dx, dy;
                        do {
                            dx = random.nextInt(3) - 1; // -1, 0 ou 1
                            dy = random.nextInt(3) - 1; // -1, 0 ou 1
                        } while (dx == 0 && dy == 0); // Evita ficar parado
                        
                        // 4. Calcular nova posição
                        int novoX = x + dx;
                        int novoY = y + dy;
                        
                        // 5. Verificar limites do tabuleiro
                        if (tabuleiro.DentroDosLimites(novoX, novoY)) {
                            // 6. Tentar adquirir semáforo da nova posição
                            boolean semaphoreAcquired = tabuleiro.tryAcquireSemaphore(novoX, novoY);
                            
                            if (semaphoreAcquired) {
                                try {
                                    // 7. Verificar se a nova posição está vazia
                                    if (tabuleiro.getPosicao(novoX, novoY) == 0) {
                                        // 8. Mover o elemento
                                        tabuleiro.moverElemento(x, y, novoX, novoY, this);
                                        this.x = novoX;
                                        this.y = novoY;
                                    }
                                } finally {
                                    // 9. Liberar semáforo da nova posição
                                    tabuleiro.releaseSemaphore(novoX, novoY);
                                }
                            }
                        }
                    }
                } finally {
                    // 10. Liberar semáforo da posição antiga
                    tabuleiro.releaseSemaphore(x, y);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Thread Zumbi em (" + x + "," + y + ") interrompida.");
                break;
            }
        }
    }
}
