package board;

import java.util.ArrayList;
import java.util.List;

public class Board extends Piece implements Constants {

    public int[] color = new int[64];
    public int[] piece = new int[64];

    public int side;
    public int xside;

    public int castle;
    public int ep;
    public List<Move> pseudomoves = new ArrayList<>();
    public int halfMoveClock;
    public int plyNumber;
    private int fifty;
    private UndoMove um = new UndoMove();

    public Board() {

    }

    public Board(Board board) {
        color = board.color;
        piece = board.piece;
        side = board.side;
        xside = board.xside;
        castle = board.castle;
        ep = board.ep;
        fifty = board.fifty;
        pseudomoves = new ArrayList<>();
        um = new UndoMove();
        halfMoveClock = board.halfMoveClock;
        plyNumber = board.plyNumber;
    }


    private boolean in_check(int s) {
        for (int i = 0; i < 64; ++i) {
            if (piece[i] == KING && color[i] == s) {
                return attack(i, s ^ 1);
            }
        }
        return true; // shouldn't get here
    }

    private boolean attack(int sq, int s) {
        for (int i = 0; i < 64; ++i) {
            if (color[i] == s) {
                if (piece[i] == PAWN) {
                    if (s == LIGHT) {
                        if ((i & 7) != 0 && i - 9 == sq) {
                            return true;
                        }
                        if ((i & 7) != 7 && i - 7 == sq) {
                            return true;
                        }
                    } else {
                        if ((i & 7) != 0 && i + 7 == sq) {
                            return true;
                        }
                        if ((i & 7) != 7 && i + 9 == sq) {
                            return true;
                        }
                    }
                } else {
                    for (int j = 0; j < offsets[piece[i]]; ++j) {
                        for (int n = i; ; ) {
                            n = mailbox[mailbox64[n] + offset[piece[i]][j]];
                            if (n == -1) {
                                break;
                            }
                            if (n == sq) {
                                return true;
                            }
                            if (color[n] != EMPTY) {
                                break;
                            }
                            if (!slide[piece[i]]) {
                                break;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public void gen() {

        for (int c = 0; c < 64; ++c) {
            if (color[c] == side) {
                if (piece[c] == PAWN) {
                    if (side == LIGHT) {
                        if ((c & 7) != 0 && color[c - 9] == DARK) {
                            gen_push(c, c - 9, 17);
                        }
                        if ((c & 7) != 7 && color[c - 7] == DARK) {
                            gen_push(c, c - 7, 17);
                        }
                        if (color[c - 8] == EMPTY) {
                            gen_push(c, c - 8, 16);
                            if (c >= 48 && color[c - 16] == EMPTY) {
                                gen_push(c, c - 16, 24);
                            }
                        }
                    } else {
                        if ((c & 7) != 0 && color[c + 7] == LIGHT) {
                            gen_push(c, c + 7, 17);
                        }
                        if ((c & 7) != 7 && color[c + 9] == LIGHT) {
                            gen_push(c, c + 9, 17);
                        }
                        if (color[c + 8] == EMPTY) {
                            gen_push(c, c + 8, 16);
                            if (c <= 15 && color[c + 16] == EMPTY) {
                                gen_push(c, c + 16, 24);
                            }
                        }
                    }
                } else {
                    // autres pieces que pions
                    gen(c);
                    //
                }
            }
        }

        /* generate castle moves */
        if (side == LIGHT) {
            if ((castle & 1) != 0) {
                gen_push(E1, G1, 2);
            }
            if ((castle & 2) != 0) {
                gen_push(E1, C1, 2);
            }
        } else {
            if ((castle & 4) != 0) {
                gen_push(E8, G8, 2);
            }
            if ((castle & 8) != 0) {
                gen_push(E8, C8, 2);
            }
        }

        /* generate en passant moves */
        if (ep != -1) {
            if (side == LIGHT) {
                if ((ep & 7) != 0 && color[ep + 7] == LIGHT && piece[ep + 7] == PAWN) {
                    gen_push(ep + 7, ep, 21);
                }
                if ((ep & 7) != 7 && color[ep + 9] == LIGHT && piece[ep + 9] == PAWN) {
                    gen_push(ep + 9, ep, 21);
                }
            } else {
                if ((ep & 7) != 0 && color[ep - 9] == DARK && piece[ep - 9] == PAWN) {
                    gen_push(ep - 9, ep, 21);
                }
                if ((ep & 7) != 7 && color[ep - 7] == DARK && piece[ep - 7] == PAWN) {
                    gen_push(ep - 7, ep, 21);
                }
            }
        }

    }

    private void gen(int c) {
        int p = piece[c];
        for (int d = 0; d < offsets[p]; ++d) {
            int _c = c;
            while (true) {
                _c = mailbox[mailbox64[_c] + offset[p][d]];
                if (_c == -1) {
                    break;
                }
                if (color[_c] != EMPTY) {
                    if (color[_c] == xside) {
                        gen_push(c, _c, 1);
                    }
                    break;
                }
                gen_push(c, _c, 0);
                if (!slide[p]) {
                    break;
                }
            }
        }
    }


    private void gen_push(int from, int to, int bits) {
        if ((bits & 16) != 0) {
            if (side == LIGHT) {
                if (to <= H8) {
                    gen_promote(from, to, bits);
                    return;
                }
            } else if (to >= A1) {
                gen_promote(from, to, bits);
                return;
            }
        }
        pseudomoves.add(new Move((byte) from, (byte) to, (byte) 0, (byte) bits));

    }

    private void gen_promote(int from, int to, int bits) {
        for (int i = KNIGHT; i <= QUEEN; ++i) {
            pseudomoves.add(new Move((byte) from, (byte) to, (byte) i, (byte) (bits | 32)));
        }
    }

    public boolean makemove(Move m) {
        if ((m.bits & 2) != 0) {
            int from;
            int to;

            if (in_check(side)) {
                return false;
            }
            switch (m.to) {
                case 62:
                    if (color[F1] != EMPTY || color[G1] != EMPTY || attack(F1, xside) || attack(G1, xside)) {
                        return false;
                    }
                    from = H1;
                    to = F1;
                    break;
                case 58:
                    if (color[B1] != EMPTY || color[C1] != EMPTY || color[D1] != EMPTY || attack(C1, xside) || attack(D1, xside)) {
                        return false;
                    }
                    from = A1;
                    to = D1;
                    break;
                case 6:
                    if (color[F8] != EMPTY || color[G8] != EMPTY || attack(F8, xside) || attack(G8, xside)) {
                        return false;
                    }
                    from = H8;
                    to = F8;
                    break;
                case 2:
                    if (color[B8] != EMPTY || color[C8] != EMPTY || color[D8] != EMPTY || attack(C8, xside) || attack(D8, xside)) {
                        return false;
                    }
                    from = A8;
                    to = D8;
                    break;
                default: // shouldn't get here
                    from = -1;
                    to = -1;
                    break;
            }
            color[to] = color[from];
            piece[to] = piece[from];
            color[from] = EMPTY;
            piece[from] = EMPTY;
        }

        /* back up information, so we can take the move back later. */
        um.mov = m;
        um.capture = piece[m.to];
        um.castle = castle;
        um.ep = ep;
        um.fifty = fifty;

        castle &= castle_mask[m.from] & castle_mask[m.to];

        if ((m.bits & 8) != 0) {
            if (side == LIGHT) {
                ep = m.to + 8;
            } else {
                ep = m.to - 8;
            }
        } else {
            ep = -1;
        }
        if ((m.bits & 17) != 0) {
            fifty = 0;
        } else {
            ++fifty;
        }

        /* move the piece */
        color[m.to] = side;
        if ((m.bits & 32) != 0) {
            piece[m.to] = m.promote;
        } else {
            piece[m.to] = piece[m.from];
        }
        color[m.from] = EMPTY;
        piece[m.from] = EMPTY;

        /* erase the pawn if this is an en passant move */
        if ((m.bits & 4) != 0) {
            if (side == LIGHT) {
                color[m.to + 8] = EMPTY;
                piece[m.to + 8] = EMPTY;
            } else {
                color[m.to - 8] = EMPTY;
                piece[m.to - 8] = EMPTY;
            }
        }

        side ^= 1;
        xside ^= 1;
        if (in_check(xside)) {
            takeback();
            return false;
        }

        return true;
    }

    public void takeback() {

        side ^= 1;
        xside ^= 1;

        Move m = um.mov;
        castle = um.castle;
        ep = um.ep;
        fifty = um.fifty;

        color[m.from] = side;
        if ((m.bits & 32) != 0) {
            piece[m.from] = PAWN;
        } else {
            piece[m.from] = piece[m.to];
        }
        if (um.capture == EMPTY) {
            color[m.to] = EMPTY;
            piece[m.to] = EMPTY;
        } else {
            color[m.to] = xside;
            piece[m.to] = um.capture;
        }
        if ((m.bits & 2) != 0) {
            int from;
            int to;

            switch (m.to) {
                case 62:
                    from = F1;
                    to = H1;
                    break;
                case 58:
                    from = D1;
                    to = A1;
                    break;
                case 6:
                    from = F8;
                    to = H8;
                    break;
                case 2:
                    from = D8;
                    to = A8;
                    break;
                default: // shouldn't get here
                    from = -1;
                    to = -1;
                    break;
            }
            color[to] = side;
            piece[to] = ROOK;
            color[from] = EMPTY;
            piece[from] = EMPTY;
        }
        if ((m.bits & 4) != 0) {
            if (side == LIGHT) {
                color[m.to + 8] = xside;
                piece[m.to + 8] = PAWN;
            } else {
                color[m.to - 8] = xside;
                piece[m.to - 8] = PAWN;
            }
        }
    }


}
