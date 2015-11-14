package visualizers.threedimensiondefault;

import galaxy.Fleet;
import galaxy.Planet;
import galaxy.Player;
import galaxy.Visualizer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Jono
 */
@SuppressWarnings("serial")
public class Display extends Visualizer {

   public Display(int[] dimensions) {
      super(1600, 900, 3);
      mouseOverInfo = mouseOver;
      this.addMouseMotionListener(ma);
      this.addMouseListener(ma);
      this.addMouseWheelListener(ma);
      this.addKeyListener(this);
      this.setFocusable(true);
      this.setBackground(Color.black);
      displayCamera = new Camera(new Vector(-1700, 0, 475), () -> {
         if (autoRotate) {
            autoRotatePosition += AUTO_ROTATE_SPEED;
            displayCamera.location = new Vector(
                  -1700 * Math.cos(autoRotatePosition), 
                  -1700 * Math.sin(autoRotatePosition),
                  475);
            displayCamera.vRot = -Math.PI / 10;
            displayCamera.hRot = -autoRotatePosition;
         } else {
            displayCamera.moveCamera(Vector.scale(cameraSpeed, .005));
         }
      });
      displayCamera.vRot = -Math.PI / 16;
      GraphicHolder.DIMESIONS = dimensions;
   }

   public Camera displayCamera;
   public double scrollSpeed = 10000;
   private Vector cameraSpeed = new Vector(0, 0, 0);
   private int mousex, mousey;
   private boolean mouseDown = false;
   private boolean autoRotate = true;
   private double autoRotatePosition = 0.0;
   private static final double AUTO_ROTATE_SPEED = 0.001;
   
   MouseOver mouseOver = new MouseOver();
   
   class MouseOver implements MouseOverInfo {
      private Font mouseOverFont = new Font("Monospaced", Font.PLAIN, 12);
      String text;
      int coords[];
      int timeToLive; //in frames

      @Override
      public void draw(Graphics g) {
         if(timeToLive > 0) {
            timeToLive--;
            g.setFont(mouseOverFont);
            g.setColor(Color.WHITE);
            g.drawString(text, coords[0] - 1, coords[1]);
            g.drawString(text, coords[0], coords[1] - 1);
            g.setColor(Color.LIGHT_GRAY);
            g.drawString(text, coords[0], coords[1]);
         }
      }
   }
   
   private MouseAdapter ma = new MouseAdapter() {
      @Override
      public void mouseDragged(MouseEvent me) {
         if (!autoRotate) {
            if (mouseDown) {
               displayCamera.hRot += (me.getX() - mousex) / (displayCamera.zoom / 3.0);
               displayCamera.vRot += (me.getY() - mousey) / (displayCamera.zoom / 3.0);
               if (displayCamera.vRot > Math.PI / 2) {
                  displayCamera.vRot = Math.PI / 2;
               }
               if (displayCamera.vRot < -Math.PI / 2) {
                  displayCamera.vRot = -Math.PI / 2;
               }
               mousex = me.getX();
               mousey = me.getY();
               repaint();
            } else {
               mousex = me.getX();
               mousey = me.getY();
               mouseDown = true;
            }
         }
      }

      @Override
      public void mouseMoved(MouseEvent me) {
         mousex = me.getX();
         mousey = me.getY();
         mouseDown = false;
      }
      
      @Override
      public void mousePressed(MouseEvent e) {
         requestFocus(true);

         //Minuses are to offset it to the tip of the mouse pointer
         int mouseCoords[] = {e.getX() - 10, e.getY() - 12};
         for(GraphicHolder gh : displayCamera.drawList) {
            double tempCoords[] = {mouseCoords[0], mouseCoords[1] - gh.screenRadius/2};
            double scrLoc[] = {gh.screenLocation.x + 780, gh.screenLocation.y + 400};
            
            //Calculate distance
            double distance = 0;
            for (int i = 0; i < 2; i++) {
               distance += Math.pow(scrLoc[i] - tempCoords[i], 2);
            }
            distance = Math.sqrt(distance);
            
            if(distance < gh.screenRadius) {
               mouseOver.coords = mouseCoords;
               mouseOver.text = "Production: " + gh.production;
               mouseOver.timeToLive = 120;
               return;
            }
         }
         //If no planet was clicked on remove any text that still exists
         mouseOver.timeToLive = 0;
      }

      @Override
      public void mouseWheelMoved(MouseWheelEvent e) {
         if (e.getWheelRotation() < 0) {
            displayCamera.zoom *= 1.05;
         } else {
            displayCamera.zoom *= .95;
         }
         if (displayCamera.zoom < 1000)
            displayCamera.zoom = 1000;
      }
   };
   
