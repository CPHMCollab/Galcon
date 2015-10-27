package visualizersthreedee;

import galaxy.Fleet;
import galaxy.Planet;
import galaxy.Player;
import galaxy.Visualizer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author Jono
 */
public class Display extends Visualizer {

   public Display(int[] dimensions) {
      super(1600, 900, 3);
      this.addMouseMotionListener(ma);
      this.addMouseListener(ma);
      this.addMouseWheelListener(ma);
      this.addKeyListener(ka);
      this.setFocusable(true);
      this.setBackground(Color.black);
      displayCamera = new Camera(new Vector(-1700, 0, 475));
      displayCamera.vRot = -Math.PI / 16;
      GraphicHolder.DIMESIONS = dimensions;
      
      Timer timer = new Timer();
      timer.schedule(new TimerTask() {
         @Override
         public void run() {
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
         }
      }, 0, 10);
   }

   public Camera displayCamera;
   public double scrollSpeed = 1000;
   private Vector cameraSpeed = new Vector(0, 0, 0);
   private int mousex, mousey;
   private boolean mouseDown = false;
   private boolean autoRotate = true;
   private double autoRotatePosition = 0.0;
   private static final double AUTO_ROTATE_SPEED = 0.001;

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
      public void mousePressed(MouseEvent me) {
         requestFocus(true);
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
   public KeyAdapter ka = new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
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
   };
   
   private List<Planet> planets;
   private List<Fleet> fleets;
   
   @Override
   protected void newGame() {
      // TODO Auto-generated method stub
      
   }

   @Override
   protected void drawPlanet(Planet p, Graphics g) {
      planets.add(p);
   }

   @Override
   protected void drawFleet(Fleet f, Graphics g) {
      fleets.add(f);
   }

   @Override
   protected void drawBackground(Graphics g) {
      
      planets = new ArrayList<>();
      fleets = new ArrayList<>();
   }

   @Override
   protected void drawPlayerInfo(Player[] players, Graphics g) {
      // TODO Auto-generated method stub
   }

   @Override
   protected void drawOther(Graphics g) {
      g.setColor(Color.BLACK);
      g.fillRect(0, 0, getWidth(), getHeight());
      g.drawLine(0, 0, 10, 10);
      displayCamera.draw(planets, fleets, g, getWidth() / 2, getHeight() / 2);
   }

   @Override
   protected void keystroke(KeyEvent e) {
      
   }
}
