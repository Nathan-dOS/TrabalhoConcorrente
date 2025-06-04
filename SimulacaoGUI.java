import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.animation.AnimationTimer;
import java.util.List;
import java.util.ArrayList;

/**
 * Interface gráfica JavaFX para visualizar a simulação.
 */
public class SimulacaoGUI extends Application {

    private static Tabuleiro tabuleiro; // Recebe a instância do tabuleiro
    private static List<Elemento> elementos; // Recebe a lista de elementos para iniciar

    private GridPane gridPane;
    private Rectangle[][] cells;
    private final int CELL_SIZE = 10; // Tamanho de cada célula em pixels
    private long lastUpdateTime = 0;
    private final long UPDATE_INTERVAL_NS = 500_000_000; // Intervalo de atualização (0.5 segundos)

    // Método estático para passar os dados da simulação principal
    public static void setTabuleiro(Tabuleiro tab) {
        tabuleiro = tab;
    }
    public static void setElementos(List<Elemento> elems) {
        elementos = new ArrayList<>(elems); // Cria cópia para segurança
    }

    @Override
    public void start(Stage primaryStage) {
        if (tabuleiro == null || elementos == null) {
            System.err.println("Erro: Tabuleiro ou Elementos não foram configurados antes de iniciar a GUI.");
            Platform.exit();
            return;
        }

        int altura = tabuleiro.getAltura();
        int largura = tabuleiro.getLargura();

        gridPane = new GridPane();
        cells = new Rectangle[altura][largura];

        // Cria a grade visual
        for (int i = 0; i < altura; i++) {
            for (int j = 0; j < largura; j++) {
                cells[i][j] = new Rectangle(CELL_SIZE, CELL_SIZE);
                cells[i][j].setStroke(Color.LIGHTGRAY); // Borda leve
                gridPane.add(cells[i][j], j, i); // Adiciona (coluna, linha)
            }
        }

        updateGridColors(); // Define cores iniciais

        Scene scene = new Scene(gridPane, largura * CELL_SIZE, altura * CELL_SIZE);
        primaryStage.setTitle("Simulação Zumbis vs Azuis");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Inicia as threads dos elementos DEPOIS que a GUI está visível
        System.out.println("GUI pronta. Iniciando threads dos elementos...");
        for (Elemento e : elementos) {
            e.start();
        }

        // Inicia o loop de atualização da GUI
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (now - lastUpdateTime >= UPDATE_INTERVAL_NS) {
                    if (tabuleiro.isJogoAcabou()) {
                        updateGridColors(); // Atualiza uma última vez
                        System.out.println("GUI: Jogo acabou. Parando timer.");
                        this.stop(); // Para o timer
                        // Poderia mostrar a mensagem final em um label ou dialog
                        primaryStage.setTitle("Simulação Finalizada: " + tabuleiro.getMensagemFim());
                    } else {
                        updateGridColors();
                    }
                    lastUpdateTime = now;
                }
            }
        };
        timer.start();

        // Garante que as threads da simulação sejam interrompidas ao fechar a janela
        primaryStage.setOnCloseRequest(event -> {
            System.out.println("Janela fechada. Interrompendo simulação...");
            if (!tabuleiro.isJogoAcabou()) {
                 tabuleiro.terminarJogo("Janela fechada pelo usuário.");
            }
            // Espera um pouco para as threads terminarem
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            Platform.exit();
            System.exit(0); // Força saída se necessário
        });
    }

    // Atualiza as cores da grade visual com base no tabuleiro
    private void updateGridColors() {
        // Executa a atualização na thread da aplicação JavaFX
        Platform.runLater(() -> {
            int[][] gridState = tabuleiro.getGridCopy(); // Pega cópia segura do estado
            for (int i = 0; i < tabuleiro.getAltura(); i++) {
                for (int j = 0; j < tabuleiro.getLargura(); j++) {
                    Color color;
                    switch (gridState[i][j]) {
                        case 1:  color = Color.BLUE; break;  // Azul
                        case 2:  color = Color.RED; break;   // Zumbi
                        default: color = Color.WHITE; break; // Vazio
                    }
                    cells[i][j].setFill(color);
                }
            }
        });
    }

    // O método main agora estará em Simulacao.java e chamará Application.launch()
}

