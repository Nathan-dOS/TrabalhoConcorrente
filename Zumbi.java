/**
 * Representa um elemento do tipo Zumbi.
 */
public class Zumbi extends Elemento {

    public Zumbi(int x, int y, Tabuleiro tabuleiro) {
        super(x, y, tabuleiro, 2); // Tipo 2 para Zumbi
    }

    @Override
    public void run() {
        // Loop principal da thread Zumbi
        while (true) { // Condição de parada precisa ser definida (ex: jogo acabou)
            try {
                // 1. Tentar adquirir semáforo da posição atual

                // 2. Verificar vizinhos em busca de Azuis

                    // 3. Escolher direção aleatória

                    // 4. Calcular nova posição

                    // 5. Verificar limites do tabuleiro

                        // 6. Tentar adquirir semáforo da nova posição

                            // 7. Verificar se a nova posição está vazia

                            // 9. Liberar semáforo da nova posição

                // 10. Liberar semáforo da posição antiga

                Thread.sleep(500);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Thread Zumbi interrompida.");
                break;
            }
        }
    }
}

