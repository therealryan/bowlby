package dev.flowty.bowlby.app.xml;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A convenient superclass for building XML strings
 *
 * @param <S> self type
 */
abstract class AbstractXml<S extends AbstractXml<S>> {

  /**
   * The element name
   */
  protected final String element;
  /**
   * The set of attributes on the element name
   */
  protected final Map<String, String> attributes = new TreeMap<>();
  /**
   * The contents of the element
   */
  protected final List<String> contents = new ArrayList<>();

  /**
   * @param element the root element name
   */
  protected AbstractXml( String element ) {
    this.element = element;
  }

  /**
   * @param name The child element name
   * @return a child element
   */
  protected abstract S child( String name );

  /**
   * @return <code>this</code>
   */
  @SuppressWarnings("unchecked")
  protected S self() {
    return (S) this;
  }

  /**
   * Adds an optional element to the document
   *
   * @param <T> The value type from which the element will be created
   * @param elm The element builder
   * @return The means to supply the value to build from
   */
  public <T> Option<S, T> optional( BiConsumer<S, T> elm ) {
    return new Option<>( self(), elm );
  }

  /**
   * @param <S> Document type
   * @param <T> Value type
   */
  public static class Option<S, T> {
    private S self;
    private final BiConsumer<S, T> elm;

    private Option( S self, BiConsumer<S, T> elm ) {
      this.self = self;
      this.elm = elm;
    }

    /**
     * Supplies the value to build the optional element from. if the value is null
     * then we skip the element.
     *
     * @param value The source value
     * @return The document
     */
    public S of( T value ) {
      return on( Optional.ofNullable( value ) );
    }

    /**
     * Supplies the value to build the optional element from. if the value is
     * missing then we skip the element.
     *
     * @param value The source value
     * @return The document
     */
    public S on( Optional<T> value ) {
      value.ifPresent( v -> elm.accept( self, v ) );
      return self;
    }
  }

  /**
   * Adds a conditional element to the document
   *
   * @param elm The element builder
   * @return The means to supply the value to build from
   */
  public Condition<S> conditional( Consumer<S> elm ) {
    return new Condition<>( self(), elm );
  }

  /**
   * @param <S> Document type
   */
  public static class Condition<S> {
    private S self;
    private final Consumer<S> elm;

    private Condition( S self, Consumer<S> elm ) {
      this.self = self;
      this.elm = elm;
    }

    /**
     * Determines if the conditional element is added or not
     *
     * @param b <code>true</code> to add the element
     * @return The document
     */
    public S on( boolean b ) {
      if( b ) {
        elm.accept( self );
      }
      return self;
    }
  }

  /**
   * Adds repeated elements to the document
   *
   * @param <T> The value type from which the elements will be created
   * @param elm The element builder
   * @return The means to supply the values to build from
   */
  public <T> Repeat<S, T> repeat( BiConsumer<S, T> elm ) {
    return new Repeat<>( self(), elm );
  }

  /**
   * @param <S> Document type
   * @param <T> Value type
   */
  public static class Repeat<S, T> {
    private S self;
    private final BiConsumer<S, T> elm;

    private Repeat( S self, BiConsumer<S, T> elm ) {
      this.self = self;
      this.elm = elm;
    }

    /**
     * Supplies the values to build the repeated elements from.
     *
     * @param values The source values
     * @return The document
     */
    public S over( Collection<T> values ) {

      return over( values.stream() );
    }

    /**
     * Supplies the values to build the repeated elements from.
     *
     * @param values The source values
     * @return The document
     */
    @SafeVarargs
    public final S over( T... values ) {
      return over( Stream.of( values ) );
    }

    /**
     * Supplies the values to build the repeated elements from.
     *
     * @param values The source values
     * @return The document
     */
    public S over( Stream<T> values ) {
      values.forEach( value -> elm.accept( self, value ) );
      return self;
    }
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
   * @param content The content, in which XML content will be escaped
   * @return <code>this</code>
   */
  public S txt( String content ) {
    return cnt( escape( content ) );
  }

  private static String escape( String str ) {
    return str.chars().mapToObj(
        c -> c > 127 || "\"'<>&".indexOf( c ) != -1
            ? "&#" + c + ";"
            : String.valueOf( (char) c ) )
        .collect( Collectors.joining() );
  }

  /**
   * Adds arbitrary content to the element
   *
   * @param content The content, that can contain XML markup
   * @return <code>this</code>
   */
  public S cnt( String content ) {
    contents.add( content );
    return self();
  }

  @Override
  public String toString() {
    if( contents.isEmpty() ) {
      return selfClosed();
    }
    return startAndEnd();
  }

  /**
   * @return A self-closed tag, e.g.: <code>&lt;name/&gt;</code>
   */
  protected String selfClosed() {
    return String.format( "<%s%s/>",
        element,
        attributes.entrySet().stream()
            .map( e -> " " + e.getKey() + "=\"" + e.getValue() + "\"" )
            .collect( joining() ) );
  }

  /**
   * @return A start/end tag pair, e.g.:
   *         <code>&lt;name&gt;contents&lt;/name&gt;</code>
   */
  protected String startAndEnd() {
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
