// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
// Required Notice: Copyright (c) 2026 George Zhang — https://github.com/TheYellowDuck

package main;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;


 /**
 * The class Key handler implements key listener
 */
public class KeyHandler implements KeyListener, MouseListener {

    public boolean upPressed, downPressed, leftPressed, rightPressed, attackPressed, shieldPressed;
    public boolean escJustPressed, enterJustPressed, hJustPressed;
    public ArrayList<Integer> recent = new ArrayList<>(4);

    // int count = 0;

    @Override

/** 
 *
 * Key typed
 *
 * @param e  the e. 
 */
    public void keyTyped(KeyEvent e) { 

    }

    @Override

/** 
 *
 * Key pressed
 *
 * @param e  the e. 
 */
    public void keyPressed(KeyEvent e) { 


        int code = e.getKeyCode();

        if (code == KeyEvent.VK_W || code == KeyEvent.VK_SPACE || code == KeyEvent.VK_UP) {
            upPressed = true;
        }
        if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) {
            downPressed = true;
        }
        if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) {
            leftPressed = true;
            if (!recent.contains(3))
                recent.add(0, 3);
        }
        if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) {
            rightPressed = true;
            if (!recent.contains(1))
                recent.add(0, 1);
        }
        if (code == KeyEvent.VK_Z || code == KeyEvent.VK_J) {
            attackPressed = true;
        }
        if (code == KeyEvent.VK_K || code == KeyEvent.VK_E) {
            shieldPressed = true;
        }
        if (code == KeyEvent.VK_ESCAPE) escJustPressed  = true;
        if (code == KeyEvent.VK_ENTER)  enterJustPressed = true;
        if (code == KeyEvent.VK_H)      hJustPressed     = true;

    }

    @Override

/** 
 *
 * Key released
 *
 * @param e  the e. 
 */
    public void keyReleased(KeyEvent e) { 

        
        int code = e.getKeyCode();

        if (code == KeyEvent.VK_W || code == KeyEvent.VK_SPACE || code == KeyEvent.VK_UP) {
            upPressed = false;
        }
        if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) {
            downPressed = false;
        }
        if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) {
            leftPressed = false;
            recent.remove((Integer)3);
        }
        if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) {
            rightPressed = false;
            recent.remove((Integer)1);
        }
        if (code == KeyEvent.VK_Z || code == KeyEvent.VK_J) {
            attackPressed = false;
        }
        if (code == KeyEvent.VK_K || code == KeyEvent.VK_E) {
            shieldPressed = false;
        }

    }

    @Override public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) attackPressed = true;
        if (e.getButton() == MouseEvent.BUTTON3) shieldPressed = true;
    }
    @Override public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) attackPressed = false;
        if (e.getButton() == MouseEvent.BUTTON3) shieldPressed = false;
    }
    @Override public void mouseClicked(MouseEvent e)  {}
    @Override public void mouseEntered(MouseEvent e)  {}
    @Override public void mouseExited(MouseEvent e)   {}

}
