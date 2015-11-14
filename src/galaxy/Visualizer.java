package galaxy;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import ais.PlayerUtils;

@SuppressWarnings("serial")
public abstract class Visualizer extends JPanel implements KeyListener, MouseListener {
   private BufferedImage bufferImage;
   private Planet[] planets = Planet.getAllPlanets();
   private Player[] players;
   protected final int WIN_WIDTH, WIN_HEIGHT;
   private JFrame frame;

   protected MouseOverInfo mouseOverInfo;

   protected interface MouseOverInfo {
      void draw(Graphics g);
   }

   protected final void debug(String str) {
      if (Main.debugMode) {
         System.out.println(str);
      }
   }

   protected Visualizer(int winWidth, int winHeight, int dimensions) {
      if (dimensions != Main.DIMENSIONS.length) {
         throw new DimensionMismatchException("Visualizer does not match galaxy dimension space.");
      }

      WIN_WIDTH = winWidth;
      WIN_HEIGHT = winHeight;

      final Visualizer face = this;
      frame = new JFrame() {{
         setName("Galcon AI Challenge");
         setSize(WIN_WIDTH, WIN_HEIGHT);
         setContentPane(face);
         setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         setBackground(Color.GRAY);
         setResizable(true);
         setVisible(true);
         addKeyListener(face);
         addMouseListener(face);
      }};
   }

   final void nextGame(Player[] active) {
      planets = Planet.getAllPlanets();
      players = active;
      newGame();
   }

   protected abstract void newGame();

   final void update() {
      BufferedImage image = new BufferedImage(WIN_WIDTH, WIN_HEIGHT, BufferedImage.TYPE_INT_ARGB);
      Graphics g = image.getGraphics();

      // Switch to being drawAllUnits(planets, fleets)
      drawBackground(g);
      drawPlanets(planets, g);
      drawFleets(Fleet.getAllFleets(), g);
      drawOther(g);
      drawPlayerInfo(players, g);
      if (mouseOverInfo != null) {
         mouseOverInfo.draw(g);
      }

      bufferImage = image;
      frame.repaint();
   }

   @Override
   public final void paint(Graphics g) {
      g.drawImage(bufferImage,0,0,this);
   }

   protected final boolean checkRecentlyConquered(Planet p) {
      return p.checkRecentlyConquered();
   }

   protected final int numUnitsOwnedBy(Player p) {
      return Galaxy.numUnitsOwnedBy(p);
   }

   protected final int totalProduction(Player p) {
	   double production = 0;
	   List<Planet> playerPlanets = PlayerUtils.getPlanetsOwnedByPlayer(planets, p);
	   for (Planet planet : playerPlanets)
		   production += planet.getProductionFrequency();
	   
	   return (int)(production * 100);
   }

   protected abstract void drawBackground(Graphics g);

   protected abstract void drawPlanets(Planet[] planets, Graphics g);

   protected abstract void drawFleets(Fleet[] fleets, Graphics g);

   protected abstract void drawPlayerInfo(Player[] players, Graphics g);

   protected abstract void drawOther(Graphics g);

   protected void keystroke(KeyEvent e){}

   @Override
   public final void keyPressed(KeyEvent e) {
      char command = (char)e.getKeyCode();
      if(command == 'Q') {
         System.exit(0);
      } else if (command == 'S') {
         Main.skipGame();
      } else if (command == ' ') {
         Main.togglePause();
      }

      keystroke(e);
   }

   protected void mousePress(MouseEvent e, Planet[] planets) {}

   @Override
   public final void mousePressed(MouseEvent e){
      mousePress(e, planets);
   }

   @Override
   public void keyReleased(KeyEvent arg0) {}
   @Override
   public void keyTyped(KeyEvent arg0) {}
   @Override
   public void mouseClicked(MouseEvent e){}
   @Override
   public void mouseEntered(MouseEvent e){}
   @Override
   public void mouseExited(MouseEvent e){}
   @Override
   public void mouseReleased(MouseEvent e){}
}
