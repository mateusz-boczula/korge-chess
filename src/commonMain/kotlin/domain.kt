import korlibs.datastructure.*
import korlibs.math.geom.*

class ChessBoard(private val array: Array2<Square> = Array2(8, 8) { EmptySquare }) {

    override fun toString() = StringBuilder().also {
        (0 until this.array.height).forEach { x ->
            (0 until this.array.width).forEach { y ->
                it.append(array[x, y])
            }
            it.append("\n")
        }
    }.toString()

    suspend fun forEachSquaresIndexed(consumer: suspend (point: PointInt, square: Square) -> Unit) =
        (0 until array.height).forEach { x ->
            (0 until array.width).forEach { y ->
                consumer(PointInt(x, y), array[x,y])
            }
        }

    operator fun get(x: Int, y: Int) = array[x, y]

    operator fun set(x: Int, y: Int, value: Square) {
        array[x, y] = value
    }

    fun getValidMoves(x: Int, y: Int): List<Pair<Int, Int>> = when(val square = this[x, y]) {
        EmptySquare -> listOf()
        is Piece -> square.type.moveList(x, y, square.color)
            .map { movesInDirection -> movesInDirection
                .filter { it.isInBounds }
                .takeWhile { this[it.first, it.second] == EmptySquare } }
            .flatten() +
            square.type.takeList(x, y, square.color).mapNotNull { movesInDirection ->
                movesInDirection
                    .filter { it.isInBounds }
                    .find {
                        when (val targetSquare = this[it.first, it.second]) {
                            is Piece -> targetSquare.color != square.color
                            else -> false
                        }
                    }
            }
    }


    private val Pair<Int, Int>.isInBounds
        get() = first >= 0 && second >= 0 && first < 8 && second < 8

    companion object {
        fun createStartingBoard(): ChessBoard {
            val board = ChessBoard()
            (0 until 8).forEach {x ->
                board.placeForBothColors(x, 1, PieceType.Pawn)
            }
            board.doublePlaceForBothColors(0, 0, PieceType.Rook)
            board.doublePlaceForBothColors(1, 0, PieceType.Knight)
            board.doublePlaceForBothColors(2, 0, PieceType.Bishop)
            board.placeForBothColors(3, 0, PieceType.Queen)
            board.placeForBothColors(4, 0, PieceType.King)

            return board
        }

        private fun ChessBoard.placeForBothColors(x: Int, y: Int, piece: PieceType) {
            this[x, y] = piece.asBlack
            this[x, 7-y] = piece.asWhite
        }

        private fun ChessBoard.doublePlaceForBothColors(x: Int, y: Int, piece: PieceType) {
            placeForBothColors(x, y, piece)
            placeForBothColors(7-x, y, piece)
        }
    }
}

sealed class PieceType(val code: String, val key: Char) {
    data object Pawn: PieceType("pawn",'P') {
        override fun moveList(x: Int, y: Int, color: PlayerColor) = when(color) {
            PlayerColor.Black -> listOf(listOf(x to y+1))
            PlayerColor.White -> listOf(listOf(x to y-1))
        }

        override fun takeList(x: Int, y: Int, color: PlayerColor) = when(color) {
            PlayerColor.Black -> listOf(listOf(x+1 to y+1), listOf(x-1 to y+1))
            PlayerColor.White -> listOf(listOf(x+1 to y-1), listOf(x-1 to y-1))
        }

    }
    data object Knight: PieceType("knight", 'K') {
        override fun moveList(x: Int, y: Int, color: PlayerColor) = listOf(
            x+1 to y+2, x+1 to y-2, x-1 to y+2, x-1 to y-2,
            x+2 to y+1, x+2 to y-1, x-2 to y+1, x-2 to y-1,
        ).map { listOf(it) }
    }
    data object Bishop: PieceType("bishop", 'B') {
        override fun moveList(x: Int, y: Int, color: PlayerColor): List<List<Pair<Int, Int>>> {
            val moveList = listOf(mutableListOf<Pair<Int, Int>>(), mutableListOf(), mutableListOf(), mutableListOf())
            (1 until 8)
                .forEach {
                    listOf(x+it to y+it, x+it to y-it, x-it to y+it, x-it to y-it)
                        .zip(moveList)
                        .forEach{(move, list) -> list.add(move)}
                }

            return moveList.map { it.toList() }
        }
    }
    data object Rook: PieceType("rook", 'R') {
        override fun moveList(x: Int, y: Int, color: PlayerColor): List<List<Pair<Int, Int>>> {
            val moveList = listOf(mutableListOf<Pair<Int, Int>>(), mutableListOf(), mutableListOf(), mutableListOf())

            (1 until 8)
                .forEach {
                    listOf(x to y+it, x to y-it, x+it to y, x-it to y)
                        .zip(moveList)
                        .forEach{ (move, list) -> list.add(move) }
                }

            return moveList.map { it.toList() }
        }
    }
    data object Queen: PieceType("queen", 'Q') {
        override fun moveList(x: Int, y: Int, color: PlayerColor) =
            (Rook.moveList(x, y, color) + Bishop.moveList(x, y, color))
    }
    data object King: PieceType("king", 'R') {
        override fun moveList(x: Int, y: Int, color: PlayerColor) =
            listOf(
                listOf(x to y+1),
                listOf(x to y-1),
                listOf(x+1 to y),
                listOf(x-1 to y),
                listOf(x+1 to y+1),
                listOf(x+1 to y-1),
                listOf(x-1 to y+1),
                listOf(x-1 to y-1)
            )
    }

    val asWhite
        get() = Piece(this, PlayerColor.White)
    val asBlack
        get() = Piece(this, PlayerColor.Black)

    override fun toString() = "$key"

    abstract fun moveList(x: Int, y: Int, color: PlayerColor): List<List<Pair<Int, Int>>>

    open fun takeList(x: Int, y: Int, color: PlayerColor): List<List<Pair<Int, Int>>> = moveList(x, y, color)
}

sealed class PlayerColor(val code: String) {
    data object White: PlayerColor("w")
    data object Black: PlayerColor("b")
}

sealed interface Square

data class Piece(
    val type: PieceType,
    val color: PlayerColor
): Square {
    val code = "${color.code}_${type.code}"

    override fun toString() = when(color) {
        PlayerColor.Black -> "${type.key.lowercase()}"
        PlayerColor.White -> "${type.key.uppercase()}"
    }
}

data object EmptySquare: Square {

    override fun toString() = "."
}
