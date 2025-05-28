
public class Azul extends Elemento {

    public Azul(int x, int y, Tabuleiro tabuleiro) {
        super(x, y, tabuleiro, 1);
    }

    @Override
    public void run() {
        while (true) { 
            try {
                // 1. Tentar adquirir semáforo da posição atual

                // 2. Escolher direção aleatória

                // 3. Calcular nova posição

                // 4. Verificar limites do tabuleiro

                    // 5. Tentar adquirir semáforo da nova posição

                        // 6. Verificar se a nova posição está vazia

                            // 7. Mover o elemento

                            // 8. Verificar condição de vitória (chegou à direita)

                        // 9. Liberar semáforo da nova posição (se não moveu ou se moveu e já terminou)

                // 10. Liberar semáforo da posição antiga

                Thread.sleep(500);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); 
                System.out.println("Thread Azul interrompida.");
                break; 
            }
        }
    }
}

