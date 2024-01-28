import PieceType.Bishop
import PieceType.King
import PlayerColor.Black
import PlayerColor.White
import org.junit.Test
import kotlin.test.*

class ChessBoardTest {

    @Test
    fun `should create an empty board`() {
        assertEquals(
            listOf(
            "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........",
                "........"
            ).joinToString("\n", postfix = "\n"),
            ChessBoard().toString()
        )
    }

    @Test
    fun `should create a simple board with a couple of pieces`() {
        val board = ChessBoard()
        board[0, 1] = Bishop.asWhite
        board[4, 4] = King.asBlack
        assertEquals(
            listOf(
            ".B......",
                "........",
                "........",
                "........",
                "....r...",
                "........",
                "........",
                "........"
            ).joinToString("\n", postfix = "\n"),
            board.toString()
        )
        assertTrue(board.isMoveAllowed(4 to 4, 4 to 5))
        assertTrue(board.isMoveAllowed(4 to 4, 4 to 3))
        assertTrue(board.isMoveAllowed(4 to 4, 5 to 4))
        assertTrue(board.isMoveAllowed(4 to 4, 3 to 4))
        assertFalse(board.isMoveAllowed(4 to 4, 2 to 4))
    }
}
