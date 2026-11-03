package okay.sudoku

import okay.business.scala3.*
import okay.sudoku.Rules.{Grid, Level, Puzzle}

/**
 * SUDOKU, in okay-ui's words alone: a menu of levels, the board — 81 cells, each a block marked by what it holds, a
 * free cell a button at its slot —, the digits' pad, a hint with its reason, a check, the end. The puzzle it asks of
 * `Puzzles`, the person of `Ui`.
 */
object Sudoku:
  type App[X] = (Ui + Puzzles)[X]

  def main[R[_]: Has[Ui]: Has[Puzzles]]: Unit ! R =
    menu[Int, R]("Sudoku", "Easy" -> 1, "Medium" -> 2, "Hard" -> 3, "How to play" -> 4).flatMap {
      case 4 => inform[R]("How to play", howTo).andThen(defer(main[R]))
      case n => game[R](Level.fromOrdinal(n - 1)).andThen(defer(main[R]))
    }

  val howTo: String = "Fill the grid so that every row, every column and every 3 × 3 box holds the digits 1 to 9, " +
    "each once. Choose a free cell, then a digit; Erase empties it. A digit that repeats in its row, column or box " +
    "turns red. Hint gives one step with its reason; Check marks the digits that are wrong. Easy and Medium are " +
    "solved by such steps alone; Hard may need a guess."

  final case class Game(level: Level, puzzle: Puzzle, grid: Grid, selected: Option[Int], wrong: Set[Int],
      note: Option[Part], hints: Int):
    def fixed(i: Int): Boolean = puzzle.givens(i) != 0
    def left: Int = grid.count(_ == 0)

  enum Act:
    case Select(i: Int)
    case Put(d: Int)
    case Hint, Check, Menu

  def game[R[_]: Has[Ui]: Has[Puzzles]](level: Level): Unit ! R =
    perform[Puzzles](Puzzles.Draw(level)).flatMap(p => turn[R](Game(level, p, p.givens, None, Set.empty, None, 0)))

  /** the board: a block of 81 cells; a cell's mark says what it holds (`given`, `entered`, `empty`), then `-bad` (a
   * repeat or a wrong digit), `-sel` (chosen) or `-near` (in the chosen cell's row, column or box) */
  def board(g: Game): Part =
    val bad = Rules.conflicts(g.grid) ++ g.wrong
    Part.block("sudoku")(Rules.cells.map { i =>
      val kind = if g.fixed(i) then "given" else if g.grid(i) != 0 then "entered" else "empty"
      val near = g.selected.exists(s => s != i && Rules.peers(s).contains(i))
      val mark = s"cell-$kind" + (if bad(i) then "-bad" else "") +
        (if g.selected.contains(i) then "-sel" else if near then "-near" else "")
      val digit = if g.grid(i) != 0 then List(Part.text(g.grid(i).toString)) else Nil
      Part.block(mark)((digit ++ (if g.fixed(i) then Nil else List(Part.slot(s"c$i"))))*)
    }*)

  def turn[R[_]: Has[Ui]](g: Game): Unit ! R =
    val status = s"${g.level} · ${g.left} cells left" + (if g.hints > 0 then s" · hints ${g.hints}" else "") +
      g.selected.fold(" · choose a cell")(i => s" · ${Rules.name(i)} chosen")
    val cells = Rules.cells.filterNot(g.fixed).map(i => Button(Rules.name(i))(Act.Select(i)).at(s"c$i"))
    val pad = (1 to 9).map(d => Button(d.toString)(Act.Put(d)).at("pad")) :+ Button("Erase")(Act.Put(0)).at("pad")
    show(Screen[Act]("Sudoku")((Seq(board(g), Part.text(status)) ++ g.note :+ Part.block("pad")(Part.slot("pad")))*)(
      (cells ++ pad ++ Seq(Button("Hint")(Act.Hint), Button("Check")(Act.Check), Button("Menu")(Act.Menu)))*))
      .flatMap {
        case Act.Menu => pure(())
        case Act.Select(i) => defer(turn[R](g.copy(selected = Some(i), note = None)))
        case Act.Put(d) => put[R](g, d)
        case Act.Check =>
          val wrong = Rules.cells.filter(i => g.grid(i) != 0 && g.grid(i) != g.puzzle.solution(i)).toSet
          val said = if wrong.isEmpty then Part.info("Everything so far is right.")
            else Part.warning(s"${if wrong.size == 1 then "1 digit is" else s"${wrong.size} digits are"} wrong, " +
              "marked in red.")
          defer(turn[R](g.copy(wrong = wrong, note = Some(said))))
        case Act.Hint => defer(turn[R](hint(g)))
      }

  /** a hint: a wrong digit first; else one logical step and its reason, its cell chosen; else the cell with the
   * fewest candidates */
  def hint(g: Game): Game =
    val counted = g.copy(hints = g.hints + 1)
    Rules.cells.find(i => g.grid(i) != 0 && g.grid(i) != g.puzzle.solution(i)) match
      case Some(i) => counted.copy(selected = Some(i), wrong = g.wrong + i,
          note = Some(Part.warning(s"${Rules.name(i)} is wrong: it cannot be ${g.grid(i)}.")))
      case None => Rules.step(g.grid) match
        case Some((i, _, why)) => counted.copy(selected = Some(i), note = Some(Part.info(why)))
        case None =>
          val i = Rules.cells.filter(g.grid(_) == 0).minBy(Rules.candidates(g.grid, _).size)
          val c = Rules.candidates(g.grid, i).toList.sorted.mkString(", ")
          counted.copy(selected = Some(i), note = Some(Part.info(s"No single step is left. ${Rules.name(i)} has the " +
            s"fewest choices: $c — try one; Check tells you later if it was wrong.")))

  def solved[R[_]: Has[Ui]](g: Game): Unit ! R =
    val hints = if g.hints == 0 then "with no hints" else if g.hints == 1 then "with 1 hint" else s"with ${g.hints} hints"
    show(Screen[Unit]("Solved")(board(g.copy(selected = None)), Part.info(s"${g.level} solved $hints."))(
      Button("Back to the menu", Style.Primary)(())))

  /** a digit into the chosen cell (0 empties it); the game solved when the grid is the solution */
  def put[R[_]: Has[Ui]](g: Game, d: Int): Unit ! R = g.selected.filterNot(g.fixed) match
    case None => defer(turn[R](g.copy(note = Some(Part.warning("Choose a free cell first.")))))
    case Some(i) =>
      val now = g.copy(grid = g.grid.updated(i, d), wrong = g.wrong - i, note = None)
      if now.grid == g.puzzle.solution then solved[R](now) else defer(turn[R](now))
