package entity;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;


 /**
 * The class Entity
 */ 
public class Entity {
    
    public int worldX, worldY;
    public int speed;
    public int tileSize = 48;

    public BufferedImage[] idle, walk, attack, death;
    public BufferedImage[] action;
    public int direction;

    // public int action;

    public double spriteFrameTime;
    public double currTime;
    public double lastTime;
    public double delta = 0;
    public int spriteNum = 0;
    public boolean drawHitBox = false;
    
    public Rectangle solidArea;
    public boolean collisionOnX = false;
    public boolean collisionOnY = false;
    
    public int newWorldX, newWorldY;
    

/** 
 *
 * Gets the sprites
 *
 * @param img  the img. 
 * @param spriteCount  the sprite count. 
 * @param orgWidth  the org width. 
 * @param xshift  the xshift. 
 * @return the sprites
 */
    public BufferedImage[] getSprites(BufferedImage img, int spriteCount, int orgWidth, int xshift) { 


        if (img == null) return null;

        System.out.println(img.getWidth());
        
        BufferedImage[] sprites = new BufferedImage[spriteCount];

        for (int i = 0; i < spriteCount; i ++) {
            sprites[i] = resize(img.getSubimage(i * orgWidth + xshift, 0, 64, 64), (int) (1.2 * 64), (int) (1.2 * 64)).getSubimage(18, 4, 48, 48);
        }

        return sprites;

    }
    

/** 
 *
 * Gets the sprites
 *
 * @param img  the img. 
 * @param spriteCount  the sprite count. 
 * @param orgWidth  the org width. 
 * @param xshifts  the xshifts. 
 * @return the sprites
 */
    public BufferedImage[] getSprites(BufferedImage img, int spriteCount, int orgWidth, int[] xshifts) { 


        if (img == null) return null;

        System.out.println(img.getWidth());
        
        BufferedImage[] sprites = new BufferedImage[spriteCount];

        for (int i = 0; i < spriteCount; i ++) {
        	sprites[i] = resize(img.getSubimage(i * orgWidth + xshifts[i], 0, 64, 64), (int) (1.2 * 64), (int) (1.2 * 64)).getSubimage(18, 4, 48, 48);
        }

        return sprites;

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
    

/** 
 *
 * Horizontally flips the image
 *
 * @param image  the image. 
 * @return BufferedImage
 */
    public static BufferedImage flip(BufferedImage image) { 

        AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
        tx.translate(-image.getWidth(null), 0);
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        return op.filter(image, null);
    }

}
