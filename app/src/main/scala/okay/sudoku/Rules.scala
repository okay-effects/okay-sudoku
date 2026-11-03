package okay.sudoku

import scala.util.Random

/** THE RULES, pure: a grid of 81 cells (0 empty), its units, the solver, the generator, the hints with their reasons */
object Rules:
  type Grid = Vector[Int]
  val cells: Range = 0 until 81
  def row(i: Int): Int = i / 9
  def col(i: Int): Int = i % 9
  def box(i: Int): Int = row(i) / 3 * 3 + col(i) / 3
  def name(i: Int): String = s"R${row(i) + 1}C${col(i) + 1}"
  val rows: Vector[Vector[Int]] = Vector.tabulate(9)(r => Vector.tabulate(9)(c => r * 9 + c))
  val cols: Vector[Vector[Int]] = Vector.tabulate(9)(c => Vector.tabulate(9)(r => r * 9 + c))
  val boxes: Vector[Vector[Int]] = Vector.tabulate(9)(b => cells.filter(box(_) == b).toVector)
  /** every unit with its name: "row 3", "column 5", "box 7" */
  val units: Vector[(String, Vector[Int])] = rows.zipWithIndex.map((u, k) => s"row ${k + 1}" -> u) ++
    cols.zipWithIndex.map((u, k) => s"column ${k + 1}" -> u) ++ boxes.zipWithIndex.map((u, k) => s"box ${k + 1}" -> u)
  val peers: Vector[Vector[Int]] = Vector.tabulate(81)(i =>
    cells.filter(j => j != i && (row(j) == row(i) || col(j) == col(i) || box(j) == box(i))).toVector)

  /** the digits cell `i` may still take */
  def candidates(g: Grid, i: Int): Set[Int] =
    if g(i) != 0 then Set.empty else (1 to 9).toSet -- peers(i).map(g)

  /** the cells whose digit is also in one of their units */
  def conflicts(g: Grid): Set[Int] = cells.filter(i => g(i) != 0 && peers(i).exists(g(_) == g(i))).toSet

  /** up to `limit` solutions, by backtracking on the cell with the fewest candidates; `order` the digits' order */
  def solutions(g: Grid, limit: Int, order: Int => Seq[Int] = _ => 1 to 9): Vector[Grid] =
    val a = g.toArray
    val found = Vector.newBuilder[Grid]; var n = 0
    def go(): Unit =
      if n < limit then
        var best = -1; var bestC = Set.empty[Int]
        var i = 0
        while i < 81 && bestC.size != 1 do
          if a(i) == 0 then
            val c = (1 to 9).toSet -- peers(i).map(a(_))
            if best < 0 || c.size < bestC.size then { best = i; bestC = c }
          i += 1
        if best < 0 then { found += a.toVector; n += 1 }
        else if bestC.nonEmpty then
          for d <- order(best) if bestC(d) && n < limit do
            a(best) = d; go(); a(best) = 0
    if conflicts(g).isEmpty then go()
    found.result()

  /** a grid solved by singles alone (naked and hidden), as far as they go */
  def singles(g: Grid): Grid =
    var cur = g; var moved = true
    while moved do
      moved = false
      step(cur).foreach { case (i, d, _) => cur = cur.updated(i, d); moved = true }
    cur

  /** ONE LOGICAL STEP, with its reason: a cell that can take one digit only, or a digit that has one place only in a
   * unit */
  def step(g: Grid): Option[(Int, Int, String)] =
    cells.find(i => g(i) == 0 && candidates(g, i).size == 1).map { i =>
      val d = candidates(g, i).head
      def in(u: Vector[Int]) = u.map(g).filter(_ != 0).distinct.sorted.mkString(", ")
      (i, d, s"${name(i)} can only be $d: its row has ${in(rows(row(i)))}; its column ${in(cols(col(i)))}; its " +
        s"box ${in(boxes(box(i)))}.")
    }.orElse {
      units.iterator.flatMap { (unit, u) =>
        (1 to 9).iterator.filter(d => !u.exists(g(_) == d)).flatMap { d =>
          u.filter(i => candidates(g, i)(d)) match
            case Vector(i) => Some((i, d, s"In $unit, $d fits only at ${name(i)}."))
            case _ => None
        }
      }.nextOption()
    }

  enum Level(val clues: Int, val bySingles: Boolean):
    case Easy extends Level(38, true)
    case Medium extends Level(30, true)
    case Hard extends Level(24, false)

  final case class Puzzle(givens: Grid, solution: Grid)

  /** A PUZZLE: a full grid at random, then its cells emptied in a random order while the solution stays unique (and,
   * for Easy and Medium, singles alone still solve it), down to the level's clues */
  def generate(level: Level, r: Random): Puzzle =
    val full = solutions(Vector.fill(81)(0), 1, _ => r.shuffle((1 to 9).toVector)).head
    var g = full
    for i <- r.shuffle(cells.toVector) if g.count(_ != 0) > level.clues do
      val tried = g.updated(i, 0)
      if solutions(tried, 2).size == 1 && (!level.bySingles || !singles(tried).contains(0)) then g = tried
    Puzzle(g, full)