   @Override
   protected void keystroke(KeyEvent e) {
      switch (e.getKeyCode()) {
      case 37: // right
         //go right
         cameraSpeed.x = -scrollSpeed;
         break;
      case 38: // down
         //go back
         cameraSpeed.y = -scrollSpeed;
         break;
      case 39: // left
         //go left
         cameraSpeed.x = scrollSpeed;
         break;
      case 40: // up
         //go forwards
         cameraSpeed.y = scrollSpeed;
         break;
      case 16: // in (Shift)
         //go up
         cameraSpeed.z = -scrollSpeed;
         break;
      case 17: // out (Ctrl)
         //go down
         cameraSpeed.z = scrollSpeed;
         break;
      case 10: // enter
         autoRotate = !autoRotate;
         break;
      }
   }
   
   @Override
   public void keyReleased(KeyEvent e) {
      switch (e.getKeyCode()) {
      case 37:
         cameraSpeed.x = 0;
         break;
      case 38:
         cameraSpeed.y = 0;
         break;
      case 39:
         cameraSpeed.x = 0;
         break;
      case 40:
         cameraSpeed.y = 0;
         break;
      case 16:
         cameraSpeed.z = 0;
         break;
      case 17:
         cameraSpeed.z = 0;
         break;
      }
   }

   private List<Planet> planets;
   private List<Fleet> fleets;

   @Override
   protected void newGame() {
      // TODO Auto-generated method stub

   }

   @Override
   protected void drawPlanets(Planet[] p, Graphics g) {
      planets = new LinkedList<Planet>();
      for (Planet pl : p) {
         planets.add(pl);
      }
   }

   @Override
   protected void drawFleets(Fleet[] f, Graphics g) {
      fleets = new LinkedList<Fleet>();
      for (Fleet fl : f) {
         fleets.add(fl);
      }
   }

   @Override
   protected void drawBackground(Graphics g) {
      //      planets = new ArrayList<>();
      //      fleets = new ArrayList<>();
   }

   @Override
   protected void drawPlayerInfo(Player[] players, Graphics g) {
      final int FONT_HEIGHT = 40;
      Font font = new Font("Monospaced", Font.PLAIN, FONT_HEIGHT);
      g.setFont (font);
      
      int offset = 1;
      for (Player p : players) {
         g.setColor(Color.DARK_GRAY);
         g.drawString(p.NAME + ": " + numUnitsOwnedBy(p) + " " + totalProduction(p), 10, FONT_HEIGHT * offset - 1);
         g.drawString(p.NAME + ": " + numUnitsOwnedBy(p) + " " + totalProduction(p), 10, FONT_HEIGHT * offset + 1);
         g.drawString(p.NAME + ": " + numUnitsOwnedBy(p) + " " + totalProduction(p), 9, FONT_HEIGHT * offset);
         g.drawString(p.NAME + ": " + numUnitsOwnedBy(p) + " " + totalProduction(p), 11, FONT_HEIGHT * offset);
         g.setColor(p.COLOR);
         g.drawString(p.NAME + ": " + numUnitsOwnedBy(p) + " " + totalProduction(p), 10, FONT_HEIGHT * offset++);
      }
   }

   @Override
   protected void drawOther(Graphics g) {
      g.setColor(Color.BLACK);
      g.fillRect(0, 0, getWidth(), getHeight());
      g.drawLine(0, 0, 10, 10);
      displayCamera.draw(planets, fleets, g, getWidth() / 2, getHeight() / 2);
   }
}
