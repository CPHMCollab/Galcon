package human;

import galaxy.Action;
import galaxy.Planet;
import human.MeatSackAI.FutureAction;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JPanel;

import ais.PlayerUtils;

public class MeatSackDisplay extends JPanel {
   private JFrame myframe;
   private MeatSackAI player;
   
   private BufferedImage base;
   private Planet selectedPlanet;
   private Planet destinationPlanet;
   private double cursorX;
   private double cursorY;
   private State interfaceState = State.NONE;
   private EButton sendUnitsButton;
   
   private static final double SCALE = 0.4;
   private static final double PLANET_SCALE = 1;
   private static final double SELECTION_AREA_SCALE = 3;
   
   private static enum State {
      NONE,
      SEEKING,
      FOUND;
   }

   public MeatSackDisplay(MeatSackAI ai) {
      this.player = ai;
      
      MeatSackDisplay display = this;
      
      myframe = new JFrame() {{
         this.setSize((int) (1280 * SCALE), (int) (800 * SCALE) + 32);
         JPanel content = new JPanel();
         content.setLayout(new BorderLayout());
         content.add(display, BorderLayout.CENTER);
         content.add(getControlPanel(), BorderLayout.SOUTH);
         this.setContentPane(content);
         this.setVisible(true);
         this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      }};
      
      Timer timer = new Timer();
      timer.scheduleAtFixedRate(new TimerTask() {
         public void run() {
            myframe.repaint();
         }
      }, 33, 33);
      
      addListeners();
      
      updateBase();
   }
   
   public JPanel getControlPanel() {
      JPanel controls = new JPanel();
      controls.setLayout(new FlowLayout(FlowLayout.LEFT));
      
      JFormattedTextField unitCount = new JFormattedTextField(NumberFormat.getIntegerInstance());
      unitCount.setPreferredSize(new Dimension(100,25));
      controls.add(unitCount);
      
      sendUnitsButton = new EButton("Send", () -> {
         player.addAction(new FutureAction(selectedPlanet, destinationPlanet, 
               Integer.parseInt(unitCount.getText().replaceAll("[^0-9]", ""))));
      });
      sendUnitsButton.setEnabled(false);
      controls.add(sendUnitsButton);
      controls.setPreferredSize(new Dimension(Integer.MAX_VALUE, 32));
      controls.add(new EButton("Finish Turn", () -> player.finishTurns(1)));
      
      final EButton autoAdvanceButton = new EButton(player.getAutoAdvance() ? "Turn Auto Advance Off" : "Turn Auto Advance On");
      autoAdvanceButton.addAction(() -> {
         player.setAutoAdvance(!player.getAutoAdvance());
         autoAdvanceButton.setText(player.getAutoAdvance() ? "Turn Auto Advance Off" : "Turn Auto Advance On");
      });
      controls.add(autoAdvanceButton);
      
      controls.add(new EButton("Finish 20 turns", () -> {
         player.finishTurns(20);
      }));
      
      return controls;
   }
   
   public void addListeners() {
      this.addMouseListener(new MouseAdapter() {
         
         @Override
         public void mousePressed(MouseEvent e) {
            setCursor(e);
            selectedPlanet = getPlanetAtScreenLocation(cursorX, cursorY);
            if (selectedPlanet == null) {
               setState(State.NONE);
            } else {
               setState(State.SEEKING);
            }
         }
         
         @Override
         public void mouseReleased(MouseEvent e) {
            setCursor(e);
            if (interfaceState == State.SEEKING) {
               destinationPlanet = getPlanetAtScreenLocation(cursorX, cursorY);
               if (destinationPlanet != null && destinationPlanet != selectedPlanet) {
                  setState(State.FOUND);
               } else {
                  selectedPlanet = destinationPlanet;
                  setState(State.NONE);
               }
            } else {
               setState(State.NONE);
            }
         }
      });
      
      this.addMouseMotionListener(new MouseAdapter() {
         @Override
         public void mouseMoved(MouseEvent e) {
            setCursor(e);
            if (interfaceState == State.SEEKING) {
               destinationPlanet = getPlanetAtScreenLocation(cursorX, cursorY);
            } else if (interfaceState == State.NONE) {
               selectedPlanet = getPlanetAtScreenLocation(cursorX, cursorY);
            }
         }
         
         @Override
         public void mouseDragged(MouseEvent e) {
            setCursor(e);
            if (interfaceState == State.SEEKING) {
               destinationPlanet = getPlanetAtScreenLocation(cursorX, cursorY);
            } else if (interfaceState == State.NONE) {
               selectedPlanet = getPlanetAtScreenLocation(cursorX, cursorY);
            }
         }
      });
      
      this.addComponentListener(new ComponentAdapter() {
         @Override
         public void componentResized(ComponentEvent e) {
            updateBase();
         }
      });
   }
   
