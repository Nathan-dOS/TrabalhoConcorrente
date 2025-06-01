import java.util.concurrent.Semaphore;

/** 
 * Cada elemento (Azul ou Zumbi) será uma thread.
 */
public abstract class Elemento extends Thread {
    protected int x, y; // Posição atual
    protected Tabuleiro tabuleiro;
    protected int tipo; // 1 para Azul, 2 para Zumbi

    public Elemento(int x, int y, Tabuleiro tabuleiro, int tipo) {
        this.x = x;
        this.y = y;
        this.tabuleiro = tabuleiro;
        this.tipo = tipo;
    }

    public int getXPos() {
        return x;
    }

    public int getYPos() {
        return y;
    }

    public int getTipo() {
        return tipo;
    }

    @Override
    public abstract void run();

    // Métodos auxiliares podem ser adicionados aqui (ex: verificar vizinhos)
}

