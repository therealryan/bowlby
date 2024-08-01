package dev.flowty.bowlby.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Exercises {@link Xml}
 */
@SuppressWarnings("static-method")
class XmlTest {

  /**
   * Empty elements are self-closed
   */
  @Test
  void elmmlmpty() {
    assertEquals( "<html/>",
        new Html()
            .toString() );
  }

  /**
   * Attributes can be added
   */
  @Test
  void atrttr() {
    assertEquals( "<html name=\"value\"/>",
        new Html()
            .atr( "name", "value" )
            .toString() );
  }

  /**
   * Element contents can be added
   */
  @Test
  void txtext() {
    assertEquals( "<html>text</html>",
        new Html()
            .txt( "text" )
            .toString() );
  }

  /**
   * Attributes can be set at any point, but text and child element order matters
   */
  @Test
  void interleaving() {
    assertEquals( "<html a=\"1\" b=\"2\">foo<child>element</child>bar</html>",
        new Html()
            .atr( "b", "2" )
            .txt( "foo" )
            .elm( "child", "element" )
            .atr( "a", "1" )
            .txt( "bar" )
            .toString() );
  }

  /**
   * Child elements can be added
   */
  @Test
  void child() {
    assertEquals( """
        <html>\
        <empty/>\
        <flat>text</flat>\
        <root><branch><leaf/></branch></root>\
        </html>""",
        new Html()
            .elm( "empty" )
            .elm( "flat", "text" )
            .elm( "root", r -> r
                .elm( "branch", b -> b
                    .elm( "leaf" ) ) )
            .toString() );
  }
}
