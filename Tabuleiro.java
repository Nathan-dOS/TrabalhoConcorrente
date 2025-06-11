import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Lock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Tabuleiro {
    private final int altura;
    private final int largura;
    private final int[][] grid;
    private final Lock[][] locks;
    public final List<Elemento> elementos;
    private volatile boolean jogoAcabou = false;
    private String mensagemFim = "";
    private final Random random = new Random();
    private long tempoInicioSimulacao;

    public Tabuleiro(int altura, int largura) {
        this.altura = altura;
        this.largura = largura;
        this.grid = new int[altura][largura]; // Inicializa com 0 (vazio)
        this.locks = new ReentrantLock[altura][largura];
        this.elementos = Collections.synchronizedList(new ArrayList<>());
        this.tempoInicioSimulacao = System.currentTimeMillis(); // Marca o tempo inicial

        // Inicializa os locks
        for (int i = 0; i < altura; i++) {
            for (int j = 0; j < largura; j++) {
                locks[i][j] = new ReentrantLock();
            }
        }
    }

    public int getAltura() {
        return altura;
    }

    public int getLargura() {
        return largura;
    }
    
    public long getSegundosPassados() {
        return (System.currentTimeMillis() - tempoInicioSimulacao) / 1000;
    }

    // Retorna o tipo de elemento na célula ou 0 se vazia, -1 se fora dos limites.
    // Acesso à grid não precisa de lock aqui, pois a leitura de int é atômica.
    // Locks são usados para operações de *modificação* ou leitura-modificação complexas (conversão).
    public int getPosicao(int x, int y) {
        if (isDentroDosLimites(x, y)) {
            return grid[x][y];
        }
        return -1; // Indica fora dos limites
    }

    public boolean isDentroDosLimites(int x, int y) {
        return x >= 0 && x < altura && y >= 0 && y < largura;
    }

    // --- Métodos de Lock --- 

    public Lock getLock(int x, int y) {
        if (isDentroDosLimites(x, y)) {
            return locks[x][y];
        }
        return null;
    }

    // --- Métodos de Gerenciamento de Elementos ---

    // Adiciona elemento na posição inicial. Assume que a posição está validada como vazia.
    public void adicionarElementoInicial(Elemento elemento) {
        int x = elemento.getXPos();
        int y = elemento.getYPos();
        if (isDentroDosLimites(x, y)) {
            // Não precisa de lock para escrita inicial assumindo que Simulacao garante não sobreposição.
            if (grid[x][y] == 0) {
                grid[x][y] = elemento.getTipo();
                elementos.add(elemento); // Adiciona à lista mestre
            } else {
                System.err.println("!!! Erro ao adicionar elemento inicial em (" + x + "," + y + "): Célula não estava vazia!");
            }
        } else {
            System.err.println("Erro ao adicionar elemento inicial em (" + x + "," + y + "): Posição inválida.");
        }
    }

    // Move um elemento. Assume que o lock da célula de ORIGEM está adquirido pela thread.
    // Tenta adquirir o lock do DESTINO.
    public boolean tentarMoverElemento(int xAntigo, int yAntigo, int xNovo, int yNovo, Elemento elemento) {
        if (!isDentroDosLimites(xAntigo, yAntigo) || !isDentroDosLimites(xNovo, yNovo)) {
            return false; // Posições inválidas
        }

        Lock lockDestino = locks[xNovo][yNovo];
        boolean lockDestinoAdquirido = false;

        try {
            lockDestinoAdquirido = lockDestino.tryLock(); // Tenta adquirir lock do destino
            if (lockDestinoAdquirido) {
                if (grid[xNovo][yNovo] == 0) { // Verifica se destino está vazio
                    grid[xNovo][yNovo] = elemento.getTipo(); // Ocupa nova posição
                    grid[xAntigo][yAntigo] = 0; // Libera posição antiga
                    elemento.updatePosition(xNovo, yNovo); // Atualiza posição interna do elemento
                    return true; // Movimento bem-sucedido
                }
            }
        } finally {
            if (lockDestinoAdquirido) {
                lockDestino.unlock(); // Libera lock do destino se foi adquirido
            }
        }
        return false; // Movimento falhou (destino ocupado ou não conseguiu lock)
    }
    
    // --- Lógica de Conversão Centralizada --- 
    
    // Método chamado pelo Azul quando detecta um Zumbi adjacente
    public void requisitarAutoConversao(Azul azul) {
        if (jogoAcabou || azul == null || !azul.isAlive()) return;
        
        int x = azul.getXPos();
        int y = azul.getYPos();
        Lock lockAzul = getLock(x, y);
        if (lockAzul == null) return;
        
        lockAzul.lock(); // Bloqueia a célula do Azul
        try {
            // Revalida: A célula ainda contém este Azul e ele está vivo?
            if (grid[x][y] == 1 && azul.isAlive()) {
                 System.out.println("Azul ID " + azul.getId() + " em (" + x + "," + y + ") requisitou auto-conversão.");
                 realizarConversao(azul, x, y);
            } else {
                 //System.out.println("Auto-conversão cancelada para Azul ID " + azul.getId() + " em (" + x + "," + y + ") - Estado mudou.");
            }
        } finally {
            lockAzul.unlock();
        }
    }
    
    // Método chamado pelo Zumbi quando detecta um Azul adjacente
    public void requisitarConversao(Elemento azulDetectado) {
        if (jogoAcabou || azulDetectado == null || azulDetectado.getTipo() != 1 || !azulDetectado.isAlive()) return;
        
        int x = azulDetectado.getXPos();
        int y = azulDetectado.getYPos();
        Lock lockAzul = getLock(x, y);
        if (lockAzul == null) return;
        
        lockAzul.lock(); // Bloqueia a célula do Azul
        try {
             // Revalida: A célula ainda contém este Azul e ele está vivo?
            if (grid[x][y] == 1 && azulDetectado.isAlive()) {
                 System.out.println("Conversão requisitada para Azul ID " + azulDetectado.getId() + " em (" + x + "," + y + ").");
                 realizarConversao((Azul)azulDetectado, x, y);
            } else {
                 //System.out.println("Conversão cancelada para Azul ID " + azulDetectado.getId() + " em (" + x + "," + y + ") - Estado mudou.");
            }
        } finally {
            lockAzul.unlock();
        }
    }
    
    // Método privado que efetivamente realiza a conversão. Assume que o lock da célula (convX, convY) JÁ ESTÁ ADQUIRIDO.
    private void realizarConversao(Azul azul, int convX, int convY) {
        System.out.println("Realizando conversão do Azul ID " + azul.getId() + " em (" + convX + "," + convY + ") para Zumbi!");
        
        // 1. Parar a thread do Azul e remover da lista mestre
        azul.interrupt();
        elementos.remove(azul);
        
        // 2. Atualizar grid para Zumbi
        grid[convX][convY] = 2; // Marca como Zumbi
        
        // 3. Criar e adicionar novo Zumbi
        Zumbi novoZumbi = new Zumbi(convX, convY, this);
        elementos.add(novoZumbi); // Adiciona à lista mestre
        novoZumbi.start();
        System.out.println("Novo Zumbi ID " + novoZumbi.getId() + " criado em (" + convX + "," + convY + ")");

        // 4. Verificar condição de fim (todos zumbis)
        // Usar synchronized na lista mestre para verificação segura
        synchronized (elementos) {
             verificarFimTodosZumbis();
        }
    }

    // Verifica se todos os elementos restantes são Zumbis.
    // Deve ser chamado dentro de um bloco synchronized(elementos)
    private void verificarFimTodosZumbis() {
        int azulCount = 0;
        for (Elemento e : elementos) {
            if (e.getTipo() == 1 && e.isAlive()) {
                azulCount++;
            }
        }
        if(azulCount == 0 && !elementos.isEmpty()){ // Garante que não acabou só porque a lista está vazia
            // Pequena pausa para garantir que a mensagem de conversão apareça antes do fim de jogo
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            terminarJogo("Todos os elementos são Zumbis!");
        }
    }

    // Termina o jogo. Usa synchronized para garantir atomicidade.
    public synchronized void terminarJogo(String mensagem) {
        if (!jogoAcabou) {
            this.jogoAcabou = true;
            this.mensagemFim = mensagem;
            System.out.println("\n==================== FIM DE JOGO ====================");
            System.out.println("Motivo: " + mensagem);
            System.out.println("=======================================================\n");
            // Interromper todas as threads de elementos na lista mestre
            List<Elemento> copiaElementos = new ArrayList<>(elementos);
            for (Elemento e : copiaElementos) {
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

    // Método para imprimir o tabuleiro (adaptado para int[][])
    public String getTabuleiroString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < altura; i++) {
            for (int j = 0; j < largura; j++) {
                 // Leitura simples da grid, não precisa de lock aqui
                 int tipo = grid[i][j];
                 if (tipo == 0) sb.append(". ");
                 else if (tipo == 1) sb.append("A ");
                 else sb.append("Z ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
    
    public void imprimirTabuleiro() {
         System.out.println("--- Tabuleiro (" + getEstatisticas() + ") ---");
         System.out.print(getTabuleiroString());
         System.out.println("---------------------------------------------------");
    }

    // Método para obter estatísticas atuais (adaptado para lista mestre)
    public String getEstatisticas() {
        int contAzul = 0;
        int contZumbi = 0;
        synchronized (elementos) {
            for (Elemento e : elementos) {
                if (e.isAlive()) {
                    if (e.getTipo() == 1) contAzul++;
                    else if (e.getTipo() == 2) contZumbi++;
                }
            }
        }
        return contAzul + " Azuis, " + contZumbi + " Zumbis vivos";
    }
    
    // Retorna a grid atual (para GUI). Retorna cópia para segurança.
    public int[][] getGridCopy() {
        int[][] copy = new int[altura][largura];
        for (int i = 0; i < altura; i++) {
            // System.arraycopy é eficiente, mas a leitura da grid int[][] é atômica por linha,
            // para consistência visual completa, um lock global seria necessário, mas é overkill aqui.
            System.arraycopy(grid[i], 0, copy[i], 0, largura);
        }
        return copy;
    }
}

