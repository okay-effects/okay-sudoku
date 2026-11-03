package okay.sudoku

import okay.business.scala3.*
import okay.sudoku.Rules.{Level, Puzzle}
import okay.sudoku.Sudoku.App

/** SUDOKU BY SCRIPT: the generator's puzzles are unique and of their level; the whole application played by its
 * buttons, the puzzle fixed */
class SudokuSuite extends munit.FunSuite:
  val easy: Puzzle = Rules.generate(Level.Easy, new scala.util.Random(1))
  def play(acts: Act*): Played[Unit] = Play(steps(Puzzles.fixed(easy)(Sudoku.main[App])), acts*)
  def notes(p: Played[?]): List[String] = p.at.toList.flatMap(_.parts).collect { case Part.Notice(_, s) => s }
  def marks(p: Played[?]): Vector[String] = p.at.toList.flatMap(_.parts).collect {
    case Part.Block("sudoku", _, cells) => cells.collect { case Part.Block(m, _, _) => m }
  }.head.toVector
  val free: Vector[Int] = Rules.cells.filter(easy.givens(_) == 0).toVector
  def fill(cells: Seq[Int], digit: Int => Int = easy.solution): List[Act] =
    cells.toList.flatMap(i => List(Act.press(Rules.name(i)), Act.press(digit(i).toString)))

  test("the generator: a unique solution, the level's clues, Easy and Medium solved by singles alone") {
    for (level, seed) <- Seq(Level.Easy -> 2, Level.Medium -> 3, Level.Hard -> 4) do
      val p = Rules.generate(level, new scala.util.Random(seed))
      assertEquals(Rules.solutions(p.givens, 2), Vector(p.solution), s"$level")
      assert(p.givens.count(_ != 0) >= level.clues, s"$level: ${p.givens.count(_ != 0)} clues")
      assert(Rules.cells.forall(i => p.givens(i) == 0 || p.givens(i) == p.solution(i)))
      if level.bySingles then assertEquals(Rules.singles(p.givens), p.solution, s"$level by singles")
    assertEquals(Rules.conflicts(easy.solution), Set.empty[Int])
  }

  test("a digit into a chosen cell: shown as entered; a repeat in its unit turns red") {
    val i = free.head
    val p = play((Act.press("Easy") +: fill(Seq(i)))*)
    assert(marks(p)(i).startsWith("cell-entered") && !marks(p)(i).contains("-bad"), marks(p)(i))
    val peer = Rules.peers(i).find(easy.givens(_) != 0).get
    val p2 = play((Act.press("Easy") +: fill(Seq(i), _ => easy.givens(peer)))*)
    assert(marks(p2)(i).contains("-bad") && marks(p2)(peer).contains("-bad"), s"${marks(p2)(i)} ${marks(p2)(peer)}")
  }

  test("a digit with no cell chosen is refused with a word; a given cell has no button") {
    val p = play(Act.press("Easy"), Act.press("5"))
    assertEquals(notes(p), List("Choose a free cell first."))
    val g = Rules.cells.find(easy.givens(_) != 0).get
    assert(p.at.get.buttons.forall(_.label != Rules.name(g)))
  }

  test("check marks the wrong digits; the hint names a wrong digit first, then a step with its reason") {
    val i = free.head
    val wrongDigit = (1 to 9).find(d => d != easy.solution(i) && !Rules.peers(i).exists(easy.givens(_) == d))
    val wrongly = wrongDigit.fold(List.empty[Act])(d => fill(Seq(i), _ => d))
    if wrongDigit.isDefined then
      val p = play((Act.press("Easy") +: wrongly :+ Act.press("Check"))*)
      assertEquals(notes(p), List("1 digit is wrong, marked in red."))
      assert(marks(p)(i).contains("-bad"))
      val h = play((Act.press("Easy") +: wrongly :+ Act.press("Hint"))*)
      assert(notes(h).head.startsWith(s"${Rules.name(i)} is wrong"), notes(h).toString)
    val h2 = play(Act.press("Easy"), Act.press("Hint"))
    val (cell, digit, why) = Rules.step(easy.givens).get
    assertEquals(notes(h2), List(why))
    assert(marks(h2)(cell).endsWith("-sel"))
    assertEquals(easy.solution(cell), digit)
  }

  test("every cell filled right: solved, the menu again") {
    val p = play((Act.press("Easy") +: fill(free))*)
    assertEquals(p.at.map(_.title), Some("Solved"))
    assertEquals(notes(p), List("Easy solved with no hints."))
    val back = play((Act.press("Easy") +: fill(free) :+ Act.press("Back to the menu"))*)
    assertEquals(back.at.map(_.title), Some("Sudoku"))
  }

  test("Erase empties a cell; Menu leaves the game") {
    val i = free.head
    val p = play((Act.press("Easy") +: fill(Seq(i)) :+ Act.press("Erase"))*)
    assert(marks(p)(i).startsWith("cell-empty"), marks(p)(i))
    assertEquals(play(Act.press("Easy"), Act.press("Menu")).at.map(_.title), Some("Sudoku"))
  }
