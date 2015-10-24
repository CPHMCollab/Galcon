package human;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

public class EButton extends JButton {
   
   public EButton(String text) {
      super(text);
   }
   
   public EButton(String text, Runnable action) {
      super(text);
      this.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            action.run();
         }
      });
   }
   
   public void addAction(Runnable action) {
      this.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            action.run();
         }
      });
   }
}
