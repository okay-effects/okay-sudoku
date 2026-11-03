//> using scala 3.3.8
//> using dep com.microsoft.playwright:playwright:1.63.0

import com.microsoft.playwright.{Browser, BrowserType, Page, Playwright}
import com.microsoft.playwright.options.AriaRole
import java.nio.file.Path
import scala.jdk.CollectionConverters.*

/** THE PAGE END TO END in the installed Chromium (`$CHROME`, else Playwright's chromium-1234; never a download):
 * each level made (and timed), a hint followed, a digit checked; pictures into the directory given (default /tmp).
 * `scala-cli run scripts/e2e.scala -- app/.js/target/web/index.html [pictures dir]`. Exit 1 on the first wrong thing */
@main def e2e(page: String, pictures: String = "/tmp"): Unit =
  try run(page, pictures)
  catch case e: Throwable => { e.printStackTrace(); sys.exit(1) }

def run(page: String, pictures: String): Unit =
  val exe = sys.env.getOrElse("CHROME", sys.props("user.home") +
    "/Library/Caches/ms-playwright/chromium-1234/chrome-mac-arm64/" +
    "Google Chrome for Testing.app/Contents/MacOS/Google Chrome for Testing")
  def fail(why: String): Nothing = { System.err.println("FAIL: " + why); sys.exit(1) }
  val pw = Playwright.create(Playwright.CreateOptions().setEnv(Map("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD" -> "1").asJava))
  val browser = pw.chromium().launch(BrowserType.LaunchOptions().setExecutablePath(Path.of(exe)))
  val p = browser.newContext(Browser.NewContextOptions().setViewportSize(500, 1000)).newPage()
  p.setDefaultTimeout(30000)
  val errors = scala.collection.mutable.ArrayBuffer.empty[String]
  p.onPageError(e => errors += e)
  p.navigate(Path.of(page).toAbsolutePath.toUri.toString)
  def button(name: String) = p.getByRole(AriaRole.BUTTON, Page.GetByRoleOptions().setName(name).setExact(true))
  // a button at a slot (a cell, a digit of the pad) is a link of the page
  def slotted(name: String) = p.getByRole(AriaRole.LINK, Page.GetByRoleOptions().setName(name).setExact(true))
  def has(s: String) = p.waitForFunction(s"() => document.body.textContent.includes('$s')")
  def shot(name: String) = p.screenshot(Page.ScreenshotOptions().setPath(Path.of(pictures, name)).setFullPage(true))
  for level <- Seq("Hard", "Medium", "Easy") do
    val t0 = System.nanoTime()
    button(level).click(); has("cells left")
    println(s"$level: made in ${(System.nanoTime() - t0) / 1000000} ms")
    if level != "Easy" then { button("Menu").click(); has("How to play") }
  button("Hint").click()
  p.waitForFunction("() => /can only be|fits only/.test(document.body.textContent)")
  val hint = p.locator("#app").innerText().linesIterator.find(l => l.contains("can only be") || l.contains("fits only")).getOrElse("")
  // the hint's cell is chosen: its digit is in the hint's text ("R4C7 can only be 5: ...")
  val digit = " can only be (\\d)".r.findFirstMatchIn(hint).orElse("In .*, (\\d) fits only".r.findFirstMatchIn(hint))
    .fold(fail("no digit in the hint: " + hint))(_.group(1))
  shot("sudoku-hint.png")
  slotted(digit).click(); has("chosen")
  button("Check").click(); has("Everything so far is right.")
  shot("sudoku-check.png")
  if errors.nonEmpty then fail("page errors: " + errors.mkString("; "))
  println(s"e2e: ok (the hint: $hint)")
  browser.close(); pw.close()
