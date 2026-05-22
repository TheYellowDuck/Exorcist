package main;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;


 /**
 * The class Main
 */ 
public class Main {


/** 
 *
 * Main
 *
 * @param args  the args. 
 */
	public static void main(String[] args) { 

		
		JFrame window = new JFrame();
		window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		window.setResizable(false);
		window.setTitle("Exorcist");

		GamePanel gamePanel = new GamePanel();
		window.add(gamePanel);

		try {
			java.awt.image.BufferedImage icon = ImageIO.read(Main.class.getResourceAsStream("/icon.png"));
			window.setIconImage(icon);
			try {
				java.awt.Taskbar.getTaskbar().setIconImage(icon);
			} catch (Exception ignored) {}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		window.pack();

		window.setLocationRelativeTo(null);
		window.setVisible(true);

		window.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent e) {
				gamePanel.saveHighScore();
				System.exit(0);
			}
		});

		gamePanel.startGameThread();

	}
	

/** 
 *
 * Resize
 *
 * @param img  the img. 
 * @param newW  the new W. 
 * @param newH  the new H. 
 * @return BufferedImage
 */
	public static BufferedImage resize(BufferedImage img, int newW, int newH) {  

        Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = dimg.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        return dimg;
    }

}
