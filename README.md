# okay-sudoku

Sudoku in a page, written on [okay-ui](https://github.com/okay-effects/okay-ui) — business screens as data on
algebraic effects — and drawn by [okay-faces](https://github.com/okay-effects/okay-faces)' Laminar face.
Play: https://okay-effects.github.io/okay-sudoku/

```sh
sbt appJS/web          # links the page into app/.js/target/web
open app/.js/target/web/index.html
```

Three levels, each puzzle made in the page with a unique solution; a hint gives one step with its reason; Check marks
the wrong digits. The whole application in the browser is one line (`InTheBrowser`).
