package com.anymindbreaker.feature.sudoku.domain

import kotlin.random.Random

/** Rows, columns and boxes of a 9×9 grid stored as 81 cells, row by row. 0 means an empty cell. */
internal object SudokuUnits {
    const val SIZE = 9
    const val CELLS = 81
    const val ALL_DIGITS = 0x1FF

    val rows: List<IntArray> = List(SIZE) { r -> IntArray(SIZE) { c -> r * SIZE + c } }
    val cols: List<IntArray> = List(SIZE) { c -> IntArray(SIZE) { r -> r * SIZE + c } }
    val boxes: List<IntArray> = List(SIZE) { b ->
        IntArray(SIZE) { k -> (b / 3 * 3 + k / 3) * SIZE + (b % 3 * 3 + k % 3) }
    }
    val all: List<IntArray> = rows + cols + boxes

    val peers: Array<IntArray> = Array(CELLS) { cell ->
        all.filter { cell in it }
            .flatMap { it.asIterable() }
            .filter { it != cell }
            .distinct()
            .toIntArray()
    }

    fun rowOf(cell: Int) = cell / SIZE
    fun colOf(cell: Int) = cell % SIZE
    fun boxOf(cell: Int) = cell / SIZE / 3 * 3 + cell % SIZE / 3
    fun bit(digit: Int) = 1 shl (digit - 1)
}

object SudokuSolver {

    /** True when the grid is completely filled and every row, column and box holds 1..9. */
    fun isValidSolution(grid: IntArray): Boolean {
        if (grid.size != SudokuUnits.CELLS) return false
        return SudokuUnits.all.all { unit ->
            var mask = 0
            for (cell in unit) {
                val digit = grid[cell]
                if (digit !in 1..9) return@all false
                mask = mask or SudokuUnits.bit(digit)
            }
            mask == SudokuUnits.ALL_DIGITS
        }
    }

    /** Counts solutions of [grid], stopping as soon as [limit] is reached. */
    fun countSolutions(grid: IntArray, limit: Int = 2): Int = Search(grid, limit, null).run().count

    fun solve(grid: IntArray): IntArray? = Search(grid, 1, null).run().firstSolution

    /** A random completely filled valid grid. */
    fun randomSolution(random: Random): IntArray =
        checkNotNull(Search(IntArray(SudokuUnits.CELLS), 1, random).run().firstSolution)

    private class Search(grid: IntArray, private val limit: Int, private val random: Random?) {
        private val cells = grid.copyOf()
        private val rowUsed = IntArray(SudokuUnits.SIZE)
        private val colUsed = IntArray(SudokuUnits.SIZE)
        private val boxUsed = IntArray(SudokuUnits.SIZE)
        private var consistent = cells.size == SudokuUnits.CELLS

        var count = 0
            private set
        var firstSolution: IntArray? = null
            private set

        init {
            if (consistent) {
                for (cell in cells.indices) {
                    val digit = cells[cell]
                    if (digit == 0) continue
                    if (digit !in 1..9 || !canPlace(cell, digit)) {
                        consistent = false
                        break
                    }
                    mark(cell, digit)
                }
            }
        }

        fun run(): Search {
            if (consistent) search()
            return this
        }

        private fun canPlace(cell: Int, digit: Int): Boolean {
            val bit = SudokuUnits.bit(digit)
            return rowUsed[SudokuUnits.rowOf(cell)] and bit == 0 &&
                colUsed[SudokuUnits.colOf(cell)] and bit == 0 &&
                boxUsed[SudokuUnits.boxOf(cell)] and bit == 0
        }

        private fun mark(cell: Int, digit: Int) {
            val bit = SudokuUnits.bit(digit)
            rowUsed[SudokuUnits.rowOf(cell)] = rowUsed[SudokuUnits.rowOf(cell)] or bit
            colUsed[SudokuUnits.colOf(cell)] = colUsed[SudokuUnits.colOf(cell)] or bit
            boxUsed[SudokuUnits.boxOf(cell)] = boxUsed[SudokuUnits.boxOf(cell)] or bit
        }

        private fun unmark(cell: Int, digit: Int) {
            val bit = SudokuUnits.bit(digit).inv()
            rowUsed[SudokuUnits.rowOf(cell)] = rowUsed[SudokuUnits.rowOf(cell)] and bit
            colUsed[SudokuUnits.colOf(cell)] = colUsed[SudokuUnits.colOf(cell)] and bit
            boxUsed[SudokuUnits.boxOf(cell)] = boxUsed[SudokuUnits.boxOf(cell)] and bit
        }

        private fun candidates(cell: Int): Int =
            SudokuUnits.ALL_DIGITS and
                (rowUsed[SudokuUnits.rowOf(cell)] or
                    colUsed[SudokuUnits.colOf(cell)] or
                    boxUsed[SudokuUnits.boxOf(cell)]).inv()

        private fun search() {
            // Branch on the empty cell with the fewest candidates.
            var best = -1
            var bestMask = 0
            var bestCount = Int.MAX_VALUE
            for (cell in cells.indices) {
                if (cells[cell] != 0) continue
                val mask = candidates(cell)
                val size = Integer.bitCount(mask)
                if (size == 0) return
                if (size < bestCount) {
                    best = cell
                    bestMask = mask
                    bestCount = size
                    if (size == 1) break
                }
            }
            if (best == -1) {
                count++
                if (firstSolution == null) firstSolution = cells.copyOf()
                return
            }

            val digits = (1..9).filter { bestMask and SudokuUnits.bit(it) != 0 }
            for (digit in if (random != null) digits.shuffled(random) else digits) {
                cells[best] = digit
                mark(best, digit)
                search()
                unmark(best, digit)
                cells[best] = 0
                if (count >= limit) return
            }
        }
    }
}
