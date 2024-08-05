package dev.flowty.bowlby.app.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.Test;

import dev.flowty.bowlby.app.xml.AbstractXml;
import dev.flowty.bowlby.app.xml.Html;

/**
 * Exercises {@link AbstractXml}
 */
@SuppressWarnings("static-method")
class HtmlTest {

  /**
   * Empty elements are self-closed
   */
  @Test
  void empty() {
    assertEquals( "<html></html>", new Html().toString() );

    assertEquals( "<html><br/></html>", new Html().br().toString(),
        "void elements are self-closed" );
    assertEquals( "<html><ul></ul></html>", new Html().ul().toString(),
        "non-void elements are start/end tagged" );
  }

  /**
   * Content is not allowed in void elements
   */
  @Test
  void voidElements() {
    Html html = new Html();

    assertThrows( IllegalArgumentException.class, () -> html
        .elm( "br", b -> b
            .txt( "foo" ) ) );

    assertThrows( IllegalArgumentException.class, () -> html
        .elm( "IMG", b -> b.span( "bar" ) ) );
  }

  /**
   * Attributes can be added
   */
  @Test
  void attributes() {
    assertEquals( "<html name=\"value\"></html>",
        new Html().atr( "name", "value" ).toString() );
  }

  /**
   * Element contents can be added
   */
  @Test
  void text() {
    assertEquals( "<html>text</html>",
        format( new Html().txt( "text" ) ) );
  }

  /**
   * Arbitrary content can be added
   */
  @Test
  void content() {
    assertEquals( """
        <html>
            text with &lt;i&gt;markup!&lt;/i&gt;
            <br>
            text with <i>markup!</i>
        </html>""",
        format( new Html()
            .txt( "text with <i>markup!</i>" )
            .br()
            .cnt( "text with <i>markup!</i>" ) ) );
  }

  /**
   * Attributes can be set at any point, but text and child element order matters
   */
  @Test
  void interleaving() {
    assertEquals( """
        <html a="1" b="2">
            foo
            <child>element</child>
            bar
        </html>""",
        format( new Html()
            .atr( "b", "2" )
            .txt( "foo" )
            .elm( "child", "element" )
            .atr( "a", "1" )
            .txt( "bar" ) ) );
  }

  /**
   * Child elements can be added
   */
  @Test
  void child() {
    assertEquals( """
        <html>
            <empty></empty>
            <flat>text</flat>
            <root>
                <branch>
                    <leaf></leaf>
                </branch>
            </root>
        </html>""",
        format( new Html()
            .elm( "empty" )
            .elm( "flat", "text" )
            .elm( "root", r -> r
                .elm( "branch", b -> b
                    .elm( "leaf" ) ) ) ) );
  }

  /**
   * Sibling elements can be added in separate functions
   */
  @Test
  void sibling() {
    assertEquals( """
        <html>
            <body>
                <span>abc</span><span>def</span>
            </body>
        </html>""",
        format( new Html().body(
            b -> b.span( "abc" ),
            b -> b.span( "def" ) ) ) );
  }

  /**
   * Optional elements can be defined in the call chain
   */
  @Test
  void optional() {
    assertEquals( """
        <html>
            <span>exists!</span>
        </html>""",
        format( new Html()
            .optional( Html::span ).of( "exists!" )
            .optional( Html::span ).of( null ) ) );
  }

  /**
   * Conditional elements can be defined in the call chain
   */
  @Test
  void conditional() {
    assertEquals( """
        <html>
            <span>hello</span>
        </html>""",
        format( new Html()
            .conditional( h -> h.span( "hello" ) ).on( true )
            .conditional( h -> h.span( "world!" ) ).on( false ) ) );
  }

  /**
   * Repeated elements can be defined in the call chain
   */
  @Test
  void repeated() {
    assertEquals( """
        <html>
            <span>a</span><span>b</span>
            <p>c</p>
            <p>d</p>
            ef
        </html>""",
        format( new Html()
            .repeat( Html::span ).over( Arrays.asList( "a", "b" ) )
            .repeat( Html::p ).over( "c", "d" )
            .repeat( Html::txt ).over( Stream.of( "e", "f" ) ) ) );
  }

  private static String format( Html html ) {
    try {
      Source xmlInput = new StreamSource( new StringReader( html.toString() ) );
      StringWriter stringWriter = new StringWriter();
      StreamResult xmlOutput = new StreamResult( stringWriter );
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      transformerFactory.setAttribute( "indent-number", 2 );
      transformerFactory.setAttribute( XMLConstants.ACCESS_EXTERNAL_DTD, "" );
      transformerFactory.setAttribute( XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "" );
      Transformer transformer = transformerFactory.newTransformer();
      transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
      transformer.setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, "yes" );
      transformer.transform( xmlInput, xmlOutput );
      try( Writer w = xmlOutput.getWriter() ) {
        return w.toString().trim();
      }
    }
    catch( TransformerException | IOException e ) {
      throw new IllegalStateException( e );
    }
  }

}
