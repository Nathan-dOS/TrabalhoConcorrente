import java.util.concurrent.Semaphore;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Tabuleiro {
    private final int altura;
    private final int largura;
    private final int[][] grid; // 0: vazio, 1: Azul, 2: Zumbi
    private final Semaphore[][] semaforos;
    private final List<Elemento> elementos; 
    private volatile boolean jogoAcabou = false;
    private String mensagemFim = "";
    private final Random random = new Random();

    public Tabuleiro(int altura, int largura) {
        this.altura = altura;
        this.largura = largura;
        this.grid = new int[altura][largura];
        this.semaforos = new Semaphore[altura][largura];
        this.elementos = new ArrayList<>();

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

    // --- Métodos de Gerenciamento de Elementos (Estrutura Inicial) ---

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
        if (DentroDosLimites(xAntigo, yAntigo) && DentroDosLimites(xNovo, yNovo)) {
            grid[xAntigo][yAntigo] = 0; // Libera posição antiga
            grid[xNovo][yNovo] = elemento.getTipo(); // Ocupa nova posição
        }
    }

    public synchronized Elemento verificarVizinhoAzul(int x, int y) {
        return null;
    }

    public synchronized void converterParaZumbi(Elemento azul) {

        System.out.println("Elemento Azul em (" + azul.getXPos() + "," + azul.getYPos() + ") foi convertido!");

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

    public boolean isJogoAcabou() {
        return jogoAcabou;
    }

    public String getMensagemFim() {
        return mensagemFim;
    }

    // Método para imprimir o tabuleiro
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
}

