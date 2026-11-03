# okay-sudoku — Sudoku in a page, on okay-ui and okay-faces (spec, 2026-11-04)

## The rule

The data model is effects; every implementation is a handler; the browser runs the application in ONE line.

| effect | operation | answered with | handlers |
|---|---|---|---|
| `Puzzles` | `Draw(level)` | a puzzle: its givens and its solution | `Puzzles.random()` (the page), `Puzzles.fixed(p)` (tests) |
| `Ui` | `Show(screen)` | the button pressed | okay-ui's `steps` |

```scala
Page.mount(OnFaces.program(steps(Puzzles.random()(Sudoku.main[App]))), skin)
```

## The game

- The menu: Easy, Medium, Hard, How to play. No Quit (the operator's rule).
- A puzzle (`Rules.generate`): a full grid at random (backtracking, digits shuffled), its cells emptied in a random
  order while the solution stays unique — and for Easy and Medium while naked and hidden singles alone still solve
  it — down to 38 clues (Easy), 30 (Medium), 24 (Hard; it may stop above). Made in the page: ~60 ms Easy, ~100–200 ms
  Medium, ~0.4–0.7 s Hard in Chromium.
- The board: `Part.block("sudoku")` of 81 blocks, each marked by what it holds — `cell-given`, `cell-entered`,
  `cell-empty` — then `-bad` (a digit repeated in its row, column or box, or marked wrong by Check), then `-sel` (the
  chosen cell) or `-near` (its row, column and box). A free cell holds a button at its slot (`Part.slot`), named by
  the cell (R1C3): the skin stretches it over the cell and does not draw its name. A given cell has no button.
- The pad: 1–9 and Erase, buttons at the slot `pad`. A digit with no cell chosen is refused with a word.
- Hint: a wrong digit first ("R3C4 is wrong"); else one logical step with its reason — a naked single ("R4C7 can only
  be 5: its row has …; its column …; its box …") or a hidden single ("In row 4, 5 fits only at R4C7") — its cell
  chosen; else the cell with the fewest candidates. Hints are counted.
- Check: the entered digits that differ from the solution marked red, or "Everything so far is right."
- Solved: when the grid is the solution; the board, the level and the hints used.
- The look: okay-ui's `Skin.calm`; the board a CSS grid of 9 columns (`minmax(0, 1fr)`: a cell's button never widens
  its column), the boxes' borders thick by `nth-child`.

## Checks

- `sbt appJVM/test` — `SudokuSuite`: the generator's puzzles unique, of their level's clues, Easy and Medium solved by
  singles; a digit entered, a repeat red; a digit with no cell; Check and Hint (a wrong digit first, then the step and
  its reason); solved; Erase; Menu.
- `scala-cli run scripts/e2e.scala -- app/.js/target/web/index.html`: each level made and timed in Chromium, a hint
  followed, Check; pictures in /tmp. Published by `scripts/publish.sh`: https://okay-effects.github.io/okay-sudoku/.
