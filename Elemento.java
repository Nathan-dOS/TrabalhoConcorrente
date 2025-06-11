import java.util.concurrent.locks.Lock;

public abstract class Elemento extends Thread {
    protected volatile int x, y;
    protected Tabuleiro tabuleiro;
    protected final int tipo; // 1 para Azul, 2 para Zumbi

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

    public void updatePosition(int novoX, int novoY) {
        this.x = novoX;
        this.y = novoY;
    }

    @Override
    public abstract void run();

}

