package game;

import board.Board;
import board.Constants;
import board.Move;
import tools.FenToBoard;

import java.util.List;

public class Game {
    private final Board board;
    private List<Move> moves;

    public Game() {
        Player A = new Player("engine1", Constants.LIGHT);
        Player B = new Player("engine2", Constants.DARK);
        board = FenToBoard.toBoard(Constants.START_POSITION);
        A.run();
        System.out.println(moves);
    }

    public static void main(String[] args) {
        new Game();
    }

    List<Move> play(Board board, int depth) {
        if (depth == 0) return moves;
        board.gen();
        List<Move> moves = board.pseudomoves;

        for (Move move : moves) {
            if (board.makemove(move)) {
                play(new Board(board), depth - 1);
                board.takeback();
            }
        }
        return moves;
    }

    class Player implements Runnable {
        String nom;
        int couleur;

        public Player(String nom, int couleur) {
            this.nom = nom;
            this.couleur = couleur;
        }

        @Override
        public void run() {
            moves = play(board, 1);

        }
    }


}
