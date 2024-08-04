package dev.flowty.bowlby.app.html;

import static java.util.stream.Collectors.joining;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A convenience layer on top of {@link AbstractXml} that supports the HTML
 * elements used in this application. Yes, I know that HTML is not a subset of
 * XML, but it's close enough for our purposes.
 */
public class Html extends AbstractXml<Html> {

  /**
   * The set of elements that do not need a closing tag and that do not support
   * content
   */
  private static final Set<String> VOID_ELEMENTS = Set.of(
      "area", "base", "br", "col", "embed", "hr", "img", "input",
      "link", "meta", "param", "source", "track", "wbr" );
  private final boolean isVoid;

  private Html( String name ) {
    super( name );
    isVoid = VOID_ELEMENTS.contains( name.toLowerCase() );
  }

  /**
   * Builds an html document
   */
  public Html() {
    this( "html" );
  }

  @Override
  protected Html child( String name ) {
    return new Html( name );
  }

  /**
   * Creates a <code>body</code> element
   *
   * @param children child elements
   * @return <code>this</code>
   */
  @SafeVarargs
  public final Html body( Consumer<Html>... children ) {
    return elm( "body", children );
  }

  /**
   * Creates a <code>head</code> element
   *
   * @param children child elements
   * @return <code>this</code>
   */
  @SafeVarargs
  public final Html head( Consumer<Html>... children ) {
    return elm( "head", children );
  }

  /**
   * Creates a <code>title</code> element
   *
   * @param text text content
   * @return <code>this</code>
   */
  public Html title( String text ) {
    return elm( "title", text );
  }

  /**
   * Creates a <code>h1</code> element
   *
   * @param text text content
   * @return <code>this</code>
   */
  public Html h1( String text ) {
    return elm( "h1", text );
  }

  /**
   * Creates a <code>h1</code> element
   *
   * @param children child elements
   * @return <code>this</code>
   */
  @SafeVarargs
  public final Html h1( Consumer<Html>... children ) {
    return elm( "h1", children );
  }

  /**
   * Creates a <code>span</code> element
   *
   * @param content text content
   * @return <code>this</code>
   */
  public Html span( Object... content ) {
    return elm( "span", text( content ) );
  }

  /**
   * Creates a <code>p</code> element
   *
   * @param content text content
   * @return <code>this</code>
   */
  public Html p( Object... content ) {
    return elm( "p", text( content ) );
  }

  /**
   * Creates a <code>br</code> element
   *
   * @return <code>this</code>
   */
  public Html br() {
    return elm( "br" );
  }

  /**
   * Creates a <code>details</code> element
   *
   * @param children child elements
   * @return <code>this</code>
   */
  @SafeVarargs
  public final Html details( Consumer<Html>... children ) {
    return elm( "details", children );
  }

  /**
   * Creates a <code>summary</code> element
   *
   * @param text text content
   * @return <code>this</code>
   */
  public final Html summary( String text ) {
    return elm( "summary", text );
  }

  /**
   * Creates a <code>summary</code> element
   *
   * @param children child elements
   * @return <code>this</code>
   */
  @SafeVarargs
  public final Html summary( Consumer<Html>... children ) {
    return elm( "summary", children );
  }

  /**
   * Creates a <code>ul</code> element
   *
   * @param children child elements
   * @return <code>this</code>
   */
  @SafeVarargs
  public final Html ul( Consumer<Html>... children ) {
    return elm( "ul", children );
  }

  /**
   * Creates a <code>li</code> element
   *
   * @param children child elements
   * @return <code>this</code>
   */
  @SafeVarargs
  public final Html li( Consumer<Html>... children ) {
    return elm( "li", children );
  }

  /**
   * Creates a <code>form</code> element
   *
   * @param children child elements
   * @return <code>this</code>
   */
  @SafeVarargs
  public final Html form( Consumer<Html>... children ) {
    return elm( "form", children );
  }

  /**
   * Creates an <code>input</code> element
   *
   * @param children child elements
   * @return <code>this</code>
   */
  @SafeVarargs
  public final Html input( Consumer<Html>... children ) {
    return elm( "input", children );
  }

  /**
   * Creates a <code>a</code> element
   *
   * @param href link destination
   * @param text link text
   * @return <code>this</code>
   */
  public Html a( String href, String text ) {
    return elm( "a", a -> a
        .atr( "href", href )
        .txt( text ) );
  }

  private static String text( Object... text ) {
    return Stream.of( text )
        .map( String::valueOf )
        .collect( joining() );
  }

  @Override
  public Html cnt( String content ) {
    if( isVoid ) {
      throw new IllegalArgumentException(
          "Content is not valid in void context " + element );
    }
    return super.cnt( content );
  }

  @Override
  public Html elm( String name, String content ) {
    if( isVoid ) {
      throw new IllegalArgumentException(
          "Elements are not valid in void context " + element );
    }
    return super.elm( name, content );
  }

  @Override
  public String toString() {
    /*
     * this is one of the crucial differences between XML and HTML: HTML has a set
     * of void elements that do not require a closing tag, but all other elements
     * _must_ have a closing tag. XML allows _all_ elements to be self-closed if
     * they have no content. Hence if this is one of those void elements then we can
     * use the XML behaviour from the superclass...
     */
    if( isVoid ) {
      return super.toString();
    }
    /*
     * ... but otherwise we have to include the start and end tags,even if there is
     * no content!
     */
    return startAndEnd();
    /*
     * Also note that XML's self-closing tag (e.g.: <br/>) is not really valid HTML,
     * but parsers will ignore that final slash. So including it doesn't hurt and it
     * takes the HTML doc closer to being valid XML, which feels kind of nice I
     * suppose
     */
  }
}
