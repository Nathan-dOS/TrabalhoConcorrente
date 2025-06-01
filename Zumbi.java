import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Representa um elemento do tipo Zumbi.
 * Lógica adaptada para a nova estrutura do Tabuleiro com locks e listas por célula,
 * e para a nova regra de conversão (entrar na célula do Azul).
 */
public class Zumbi extends Elemento {
    private final Random random = new Random();

    public Zumbi(int x, int y, Tabuleiro tabuleiro) {
        super(x, y, tabuleiro, 2); // Tipo 2 para Zumbi
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
                if (currentLock == null) continue; 
                currentLock.lock();

                // Verificar se ainda estamos na lista desta célula
                boolean stillHere = false;
                for(Elemento e : tabuleiro.getOcupantes(currentX, currentY)){
                    if(e == this){
                        stillHere = true;
                        break;
                    }
                }
                if (!stillHere || Thread.currentThread().isInterrupted()) {
                    break; // Fomos removidos ou interrompidos
                }

                // 2. Verificar vizinhos em busca de Azuis
                Elemento vizinhoAzul = tabuleiro.verificarVizinhoAzul(currentX, currentY);

                boolean moved = false;
                if (vizinhoAzul != null) {
                    // 3. Tentar mover para a célula do Azul para converter
                    int azulX = vizinhoAzul.getXPos();
                    int azulY = vizinhoAzul.getYPos();

                    destLock = tabuleiro.getLock(azulX, azulY);
                    if (destLock != null && destLock.tryLock()) {
                        try {
                            // Re-verificar se o Azul ainda está lá e é o único ocupante
                            List<Elemento> ocupantesAzul = tabuleiro.getOcupantes(azulX, azulY);
                            if (ocupantesAzul.size() == 1 && ocupantesAzul.get(0) == vizinhoAzul && vizinhoAzul.isAlive()) {
                                
                                System.out.println("Zumbi ID " + getId() + " movendo de (" + currentX + "," + currentY + ") para converter Azul em (" + azulX + "," + azulY + ")");
                                // Mover Zumbi para a célula do Azul
                                tabuleiro.moverElemento(currentX, currentY, azulX, azulY, this);
                                this.updatePosition(azulX, azulY);
                                moved = true;

                                // Chamar conversão (Tabuleiro lida com a lógica interna)
                                tabuleiro.converterParaZumbi(vizinhoAzul, this);
                                // A lógica de tentar mover o Zumbi original para fora está em converterParaZumbi
                            }
                        } finally {
                            destLock.unlock();
                            destLock = null;
                        }
                    }
                }
                
                // 4. Se não tentou/conseguiu converter, tenta mover aleatoriamente
                if (!moved) {
                    // Escolher direção aleatória
                    int dx, dy;
                    do {
                        dx = random.nextInt(3) - 1;
                        dy = random.nextInt(3) - 1;
                    } while (dx == 0 && dy == 0);

                    int novoX = currentX + dx;
                    int novoY = currentY + dy;

                    if (tabuleiro.isDentroDosLimites(novoX, novoY)) {
                        destLock = tabuleiro.getLock(novoX, novoY);
                        if (destLock != null && destLock.tryLock()) {
                            try {
                                List<Elemento> ocupantesDestino = tabuleiro.getOcupantes(novoX, novoY);
                                boolean podeMover = false;
                                Elemento azulParaConverter = null;

                                if (ocupantesDestino.isEmpty()) {
                                    podeMover = true; // Vazia, pode mover
                                } else if (ocupantesDestino.size() == 1 && ocupantesDestino.get(0).getTipo() == 1 && ocupantesDestino.get(0).isAlive()) {
                                    podeMover = true; // Contém um Azul vivo, pode mover (Zumbi entra para converter)
                                    azulParaConverter = ocupantesDestino.get(0);
                                } // else: Contém um Zumbi ou 2 elementos -> não pode mover

                                if (podeMover) {
                                    // Mover o elemento
                                    tabuleiro.moverElemento(currentX, currentY, novoX, novoY, this);
                                    this.updatePosition(novoX, novoY);
                                    moved = true;

                                    // Se moveu para célula com Azul, iniciar conversão
                                    if (azulParaConverter != null) {
                                         System.out.println("Zumbi ID " + getId() + " moveu para (" + novoX + "," + novoY + ") e encontrou Azul para converter.");
                                         tabuleiro.converterParaZumbi(azulParaConverter, this);
                                    }
                                }
                            } finally {
                                destLock.unlock();
                                destLock = null;
                            }
                        }
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Thread Zumbi ID " + getId() + " em (" + x + "," + y + ") interrompida.");
                break;
            } finally {
                // Liberar lock da posição atual (se adquirido)
                if (currentLock != null && ((ReentrantLock)currentLock).isHeldByCurrentThread()) {
                    currentLock.unlock();
                }
                 // Garantir liberação do lock de destino em caso de erro
                if (destLock != null && ((ReentrantLock)destLock).isHeldByCurrentThread()) {
                    destLock.unlock();
                }
            }
        }
        System.out.println("Thread Zumbi ID " + getId() + " terminando.");
    }
}
