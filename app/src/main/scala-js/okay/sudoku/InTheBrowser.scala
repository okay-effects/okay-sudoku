package okay.sudoku

import okay.business.faces.{OnFaces, Skin}
import okay.business.laminar.Page
import okay.business.scala3.steps
import okay.sudoku.Sudoku.App

/** SUDOKU IN THE BROWSER: `sbt appJS/web`, then open app/.js/target/web/index.html — a new puzzle made in the page,
 * the screens on okay-faces' Laminar face. The whole application is the line below */
object InTheBrowser:
  /** okay-ui's calm look, and the board: a 9 x 9 grid of cells, its boxes drawn thick; a free cell's button covers
   * it, its name read by screen readers but not drawn; the pad's ten buttons in two rows */
  val skin: Skin =
    val calm = Skin.calm(accent = "#3b5bdb", deep = "#1e2a5a")
    calm.copy(css = calm.css + """
      |body > footer .bar { display: flex; flex-wrap: wrap; align-items: baseline; gap: 6px; }
      |body > footer .more { margin-left: auto; }
      |body .okay-mark-sudoku { display: block; }
      |body .okay-mark-sudoku > .okay-group { display: grid; grid-template-columns: repeat(9, minmax(0, 1fr)); gap: 0;
      |  box-sizing: border-box; width: min(100%, 450px); margin: 0 auto; border: 2px solid var(--okay-fg); padding: 0;
      |  background: var(--okay-surface); }
      |body .okay-mark-sudoku > .okay-group > .okay-mark { position: relative; display: block; aspect-ratio: 1;
      |  box-sizing: border-box; min-width: 0;
      |  border-right: 1px solid var(--okay-border); border-bottom: 1px solid var(--okay-border); }
      |body .okay-mark-sudoku > .okay-group > .okay-mark:nth-child(9n+3),
      |body .okay-mark-sudoku > .okay-group > .okay-mark:nth-child(9n+6) { border-right: 2px solid var(--okay-fg); }
      |body .okay-mark-sudoku > .okay-group > .okay-mark:nth-child(9n) { border-right: 0; }
      |body .okay-mark-sudoku > .okay-group > .okay-mark:nth-child(n+19):nth-child(-n+27),
      |body .okay-mark-sudoku > .okay-group > .okay-mark:nth-child(n+46):nth-child(-n+54) {
      |  border-bottom: 2px solid var(--okay-fg); }
      |body .okay-mark-sudoku > .okay-group > .okay-mark:nth-child(n+73) { border-bottom: 0; }
      |body .okay-mark-sudoku .okay-mark > .okay-group { position: absolute; inset: 0; display: flex;
      |  align-items: center; justify-content: center; margin: 0; padding: 0; gap: 0; border: 0; background: none;
      |  box-shadow: none; }
      |body .okay-mark-sudoku .okay-text { margin: 0; font-size: clamp(18px, 5.5vw, 28px); line-height: 1;
      |  font-weight: 700; color: var(--okay-fg); }
      |body .okay-mark-sudoku [class*="okay-mark-cell-entered"] .okay-text { color: var(--ui-accent); font-weight: 600; }
      |body .okay-mark-sudoku [class*="-bad"] .okay-text { color: var(--okay-problem); }
      |body .okay-mark-sudoku [class*="-near"] { background: color-mix(in srgb, var(--ui-accent) 7%, transparent); }
      |body .okay-mark-sudoku [class*="-sel"] { background: color-mix(in srgb, var(--ui-accent) 22%, transparent); }
      |body .okay-mark-sudoku .okay-mark-slot { position: absolute; inset: 0; display: block; }
      |body .okay-mark-sudoku .okay-mark-slot > .okay-group, body .okay-mark-sudoku .okay-mark-button { display: block;
      |  position: absolute; inset: 0; margin: 0; padding: 0; }
      |body .okay-mark-sudoku .okay-mark-button > .okay-ref { position: absolute; inset: 0; display: block;
      |  min-height: 0; padding: 0; margin: 0; border: 0; border-radius: 0; background: transparent; box-shadow: none;
      |  color: transparent; font-size: 0; cursor: pointer; }
      |body .okay-mark-sudoku .okay-mark-button > .okay-ref:hover {
      |  background: color-mix(in srgb, var(--ui-accent) 12%, transparent); }
      |body .okay-mark-pad .okay-mark-slot > .okay-group { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr));
      |  gap: 8px; width: min(100%, 450px); margin: 0 auto; box-sizing: border-box; }
      |body .okay-mark-pad .okay-mark-button { display: block; min-width: 0; }
      |body .okay-mark-pad .okay-mark-button > .okay-ref { box-sizing: border-box; width: 100%; min-width: 0;
      |  padding: 10px 0; font-size: 20px; font-weight: 700; }
      |""".stripMargin)

  def main(args: Array[String]): Unit =
    Page.mount(OnFaces.program(steps(Puzzles.random()(Sudoku.main[App]))), skin)
