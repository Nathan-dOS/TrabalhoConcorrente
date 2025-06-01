import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.List;

/**
 * Representa um elemento do tipo Azul.
 * Lógica adaptada para a nova estrutura do Tabuleiro com locks e listas por célula.
 */
public class Azul extends Elemento {
    private final Random random = new Random();

    public Azul(int x, int y, Tabuleiro tabuleiro) {
        super(x, y, tabuleiro, 1); // Tipo 1 para Azul
    }

    @Override
    public void run() {
        while (!tabuleiro.isJogoAcabou() && !Thread.currentThread().isInterrupted()) {
            Lock currentLock = null;
            Lock destLock = null;
            try {
                // Tempo aleatório entre movimentos (100-1000ms)
                Thread.sleep(random.nextInt(901) + 100);

                if (tabuleiro.isJogoAcabou() || Thread.currentThread().isInterrupted()) break;

                int currentX = this.x;
                int currentY = this.y;

                // 1. Adquirir lock da posição atual
                currentLock = tabuleiro.getLock(currentX, currentY);
                if (currentLock == null) continue; // Posição inválida?
                currentLock.lock();

                // Verificar se ainda estamos na lista desta célula (não fomos convertidos)
                boolean stillHere = false;
                for(Elemento e : tabuleiro.getOcupantes(currentX, currentY)){
                    if(e == this){
                        stillHere = true;
                        break;
                    }
                }
                if (!stillHere || Thread.currentThread().isInterrupted()) {
                    // Fomos removidos ou interrompidos enquanto esperávamos o lock
                    break;
                }

                // 2. Escolher direção aleatória
                int dx, dy;
                do {
                    dx = random.nextInt(3) - 1; // -1, 0 ou 1
                    dy = random.nextInt(3) - 1; // -1, 0 ou 1
                } while (dx == 0 && dy == 0); // Evita ficar parado

                // 3. Calcular nova posição
                int novoX = currentX + dx;
                int novoY = currentY + dy;

                // 4. Verificar limites do tabuleiro
                if (tabuleiro.isDentroDosLimites(novoX, novoY)) {
                    // 5. Tentar adquirir lock da nova posição
                    destLock = tabuleiro.getLock(novoX, novoY);
                    if (destLock != null && destLock.tryLock()) {
                        try {
                            // 6. Verificar ocupantes da nova posição
                            List<Elemento> ocupantesDestino = tabuleiro.getOcupantes(novoX, novoY);
                            boolean podeMover = false;
                            if (ocupantesDestino.isEmpty()) {
                                podeMover = true; // Vazia, pode mover
                            } else if (ocupantesDestino.size() == 1 && ocupantesDestino.get(0).getTipo() == 2) {
                                podeMover = true; // Contém apenas um Zumbi, pode mover (Azul entra)
                            } // else: Contém um Azul ou 2 elementos -> não pode mover

                            if (podeMover) {
                                // 7. Mover o elemento (Tabuleiro atualiza listas)
                                tabuleiro.moverElemento(currentX, currentY, novoX, novoY, this);
                                // Atualiza posição interna
                                this.updatePosition(novoX, novoY);

                                // 8. Verificar condição de vitória (chegou à direita)
                                if (this.y == tabuleiro.getLargura() - 1) {
                                    tabuleiro.terminarJogo("Azul ID " + getId() + " venceu! Chegou à borda direita.");
                                    break; // Sai do loop da thread
                                }
                            }
                        } finally {
                            // 9. Liberar lock da nova posição
                            destLock.unlock();
                            destLock = null; // Garante que não será liberado de novo no finally externo
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restaura o status de interrupção
                System.out.println("Thread Azul ID " + getId() + " em (" + x + "," + y + ") interrompida.");
                break; // Sai do loop se interrompida
            } finally {
                // 10. Liberar lock da posição antiga (se adquirido)
                if (currentLock != null && ((ReentrantLock)currentLock).isHeldByCurrentThread()) {
                    currentLock.unlock();
                }
                // Garante que o lock de destino seja liberado se algo deu errado entre tryLock e o finally interno
                if (destLock != null && ((ReentrantLock)destLock).isHeldByCurrentThread()) {
                    destLock.unlock();
                }
            }
        }
        System.out.println("Thread Azul ID " + getId() + " terminando.");
    }
}
