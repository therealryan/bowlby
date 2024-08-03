package dev.flowty.bowlby.app.html;

import java.util.function.Consumer;

/**
 * A convenience layer on top of {@link AbstractXml} that supports the HTML
 * elements used in this application. Yes, I know that HTML is not a subset of
 * XML, but it's close enough for our purposes.
 */
public class Html extends AbstractXml<Html> {

  private Html( String name ) {
    super( name );
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
   * @param text text content
   * @return <code>this</code>
   */
  public Html span( String text ) {
    return elm( "span", text );
  }

  /**
   * Creates a <code>p</code> element
   *
   * @param text text content
   * @return <code>this</code>
   */
  public Html p( String text ) {
    return elm( "p", text );
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
}
