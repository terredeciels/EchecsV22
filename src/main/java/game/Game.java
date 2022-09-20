package game;

import board.Board;
import board.Constants;
import board.Move;
import tools.FenToBoard;

import java.util.List;
import java.util.Random;

public class Game {
    private final Board board;
    private Result result;


    public Game() {
        Player A = new Player("engine1", Constants.LIGHT);
        Player B = new Player("engine2", Constants.DARK);
        board = FenToBoard.toBoard(Constants.START_POSITION);
        A.run();
        System.out.println(result.moves);
        Move move = A.IA(result.moves);

        System.out.println(move);
    }

    public static void main(String[] args) {
        new Game();
    }

    Result play(Board board, int depth) {

        result = new Result();
        if (depth == 0) {
            result.moveCount++;
            result.moves=board.pseudomoves;
            return result;
        }

        board.gen();
        List<Move> moves = board.pseudomoves;

        for (Move move : moves) {
            if (board.makemove(move)) {
                Result subPerft = play(new Board(board), depth - 1);
                board.takeback();
                result.moveCount += subPerft.moveCount;
            }
        }
        result.moves=board.pseudomoves;
        return result;
    }

    class Result {

        public long timeTaken = 0;
        long moveCount = 0;
        List<Move> moves;

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
            play(board, 1);

        }

        public Move IA(List<Move> moves) {
            Random r = new Random();
            int index = r.nextInt((int) result.moveCount);
            return moves.get(index);
        }
    }


}
