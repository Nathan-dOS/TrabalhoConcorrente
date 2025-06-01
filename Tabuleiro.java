import java.util.concurrent.Semaphore;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Representa o tabuleiro do jogo.
 * Contém a grade, os semáforos para cada célula e gerencia os elementos.
 */
public class Tabuleiro {
    private final int altura;
    private final int largura;
    private final int[][] grid; // 0: vazio, 1: Azul, 2: Zumbi
    private final Semaphore[][] semaforos;
    private final List<Elemento> elementos; // Lista para manter referência aos elementos/threads
    private volatile boolean jogoAcabou = false;
    private String mensagemFim = "";
    private final Random random = new Random();

    public Tabuleiro(int altura, int largura) {
        this.altura = altura;
        this.largura = largura;
        this.grid = new int[altura][largura];
        this.semaforos = new Semaphore[altura][largura];
        this.elementos = new ArrayList<>();

        // Inicializa semáforos (um para cada célula, permitindo apenas 1 acesso)
        for (int i = 0; i < altura; i++) {
            for (int j = 0; j < largura; j++) {
                semaforos[i][j] = new Semaphore(1);
            }
        }
    }

    public int getAltura() {
        return altura;
    }

    public int getLargura() {
        return largura;
    }

    public synchronized int getPosicao(int x, int y) {
        if (DentroDosLimites(x, y)) {
            return grid[x][y];
        }
        return -1; // Indica fora dos limites
    }

    public boolean DentroDosLimites(int x, int y) {
        return x >= 0 && x < altura && y >= 0 && y < largura;
    }

    // --- Métodos de Semáforo --- 

    public void acquireSemaphore(int x, int y) throws InterruptedException {
        if (DentroDosLimites(x, y)) {
            semaforos[x][y].acquire();
        }
    }

    public boolean tryAcquireSemaphore(int x, int y) {
        if (DentroDosLimites(x, y)) {
            return semaforos[x][y].tryAcquire();
        }
        return false;
    }

    public void releaseSemaphore(int x, int y) {
        if (DentroDosLimites(x, y)) {
            semaforos[x][y].release();
        }
    }

    // --- Métodos de Gerenciamento de Elementos ---

    public synchronized void adicionarElemento(Elemento elemento) {
        if (DentroDosLimites(elemento.getXPos(), elemento.getYPos()) && grid[elemento.getXPos()][elemento.getYPos()] == 0) {
            grid[elemento.getXPos()][elemento.getYPos()] = elemento.getTipo();
            elementos.add(elemento);
        } else {
            // Tratar colisão inicial ou posição inválida se necessário
            System.err.println("Erro ao adicionar elemento em (" + elemento.getXPos() + "," + elemento.getYPos() + "): Posição inválida ou ocupada.");
        }
    }

    public synchronized void moverElemento(int xAntigo, int yAntigo, int xNovo, int yNovo, Elemento elemento) {
        // Assume que os semáforos necessários já foram adquiridos
        if (DentroDosLimites(xAntigo, yAntigo) && DentroDosLimites(xNovo, yNovo)) {
            grid[xAntigo][yAntigo] = 0; // Libera posição antiga
            grid[xNovo][yNovo] = elemento.getTipo(); // Ocupa nova posição
        }
    }

    public synchronized Elemento verificarVizinhoAzul(int x, int y) {
        // Verifica as 8 células adjacentes em busca de um Azul
        int[] dx = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dy = {-1, 0, 1, -1, 1, -1, 0, 1};
        
        for (int i = 0; i < 8; i++) {
            int nx = x + dx[i];
            int ny = y + dy[i];
            
            if (DentroDosLimites(nx, ny) && grid[nx][ny] == 1) {
                // Encontrou um Azul, agora precisa encontrar o objeto Elemento correspondente
                for (Elemento e : elementos) {
                    if (e.getTipo() == 1 && e.getXPos() == nx && e.getYPos() == ny && e.isAlive()) {
                        return e;
                    }
                }
            }
        }
        return null; // Nenhum Azul encontrado nas células adjacentes
    }

    public synchronized void converterParaZumbi(Elemento azul) {
        if (azul == null || azul.getTipo() != 1) return;
        
        int azulX = azul.getXPos();
        int azulY = azul.getYPos();
        
        System.out.println("Elemento Azul em (" + azulX + "," + azulY + ") foi convertido para Zumbi!");
        
        // Parar a thread do azul
        azul.interrupt();
        
        // Remover da lista de elementos
        elementos.remove(azul);
        
        // Atualizar grid
        grid[azulX][azulY] = 2; // Marca como Zumbi
        
        // Criar e adicionar novo zumbi
        Zumbi novoZumbi = new Zumbi(azulX, azulY, this);
        elementos.add(novoZumbi);
        novoZumbi.start();

        // Verificar condição de fim (todos zumbis)
        verificarFimTodosZumbis();
    }

    private synchronized void verificarFimTodosZumbis() {
        boolean todosZumbis = true;
        for (Elemento e : elementos) {
            if (e.getTipo() == 1 && e.isAlive()) { // Verifica se é Azul e está ativo
                todosZumbis = false;
                break;
            }
        }
        if (todosZumbis) {
            terminarJogo("Todos os elementos são Zumbis!");
        }
    }

    public synchronized void terminarJogo(String mensagem) {
        if (!jogoAcabou) {
            this.jogoAcabou = true;
            this.mensagemFim = mensagem;
            System.out.println("FIM DE JOGO: " + mensagem);
            // Interromper todas as threads de elementos
            for (Elemento e : elementos) {
                e.interrupt();
            }
        }
    }

    public boolean JogoAcabou() {
        return jogoAcabou;
    }

    public String getMensagemFim() {
        return mensagemFim;
    }

    // Método para imprimir o tabuleiro (para debug)
    public synchronized void imprimirTabuleiro() {
        System.out.println("--- Tabuleiro ---");
        for (int i = 0; i < altura; i++) {
            for (int j = 0; j < largura; j++) {
                char c = '.';
                if (grid[i][j] == 1) c = 'A';
                else if (grid[i][j] == 2) c = 'Z';
                System.out.print(c + " ");
            }
            System.out.println();
        }
        System.out.println("-----------------");
    }
    
    // Método para obter estatísticas atuais (para debug e monitoramento)
    public synchronized String getEstatisticas() {
        int contAzul = 0;
        int contZumbi = 0;
        
        for (Elemento e : elementos) {
            if (e.getTipo() == 1 && e.isAlive()) contAzul++;
            else if (e.getTipo() == 2 && e.isAlive()) contZumbi++;
        }
        
        return "Estatísticas: " + contAzul + " Azuis, " + contZumbi + " Zumbis";
    }
}
