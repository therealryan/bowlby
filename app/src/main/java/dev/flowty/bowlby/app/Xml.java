package dev.flowty.bowlby.app;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * A convenient way to build XML strings
 *
 * @param <S> self type
 */
class Xml<S extends Xml<S>> {

  private final String element;
  private final Map<String, String> attributes = new TreeMap<>();
  private final List<String> contents = new ArrayList<>();

  /**
   * @param element the root element name
   */
  public Xml( String element ) {
    this.element = element;
  }

  /**
   * @param name The child element name
   * @return a child element
   */
  @SuppressWarnings("unchecked")
  protected S child( String name ) {
    return (S) new Xml<S>( name );
  }

  /**
   * @return <code>this</code>
   */
  @SuppressWarnings("unchecked")
  protected S self() {
    return (S) this;
  }

  /**
   * Adds a new element
   *
   * @param name         The element name
   * @param childContent The contents of the element
   * @return <code>this</code>
   */
  @SafeVarargs
  public final S elm( String name, Consumer<S>... childContent ) {
    S child = child( name );
    for( Consumer<S> consumer : childContent ) {
      consumer.accept( child );
    }
    contents.add( child.toString() );
    return self();
  }

  /**
   * Adds a new element
   *
   * @param name    The element name
   * @param content The element text content
   * @return <code>this</code>
   */
  public S elm( String name, String content ) {
    return elm( name, c -> c.txt( content ) );
  }

  /**
   * Adds an element attribute
   *
   * @param name  the attribute name
   * @param value The attribute value
   * @return <code>this</code>
   */
  public S atr( String name, String value ) {
    attributes.put( name, value );
    return self();
  }

  /**
   * Adds text content to the element
   *
   * @param content The content
   * @return <code>this</code>
   */
  public S txt( String content ) {
    contents.add( content );
    return self();
  }

  @Override
  public String toString() {
    if( contents.isEmpty() ) {
      return String.format( "<%s%s/>",
          element,
          attributes.entrySet().stream()
              .map( e -> " " + e.getKey() + "=\"" + e.getValue() + "\"" )
              .collect( joining() ) );
    }
    return String.format( "<%s%s>%s</%s>",
        element,
        attributes.entrySet().stream()
            .map( e -> " " + e.getKey() + "=\"" + e.getValue() + "\"" )
            .collect( joining() ),
        contents.stream()
            .collect( joining() ),
        element );
  }
}
