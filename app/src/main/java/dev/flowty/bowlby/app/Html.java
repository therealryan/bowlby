package dev.flowty.bowlby.app;

import java.util.function.Consumer;

/**
 * A convenient way to build html strings
 */
public class Html extends Xml<Html> {

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
   * Creates a body element
   *
   * @param children child elements
   * @return <code>this</code>
   */
  @SafeVarargs
  public final Html body( Consumer<Html>... children ) {
    return elm( "body", children );
  }

  /**
   * Creates a head element
   *
   * @param children child elements
   * @return <code>this</code>
   */
  @SafeVarargs
  public final Html head( Consumer<Html>... children ) {
    return elm( "head", children );
  }

  /**
   * Creates a title element
   *
   * @param text text content
   * @return <code>this</code>
   */
  public Html title( String text ) {
    return elm( "title", text );
  }

  /**
   * Creates a h1 element
   *
   * @param text text content
   * @return <code>this</code>
   */
  public Html h1( String text ) {
    return elm( "h1", text );
  }

  /**
   * Creates a ul element
   *
   * @param children child elements
   * @return <code>this</code>
   */
  @SafeVarargs
  public final Html ul( Consumer<Html>... children ) {
    return elm( "ul", children );
  }

  /**
   * Creates a li element
   *
   * @param children child elements
   * @return <code>this</code>
   */
  @SafeVarargs
  public final Html li( Consumer<Html>... children ) {
    return elm( "li", children );
  }

  /**
   * Creates a link element
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
