package dev.flowty.bowlby.app.ui;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.flowty.bowlby.app.Main;

/**
 * Drops an icon into the systray for local users
 */
public class Gui {
  private static final Logger LOG = LoggerFactory.getLogger( Gui.class );

  private TrayIcon icon;
  private Image quiescent;
  private Image provoked;

  private final Main app;

  /**
   * Determines system tray icon behaviour
   */
  public enum IconBehaviour {
    /**
     * No icon
     */
    NONE,
    /**
     * A static icon
     */
    STATIC,
    /**
     * An icon that indicates request handling behaviour
     */
    DYNAMIC;

    /**
     * @param s A string
     * @return the matching behaviour
     */
    public static IconBehaviour from( String s ) {
      for( IconBehaviour ics : values() ) {
        if( s.equalsIgnoreCase( ics.name() ) ) {
          return ics;
        }
      }
      return DYNAMIC;
    }
  }

  /**
   * @param app           The application instance
   * @param iconBehaviour Controls icon behaviour
   */
  public Gui( Main app, IconBehaviour iconBehaviour ) {
    this.app = app;
    if( iconBehaviour != IconBehaviour.NONE && SystemTray.isSupported() ) {
      try {
        quiescent = ImageIO.read( Gui.class.getResource( "/bowlby.png" ) );
      }
      catch( IOException e ) {
        throw new IllegalStateException( "Failed to load icon", e );
      }
      provoked = provoked( quiescent );
      PopupMenu menu = new PopupMenu();
      menu.add( openItem() );
      menu.addSeparator();
      menu.add( quitItem() );

      icon = new TrayIcon( provoked, "bowlby", menu );
      icon.setImageAutoSize( true );

      quiescent = quiescent.getScaledInstance(
          icon.getImage().getWidth( null ),
          icon.getImage().getHeight( null ),
          Image.SCALE_SMOOTH );
      provoked = provoked.getScaledInstance(
          quiescent.getWidth( null ),
          quiescent.getHeight( null ),
          Image.SCALE_SMOOTH );

      icon.setImage( quiescent );

      if( iconBehaviour == IconBehaviour.DYNAMIC ) {
        app.withListener( active -> icon.setImage( active ? provoked : quiescent ) );
      }
    }
  }

  private static Image provoked( Image quiescent ) {
    BufferedImage bi = new BufferedImage( quiescent.getWidth( null ), quiescent.getHeight( null ),
        BufferedImage.TYPE_INT_RGB );
    Graphics2D g = bi.createGraphics();
    g.drawImage( quiescent, 0, 0, null );
    g.setColor( Color.white );
    g.fillOval( 30, 80, 50, 50 );
    g.fillOval( 150, 80, 50, 50 );
    g.setColor( Color.black );
    g.fillOval( 48, 98, 14, 14 );
    g.fillOval( 168, 98, 14, 14 );

    return bi;
  }

  private MenuItem openItem() {
    MenuItem item = new MenuItem( "Open" );
    item.addActionListener( e -> {
      if( Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported( Action.BROWSE ) ) {
        try {
          Desktop.getDesktop().browse( app.uri() );
        }
        catch( IOException ioe ) {
          LOG.warn( "Failed to browse {}", app.uri(), ioe );
        }
      }
      else {
        try {
          new ProcessBuilder( "xdg-open", app.uri().toString() ).start();
        }
        catch( IOException ioe ) {
          LOG.warn( "Failed to invoke `xdg-open {}`", app.uri(), ioe );
        }
      }
    } );
    return item;
  }

  private MenuItem quitItem() {
    MenuItem item = new MenuItem( "Quit" );
    item.addActionListener( e -> app.stop() );
    return item;
  }

  /**
   * Adds the icon the system tray
   */
  public void start() {
    if( icon != null && SystemTray.isSupported() ) {
      SwingUtilities.invokeLater( () -> {
        try {
          SystemTray.getSystemTray().add( icon );
        }
        catch( AWTException e ) {
          LOG.warn( "Failed to add tray icon", e );
        }
      } );
    }
  }

  /**
   * Removes the icon from the system tray
   */
  public void stop() {
    if( icon != null && SystemTray.isSupported() ) {
      SwingUtilities.invokeLater( () -> {
        SystemTray.getSystemTray().remove( icon );
      } );
    }
  }

}
