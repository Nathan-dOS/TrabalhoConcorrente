import java.util.concurrent.locks.Lock;

/**
 * Classe abstrata que representa um elemento no tabuleiro.
 * Cada elemento (Azul ou Zumbi) será uma thread.
 */
public abstract class Elemento extends Thread {
    // Posição é volátil para garantir visibilidade entre threads,
    // embora o acesso principal seja protegido por locks no Tabuleiro.
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

    /**
     * Atualiza a posição interna do elemento.
     * Chamado pelo Tabuleiro após um movimento bem-sucedido.
     */
    public void updatePosition(int novoX, int novoY) {
        this.x = novoX;
        this.y = novoY;
    }

    /**
     * Lógica principal da thread do elemento (movimento, interação).
     * Esta será implementada nas subclasses Azul e Zumbi.
     */
    @Override
    public abstract void run();

}

