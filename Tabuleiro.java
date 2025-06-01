import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Lock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Representa o tabuleiro do jogo.
 * Contém a grade (agora uma lista de Elementos por célula), os locks para cada célula e gerencia os elementos.
 */
public class Tabuleiro {
    private final int altura;
    private final int largura;
    // Alterado: Cada célula agora contém uma lista de elementos presentes.
    // Usar CopyOnWriteArrayList pode ser uma alternativa se a leitura for muito mais frequente que a escrita,
    // mas com locks, ArrayList sincronizado externamente é suficiente.
    private final List<Elemento>[][] grid;
    // Alterado: Usando ReentrantLock para mais flexibilidade.
    private final Lock[][] locks;
    private final List<Elemento> elementos; // Lista mestre para manter referência a todas as threads
    private volatile boolean jogoAcabou = false;
    private String mensagemFim = "";
    private final Random random = new Random();

    @SuppressWarnings("unchecked")
    public Tabuleiro(int altura, int largura) {
        this.altura = altura;
        this.largura = largura;
        this.grid = new ArrayList[altura][largura];
        this.locks = new ReentrantLock[altura][largura];
        this.elementos = Collections.synchronizedList(new ArrayList<>()); // Lista mestre sincronizada

        // Inicializa a grade com listas vazias e os locks
        for (int i = 0; i < altura; i++) {
            for (int j = 0; j < largura; j++) {
                grid[i][j] = new ArrayList<>(); // Lista específica da célula
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

    // Retorna uma cópia da lista de ocupantes para evitar modificação externa e problemas de concorrência
    // Acesso à lista DEVE ser protegido pelo lock da célula correspondente
    public List<Elemento> getOcupantes(int x, int y) {
        if (isDentroDosLimites(x, y)) {
            // Não precisa sincronizar aqui, pois a leitura da referência é atômica.
            // A sincronização é necessária ao iterar ou modificar a lista retornada,
            // mas como retornamos uma cópia, a thread chamadora opera na cópia.
            // A modificação da lista original SÓ ocorre dentro de blocos lock(locks[x][y])
            return new ArrayList<>(grid[x][y]);
        }
        return new ArrayList<>(); // Retorna lista vazia se fora dos limites
    }

    public boolean isDentroDosLimites(int x, int y) {
        return x >= 0 && x < altura && y >= 0 && y < largura;
    }

    // --- Métodos de Lock --- 

    public Lock getLock(int x, int y) {
        if (isDentroDosLimites(x, y)) {
            return locks[x][y];
        }
        return null; // Ou lançar exceção
    }

    // --- Métodos de Gerenciamento de Elementos ---

    // Adiciona elemento na posição inicial. Assume que a posição está validada como vazia.
    public void adicionarElementoInicial(Elemento elemento) {
        int x = elemento.getXPos();
        int y = elemento.getYPos();
        if (isDentroDosLimites(x, y)) {
            Lock lock = locks[x][y];
            lock.lock();
            try {
                if (grid[x][y].isEmpty()) { // Confirma se está vazia (embora já devesse estar)
                    grid[x][y].add(elemento);
                    elementos.add(elemento); // Adiciona à lista mestre
                } else {
                    System.err.println("!!! Erro ao adicionar elemento inicial em (" + x + "," + y + "): Célula não estava vazia!");
                }
            } finally {
                lock.unlock();
            }
        } else {
            System.err.println("Erro ao adicionar elemento inicial em (" + x + "," + y + "): Posição inválida.");
        }
    }

    // Move um elemento. Assume que os locks necessários (origem e destino) JÁ ESTÃO ADQUIRIDOS pela thread chamadora.
    public void moverElemento(int xAntigo, int yAntigo, int xNovo, int yNovo, Elemento elemento) {
        if (isDentroDosLimites(xAntigo, yAntigo) && isDentroDosLimites(xNovo, yNovo)) {
            grid[xAntigo][yAntigo].remove(elemento); // Remove da lista da célula antiga
            grid[xNovo][yNovo].add(elemento);     // Adiciona à lista da célula nova
        } else {
             System.err.println("!!! Erro ao mover elemento: Posição inválida.");
        }
    }

    // Verifica células adjacentes em busca de um Azul.
    // Retorna o primeiro Elemento Azul encontrado, ou null.
    // IMPORTANTE: Este método NÃO adquire locks. A thread chamadora deve garantir a sincronização necessária
    // se precisar de um estado consistente dos vizinhos.
    public Elemento verificarVizinhoAzul(int x, int y) {
        int[] dx = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dy = {-1, 0, 1, -1, 1, -1, 0, 1};

        for (int i = 0; i < 8; i++) {
            int nx = x + dx[i];
            int ny = y + dy[i];

            if (isDentroDosLimites(nx, ny)) {
                Lock vizinhoLock = locks[nx][ny];
                vizinhoLock.lock();
                try {
                    for (Elemento e : grid[nx][ny]) {
                        if (e.getTipo() == 1 && e.isAlive()) {
                            return e; // Encontrou um Azul vivo
                        }
                    }
                } finally {
                    vizinhoLock.unlock();
                }
            }
        }
        return null; // Nenhum Azul encontrado
    }

    // Converte um Azul para Zumbi. Assume que o lock da célula (convX, convY) JÁ ESTÁ ADQUIRIDO pela thread Zumbi.
    // O zumbiOriginal é o que entrou na célula e causou a conversão.
    public void converterParaZumbi(Elemento azul, Elemento zumbiOriginal) {
        if (azul == null || azul.getTipo() != 1 || !azul.isAlive() || zumbiOriginal == null || zumbiOriginal.getTipo() != 2) {
             System.err.println("!!! Tentativa de conversão inválida.");
             return;
        }

        int convX = azul.getXPos();
        int convY = azul.getYPos();

        System.out.println("Convertendo Azul em (" + convX + "," + convY + ") por Zumbi ID " + zumbiOriginal.getId());

        // 1. Parar a thread do Azul e remover das listas
        azul.interrupt();
        elementos.remove(azul); // Remove da lista mestre
        grid[convX][convY].remove(azul); // Remove da lista da célula

        // 2. Criar e adicionar novo Zumbi na célula
        Zumbi novoZumbi = new Zumbi(convX, convY, this);
        grid[convX][convY].add(novoZumbi); // Adiciona novo Zumbi à célula
        elementos.add(novoZumbi); // Adiciona à lista mestre
        novoZumbi.start();
        System.out.println("Novo Zumbi ID " + novoZumbi.getId() + " criado em (" + convX + "," + convY + ")");


        // 3. Tentar mover o Zumbi original para fora imediatamente
        boolean movedOut = tentarMoverImediatamente(zumbiOriginal, convX, convY);
        if (movedOut) {
            // Se moveu, remove o original da célula de conversão
            grid[convX][convY].remove(zumbiOriginal);
        } else {
            System.out.println("Zumbi original ID " + zumbiOriginal.getId() + " não conseguiu sair imediatamente de (" + convX + "," + convY + ")");
            // Permanece temporariamente na célula com o novo Zumbi
        }

        // 4. Verificar condição de fim (todos zumbis)
        // Usar synchronized na lista mestre para verificação segura
        synchronized (elementos) {
             verificarFimTodosZumbis();
        }
    }

    // Tenta mover um Zumbi para uma célula adjacente vazia aleatória.
    // Assume que o lock da célula atual (currentX, currentY) JÁ ESTÁ ADQUIRIDO.
    private boolean tentarMoverImediatamente(Elemento zumbi, int currentX, int currentY) {
        int[] dx = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dy = {-1, 0, 1, -1, 1, -1, 0, 1};
        List<Integer> possibleMoves = new ArrayList<>();
        for (int i = 0; i < 8; i++) possibleMoves.add(i);
        Collections.shuffle(possibleMoves, random); // Randomiza ordem de verificação

        for (int i : possibleMoves) {
            int nx = currentX + dx[i];
            int ny = currentY + dy[i];

            if (isDentroDosLimites(nx, ny)) {
                Lock destLock = locks[nx][ny];
                if (destLock.tryLock()) { // Tenta adquirir lock do destino
                    try {
                        if (grid[nx][ny].isEmpty()) { // Verifica se está vazia
                            // Move imediatamente
                            grid[nx][ny].add(zumbi); // Adiciona ao destino
                            // A remoção da célula atual (currentX, currentY) será feita em converterParaZumbi se retornar true
                            zumbi.updatePosition(nx, ny); // Atualiza posição interna do elemento
                            System.out.println("Zumbi original ID " + zumbi.getId() + " saiu imediatamente para (" + nx + "," + ny + ")");
                            return true; // Movido com sucesso
                        }
                    } finally {
                        destLock.unlock(); // Libera lock do destino
                    }
                }
            }
        }
        return false; // Não encontrou célula vazia adjacente ou não conseguiu lock
    }

    // Verifica se todos os elementos restantes são Zumbis.
    // Deve ser chamado dentro de um bloco synchronized(elementos)
    private void verificarFimTodosZumbis() {
        boolean todosZumbis = true;
        int azulCount = 0;
        for (Elemento e : elementos) {
            if (e.getTipo() == 1 && e.isAlive()) {
                //todosZumbis = false;
                //break;
                azulCount++; // Conta Azuis vivos
            }
        }
        //if (todosZumbis) {
        if(azulCount == 0 && !elementos.isEmpty()){ // Garante que não acabou só porque a lista está vazia
            terminarJogo("Todos os elementos são Zumbis!");
        }
    }

    // Termina o jogo. Usa synchronized para garantir que a mensagem e a flag sejam definidas atomicamente.
    public synchronized void terminarJogo(String mensagem) {
        if (!jogoAcabou) {
            this.jogoAcabou = true;
            this.mensagemFim = mensagem;
            System.out.println("\n==================== FIM DE JOGO ====================");
            System.out.println("Motivo: " + mensagem);
            System.out.println("=======================================================\n");
            // Interromper todas as threads de elementos na lista mestre
            // Usar cópia para evitar ConcurrentModificationException
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

    // Método para imprimir o tabuleiro (adaptado para listas)
    public void imprimirTabuleiro() {
        System.out.println("--- Tabuleiro (" + getEstatisticas() + ") ---");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < altura; i++) {
            for (int j = 0; j < largura; j++) {
                Lock lock = locks[i][j];
                lock.lock();
                try {
                    List<Elemento> ocupantes = grid[i][j];
                    if (ocupantes.isEmpty()) {
                        sb.append(". ");
                    } else if (ocupantes.size() == 1) {
                        sb.append(ocupantes.get(0).getTipo() == 1 ? "A " : "Z ");
                    } else { // Deve ter 1 Azul e 1 Zumbi (ou Zumbi original + novo Zumbi temporariamente)
                        boolean temAzul = false;
                        boolean temZumbi = false;
                        for(Elemento e : ocupantes) {
                            if(e.getTipo() == 1) temAzul = true;
                            if(e.getTipo() == 2) temZumbi = true;
                        }
                        if(temAzul && temZumbi) sb.append("X "); // Conflito/Conversão
                        else if (temZumbi) sb.append("2Z"); // Dois zumbis temporariamente
                        else sb.append("? "); // Estado inesperado
                    }
                } finally {
                    lock.unlock();
                }
            }
            sb.append("\n");
        }
        System.out.print(sb.toString());
        System.out.println("---------------------------------------------------");
    }

    // Método para obter estatísticas atuais (adaptado para lista mestre)
    public String getEstatisticas() {
        int contAzul = 0;
        int contZumbi = 0;
        // Sincroniza na lista mestre para contagem segura
        synchronized (elementos) {
            for (Elemento e : elementos) {
                if (e.isAlive()) { // Conta apenas threads vivas
                    if (e.getTipo() == 1) contAzul++;
                    else if (e.getTipo() == 2) contZumbi++;
                }
            }
        }
        return contAzul + " Azuis, " + contZumbi + " Zumbis vivos";
    }
}

