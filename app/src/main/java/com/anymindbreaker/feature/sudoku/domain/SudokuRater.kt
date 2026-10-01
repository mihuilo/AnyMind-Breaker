package com.anymindbreaker.feature.sudoku.domain

/** The hardest kind of reasoning a puzzle requires. */
enum class SudokuTechniqueLevel {
    /** Naked and hidden singles are enough. */
    SINGLES,

    /** Needs locked candidates or naked pairs. */
    INTERMEDIATE,

    /** Cannot be finished with the techniques above. */
    ADVANCED,
}

/**
 * Rates a puzzle by solving it the way a person would, so difficulty reflects the reasoning
 * required and not just the number of empty cells.
 */
object SudokuRater {

    fun rate(grid: IntArray): SudokuTechniqueLevel {
        val cells = grid.copyOf()
        val candidates = IntArray(SudokuUnits.CELLS) { SudokuUnits.ALL_DIGITS }
        for (cell in cells.indices) {
            if (cells[cell] != 0) eliminateFromPeers(cells, candidates, cell)
        }

        var usedIntermediate = false
        while (true) {
            if (cells.none { it == 0 }) {
                return if (usedIntermediate) SudokuTechniqueLevel.INTERMEDIATE else SudokuTechniqueLevel.SINGLES
            }
            if (placeSingles(cells, candidates)) continue
            if (lockedCandidates(cells, candidates) || nakedPairs(cells, candidates)) {
                usedIntermediate = true
                continue
            }
            return SudokuTechniqueLevel.ADVANCED
        }
    }

    private fun eliminateFromPeers(cells: IntArray, candidates: IntArray, cell: Int) {
        val keep = SudokuUnits.bit(cells[cell]).inv()
        candidates[cell] = 0
        for (peer in SudokuUnits.peers[cell]) candidates[peer] = candidates[peer] and keep
    }

    private fun place(cells: IntArray, candidates: IntArray, cell: Int, digit: Int) {
        cells[cell] = digit
        eliminateFromPeers(cells, candidates, cell)
    }

    private fun placeSingles(cells: IntArray, candidates: IntArray): Boolean {
        var progress = false
        for (cell in cells.indices) {
            if (cells[cell] == 0 && Integer.bitCount(candidates[cell]) == 1) {
                place(cells, candidates, cell, Integer.numberOfTrailingZeros(candidates[cell]) + 1)
                progress = true
            }
        }
        for (unit in SudokuUnits.all) {
            for (digit in 1..9) {
                val bit = SudokuUnits.bit(digit)
                var only = -1
                var count = 0
                for (cell in unit) {
                    if (cells[cell] == 0 && candidates[cell] and bit != 0) {
                        only = cell
                        count++
                    }
                }
                if (count == 1) {
                    place(cells, candidates, only, digit)
                    progress = true
                }
            }
        }
        return progress
    }

    /** Pointing and claiming: a digit confined to the overlap of a box and a line. */
    private fun lockedCandidates(cells: IntArray, candidates: IntArray): Boolean {
        var progress = false
        val lines = SudokuUnits.rows + SudokuUnits.cols
        for (box in SudokuUnits.boxes) {
            for (line in lines) {
                val overlap = box.filter { it in line }
                if (overlap.isEmpty()) continue
                for (digit in 1..9) {
                    val bit = SudokuUnits.bit(digit)
                    fun has(cell: Int) = cells[cell] == 0 && candidates[cell] and bit != 0
                    if (overlap.none(::has)) continue
                    val boxRest = box.filter { it !in overlap }
                    val lineRest = line.filter { it !in overlap }
                    val target = when {
                        boxRest.none(::has) -> lineRest
                        lineRest.none(::has) -> boxRest
                        else -> continue
                    }
                    for (cell in target) {
                        if (has(cell)) {
                            candidates[cell] = candidates[cell] and bit.inv()
                            progress = true
                        }
                    }
                }
            }
        }
        return progress
    }

    private fun nakedPairs(cells: IntArray, candidates: IntArray): Boolean {
        var progress = false
        for (unit in SudokuUnits.all) {
            val open = unit.filter { cells[it] == 0 }
            for (i in open.indices) {
                val mask = candidates[open[i]]
                if (Integer.bitCount(mask) != 2) continue
                for (j in i + 1 until open.size) {
                    if (candidates[open[j]] != mask) continue
                    for (cell in open) {
                        if (cell != open[i] && cell != open[j] && candidates[cell] and mask != 0) {
                            candidates[cell] = candidates[cell] and mask.inv()
                            progress = true
                        }
                    }
                }
            }
        }
        return progress
    }
}
