import korlibs.datastructure.*
import korlibs.image.bitmap.Bitmaps.transparent
import korlibs.korge.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.io.file.std.*
import korlibs.korge.input.*
import korlibs.math.*
import korlibs.math.geom.*
import kotlinx.coroutines.*

suspend fun main() = Korge(windowSize = Size(512, 512), backgroundColor = Colors["#2b2b2b"]) {
	val sceneContainer = sceneContainer()

	sceneContainer.changeTo{ MyScene() }
}

class MyScene : PixelatedScene(128*8, 128*8, sceneSmoothing = true) {
	override suspend fun SContainer.sceneMain() {
        val board = ChessBoard.createStartingBoard()
        board[0, 1] = EmptySquare
        board[1, 1] = EmptySquare
        board[3, 1] = EmptySquare
        board[4, 4] = PieceType.Rook.asBlack

        drawBoard(board)
    }

    private suspend fun SContainer.drawBoard(board: ChessBoard) {
        val darkSquareImage = resourcesVfs["gfx/chess_shadow.atlas/square brown dark.png"].readBitmap()
        val lightSquareImage = resourcesVfs["gfx/chess_shadow.atlas/square brown light.png"].readBitmap()

        val voidContainer = container { }
        val boardContainer = container { }
        val moveIndicationContainer = container { }
        val piecesContainer = container { }

        val pieceImages = Array2(8, 8) { voidContainer.image(transparent) }

        board.forEachSquaresIndexed { point, square ->
            val (x, y) = point
            (if ((x + y).isOdd) darkSquareImage else lightSquareImage)
                .let { boardContainer.image(it).xy(x * 128, y * 128) }
            if(square is Piece) {
                pieceImages[x, y] = placePiece(
                    square,
                    point,
                    board,
                    piecesContainer,
                    moveIndicationContainer,
                    voidContainer,
                    pieceImages
                )
            }
        }
    }

    private suspend fun placePiece(
        square: Square,
        point: PointInt,
        board: ChessBoard,
        piecesContainer: Container,
        moveIndicationContainer: Container,
        voidContainer: Container,
        pieceImages: Array2<Image>
    ): Image {
        val (x, y) = point
        return square.bitmap()!!.let { bitmap ->
            piecesContainer.image(bitmap).centered.xy(x * 128 + 64, y * 128 + 64).scale(0.9).also {img ->
                img.mouse {
                    img.draggable { info ->
                        val validMoves = board.getValidMoves(x, y)
                        if (info.start) {
                            validMoves.forEach {
                                moveIndicationContainer.solidRect(128, 128, Colors.GREENYELLOW).alpha(0.5)
                                    .xy(it.first * 128, it.second * 128)
                            }
                        }
                        if (info.end) {
                            moveIndicationContainer.removeChildren()
                            val targetSquareCoordinates = (info.viewNextXY / Size(128, 128)).toInt()
                            if (Pair(targetSquareCoordinates.x, targetSquareCoordinates.y) in validMoves) {
                                performPieceMove(
                                    img,
                                    targetSquareCoordinates,
                                    board,
                                    point,
                                    pieceImages,
                                    voidContainer,
                                    piecesContainer,
                                    moveIndicationContainer
                                )
                            }
                            else {
                                img.xy(x * 128 + 64, y * 128 + 64)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun performPieceMove(
        img: Image,
        targetSquareCoordinates: Vector2I,
        board: ChessBoard,
        point: PointInt,
        pieceImages: Array2<Image>,
        voidContainer: Container,
        piecesContainer: Container,
        moveIndicationContainer: Container
    ) {
        val (x, y) = point
        val movedPiece = board[x, y]
        img.xy(targetSquareCoordinates.x * 128 + 64, targetSquareCoordinates.y * 128 + 64)
        val targetSquare = board[targetSquareCoordinates.x, targetSquareCoordinates.y]
        movePiece(board, targetSquareCoordinates, movedPiece, point)
        if (targetSquare is Piece) {
            removeTargetPieceImage(pieceImages, targetSquareCoordinates, voidContainer, piecesContainer)

        }
        piecesContainer.removeChild(img)
        runBlocking {
            pieceImages[targetSquareCoordinates.x, targetSquareCoordinates.y] =
                placePiece(
                    movedPiece,
                    PointInt(targetSquareCoordinates.x, targetSquareCoordinates.y),
                    board,
                    piecesContainer,
                    moveIndicationContainer,
                    voidContainer,
                    pieceImages
                )
        }
    }

    private fun movePiece(
        board: ChessBoard,
        targetSquareCoordinates: Vector2I,
        piece: Square,
        point: PointInt
    ) {
        val (x, y) = point
        board[targetSquareCoordinates.x, targetSquareCoordinates.y] = piece
        board[x, y] = EmptySquare
    }

    private fun removeTargetPieceImage(
        pieceImages: Array2<Image>,
        targetSquareCoordinates: Vector2I,
        voidContainer: Container,
        piecesContainer: Container
    ) {
        val targetPieceImage = pieceImages[targetSquareCoordinates.x, targetSquareCoordinates.y]
        pieceImages[targetSquareCoordinates.x, targetSquareCoordinates.y] = voidContainer.image(transparent)
        piecesContainer.removeChild(targetPieceImage)
    }
}

suspend fun Square.bitmap() = when(this) {
        EmptySquare -> null
        is Piece -> resourcesVfs["gfx/chess_shadow.atlas/${this.code}.png"].readBitmap()
    }