   private void setCursor(MouseEvent e) {
      cursorX = e.getX();
      cursorY = e.getY();
   }

   @Override
   public void paint(Graphics g) {
      g.drawImage(getFrame(), 0, 0, null);
   }
   
   public BufferedImage getFrame() {
      BufferedImage frame = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_RGB);
      Graphics2D g = frame.createGraphics();
      g.drawImage(base, 0, 0, null);
      if (interfaceState == State.NONE) {
         if (selectedPlanet != null) {
            circle(selectedPlanet, g);
         }
      }
      if (interfaceState == State.SEEKING) {
         circle(selectedPlanet, g);
         
         drawPointer(selectedPlanet, cursorX, cursorY, g);
         if (destinationPlanet != null) {
            circle(destinationPlanet, g);
         }
      } else if (interfaceState == State.FOUND) {
         circle(selectedPlanet, g);
         circle(destinationPlanet, g);
         
         drawPointer(selectedPlanet, destinationPlanet.getCoords()[0] * SCALE, destinationPlanet.getCoords()[1] * SCALE, g);
      }
      
      return frame;
   }
   
   private void circle(Planet p, Graphics2D g) {
      g.setStroke(new BasicStroke(3.0f));
      g.setColor(Color.WHITE);
      g.drawOval(
            (int) (p.getCoords()[0] * SCALE - p.RADIUS * PLANET_SCALE * SCALE - 5), 
            (int) (p.getCoords()[1] * SCALE - p.RADIUS * PLANET_SCALE * SCALE - 5), 
            (int) (p.RADIUS * PLANET_SCALE * 2 * SCALE + 10), 
            (int) (p.RADIUS * PLANET_SCALE * 2 * SCALE + 10));
   }
   
   private void drawPointer(Planet from, double xTo, double yTo, Graphics2D g) {
      g.setStroke(new BasicStroke(3.0f));
      g.setColor(Color.WHITE);
      double x = from.getCoords()[0] * SCALE;
      double y = from.getCoords()[1] * SCALE;
      double diffx = x - xTo;
      double diffy = y - yTo;
      double factor = Math.sqrt(diffx * diffx + diffy * diffy);
      double fromx1 = x + diffy * (5 + from.RADIUS * PLANET_SCALE * SCALE) / factor;
      double fromx2 = x - diffy * (5 + from.RADIUS * PLANET_SCALE * SCALE) / factor;
      double fromy1 = y - diffx * (5 + from.RADIUS * PLANET_SCALE * SCALE) / factor;
      double fromy2 = y + diffx * (5 + from.RADIUS * PLANET_SCALE * SCALE) / factor;
      g.drawLine((int) (fromx1), (int) (fromy1), (int) xTo, (int) yTo);
      g.drawLine((int) (fromx2), (int) (fromy2), (int) xTo, (int) yTo);
   }
   
   public void updateBase() {
      BufferedImage newImage = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_RGB);
      drawBackground(newImage.getGraphics());
      drawPlanets(newImage.getGraphics());
      base = newImage;
   }

   private void drawBackground(Graphics g) {
      g.setColor(Color.BLACK);
      g.fillRect(0, 0, this.getWidth(), this.getHeight());
   }

   private void drawPlanets(Graphics g) {
      for (Planet p : player.getPlanets()) {
         g.setColor(p.getColor());
         g.fillOval(
               (int) ((p.getCoords()[0] - p.RADIUS * PLANET_SCALE) * SCALE), 
               (int) ((p.getCoords()[1] -  p.RADIUS * PLANET_SCALE) * SCALE), 
               (int) (p.RADIUS * PLANET_SCALE * 2 * SCALE), 
               (int) (p.RADIUS * PLANET_SCALE * 2 * SCALE));
      }
   }
   
   private Planet getPlanetAtScreenLocation(double x, double y) {
      x = x / SCALE;
      y = y / SCALE;
      
      double best = SELECTION_AREA_SCALE + 1;
      Planet rtn = null;
      for (Planet p : player.getPlanets()) {
         double planetX = p.getCoords()[0];
         double planetY = p.getCoords()[1];
         double distance = Math.sqrt((x - planetX) * (x - planetX) + (y - planetY) * (y - planetY));
         
         if (distance < p.RADIUS * PLANET_SCALE * SELECTION_AREA_SCALE) {
            double ratio = distance / (p.RADIUS * PLANET_SCALE);
            if (ratio < best) {
               best = ratio;
               rtn = p;
            }
         }
      }
      return rtn;
   }
   
   private void setState(State state) {
      this.interfaceState = state;
      sendUnitsButton.setEnabled(state == State.FOUND);
   }
   
   public void newGame() {
      setState(State.NONE);
      selectedPlanet = null;
      destinationPlanet = null;
   }
}
