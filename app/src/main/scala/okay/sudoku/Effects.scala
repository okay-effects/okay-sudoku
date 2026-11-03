package okay.sudoku

import okay.business.scala3.*
import okay.sudoku.Rules.{Level, Puzzle}

/** A HANDLER, as this application names one: the effect `E` taken out of any program's row */
trait Handles[E[_]]:
  def apply[R[_], Rest[_], A](p: A ! R)(using Split[E, R, Rest]): A ! Rest

/** THE PUZZLE OF A GAME, as an effect: the program never makes one, it asks */
enum Puzzles[X]:
  case Draw(level: Level) extends Puzzles[Puzzle]

object Puzzles:
  /** a new puzzle each time, by the generator and `random` */
  def random(random: scala.util.Random = new scala.util.Random()): Handles[Puzzles] =
    Drawn(level => Rules.generate(level, random))
  /** this puzzle, whatever the level — for tests */
  def fixed(p: Puzzle): Handles[Puzzles] = Drawn(_ => p)

final class Drawn(make: Level => Puzzle) extends Handles[Puzzles]:
  def apply[R[_], Rest[_], A](p: A ! R)(using Split[Puzzles, R, Rest]): A ! Rest =
    handle(p, Handler.Tail[Puzzles, Rest, A, A](a => pure(a), [X] => (asked: Puzzles[X]) => asked match
      case Puzzles.Draw(level) => make(level)))
