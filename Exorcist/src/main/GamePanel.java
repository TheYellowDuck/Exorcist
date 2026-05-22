package main;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import java.util.ArrayList;
import java.util.List;

import entity.Bat;
import entity.Entity;
import entity.Golem;
import entity.Player;
import entity.Witch;
import entity.Wolf;
import tile.TileManager;


 /**
 * The class Game panel extends J panel implements runnable
 */ 
public class GamePanel extends JPanel implements Runnable {
	
	BufferedImage background;

	// SCREEN SETTINGS
    public final int originalTileSize = 16; // 16x16 tile
    public final int scale = 3;

    public final int tileSize = originalTileSize * scale; // 48x48 tile
    public final int maxScreenCol = 20;
    public final int maxScreenRow = 12;
    public final int screenWidth = tileSize * maxScreenCol; // 960 pixels
    public final int screenHeight = tileSize * maxScreenRow; // 576 pixels
    
    // WORLD SETTINGS
//    public final int maxWorldCol = 50;
//    public final int maxWorldRow = 50;
//    public final int worldWidth = tileSize * maxWorldCol;
//    public final int worldHeight = tileSize * maxWorldRow;

    // FPS
    int FPS = 60;
    public int frameCount = 0;

    public TileManager tileM = new TileManager(this);
    KeyHandler keyH = new KeyHandler();
    Thread gameThread;
    public CollisionChecker cChecker = new CollisionChecker(this);
    public Player player = new Player(this, keyH);
    public List<Wolf> wolves = new ArrayList<>();
    public List<Golem> golems = new ArrayList<>();
    public List<Bat> bats = new ArrayList<>();
    public List<Witch> witches = new ArrayList<>();
    private int batSpawnTimer = 0;
    private int batSpawnInterval = 600;

    private enum GameState { HOME, PLAYING, PAUSED }
    private GameState gameState = GameState.HOME;
    private int highScore = 0;
    private static final String HIGH_SCORE_FILE = getHighScorePath();

    private static String getHighScorePath() {
        try {
            java.io.File jar = new java.io.File(
                GamePanel.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return jar.getParent() + java.io.File.separator + "highscore.dat";
        } catch (Exception e) {
            return "highscore.dat";
        }
    }


/** 
 *
 * Game panel Constructor
 *
 */
    public GamePanel() { 


        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.black);
        this.setDoubleBuffered(true);
        this.addKeyListener(keyH);
        this.addMouseListener(keyH);
        this.setFocusable(true);
        
        try {
        	background = drawBackground(Entity.resize(ImageIO.read(getClass().getResourceAsStream("/Tiles/Background.png")), screenWidth, screenHeight));
        } catch (IOException e) {
        	e.printStackTrace();
        }

        loadHighScore();

    }

    public void loadHighScore() {
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(HIGH_SCORE_FILE))) {
            highScore = Integer.parseInt(br.readLine().trim());
        } catch (Exception e) {
            highScore = 0;
        }
    }

    public void saveHighScore() {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(HIGH_SCORE_FILE))) {
            pw.println(highScore);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


/** 
 *
 * Start game thread
 *
 */
    public void startGameThread() { 


        gameThread = new Thread(this);
        gameThread.start();

    }

    @Override

/** 
 *
 * Run
 *
 */
    public void run() { 


        // double drawInterval = 1e9 / FPS; // 0.01666666 seconds
        // double nextDrawTime = System.nanoTime() + drawInterval;
        
        // while(gameThread != null) {

        //     // 1 UPDATE; update information such as character positions
        //     update();

        //     // 2 DRAW: draw the screen with the updated information
        //     repaint();


        //     try {

        //         double remainingTime = nextDrawTime - System.nanoTime();
        //         remainingTime /= 1e6;

        //         if (remainingTime < 0)
        //             remainingTime = 0;

        //         Thread.sleep((long) remainingTime);

        //         nextDrawTime += drawInterval;
                
        //     } catch (InterruptedException e) {
        //         // TODO Auto-generated catch block
        //         e.printStackTrace();
        //     }

        // }

        double drawInterval = 1e9 / FPS; // 0.01666666 seconds
        double delta = 0;
        long lastTime = System.nanoTime();
        long currTime;
        

        int prevFrameCount = 0;
        long prevTime = System.nanoTime();

        
        while(gameThread != null) {

            currTime = System.nanoTime();

            delta += (currTime - lastTime) / drawInterval;
            lastTime = currTime;

            if (delta >= 1) {
                // 1 UPDATE; update information such as character positions
                update();

                // 2 DRAW: draw the screen with the updated information
                repaint();
                
                frameCount ++;

                delta --;
            }
            
            if (currTime >= prevTime + 1e9) {
            	System.out.println("FPS: " + (frameCount - prevFrameCount));
            	prevFrameCount = frameCount;
            	prevTime += 1e9;
            }

        }

    }

/** 
 *
 * Update
 *
 */
    private void resetGame() {
        wolves.clear();
        golems.clear();
        bats.clear();
        witches.clear();
        tileM = new TileManager(this);
        player.fullReset();
        batSpawnTimer = 0;
        batSpawnInterval = 600 + (int)(Math.random() * 1200);
    }

    public void update() {

        if (keyH.escJustPressed) {
            keyH.escJustPressed = false;
            if (gameState == GameState.PLAYING) gameState = GameState.PAUSED;
            else if (gameState == GameState.PAUSED) gameState = GameState.PLAYING;
        }
        if (keyH.enterJustPressed) {
            keyH.enterJustPressed = false;
            if (gameState == GameState.HOME) { resetGame(); gameState = GameState.PLAYING; }
        }
        if (keyH.hJustPressed) {
            keyH.hJustPressed = false;
            if (gameState == GameState.PAUSED) { resetGame(); gameState = GameState.HOME; }
        }

        if (gameState != GameState.PLAYING) return;

        tileM.update();
        player.update();

        for (int[] spawn : tileM.pendingSpawns) {
            if (wolves.size() < 10) wolves.add(new Wolf(this, spawn[0], spawn[1]));
        }
        tileM.pendingSpawns.clear();

        for (int[] spawn : tileM.pendingGolemSpawns) {
            if (golems.size() < 5) golems.add(new Golem(this, spawn[0], spawn[1]));
        }
        tileM.pendingGolemSpawns.clear();

        for (int[] spawn : tileM.pendingWitchSpawns) {
            if (witches.size() < 5) witches.add(new Witch(this, spawn[0], spawn[1]));
        }
        tileM.pendingWitchSpawns.clear();

        wolves.removeIf(w -> w.dead);
        for (Wolf wolf : wolves) wolf.update();

        golems.removeIf(g -> g.dead);
        for (Golem golem : golems) golem.update();

        if (++batSpawnTimer >= batSpawnInterval && bats.size() < 8) {
            batSpawnTimer = 0;
            batSpawnInterval = 600 + (int)(Math.random() * 1200);
            int side = Math.random() < 0.5 ? -1 : 1;
            int spawnX = player.worldX + side * (screenWidth / 2 + tileSize * 2);
            int spawnY = player.worldY + (int)((Math.random() - 0.5) * screenHeight);
            spawnX = Math.max(tileSize, Math.min((tileM.cols - 2) * tileSize, spawnX));
            bats.add(new Bat(this, spawnX, spawnY));
        }

        bats.removeIf(b -> b.dead);
        for (Bat bat : bats) bat.update();

        witches.removeIf(w -> w.dead);
        for (Witch witch : witches) witch.update();

    }

/** 
 *
 * Paint component
 *
 * @param g  the g. 
 */
    public void paintComponent(Graphics g) { 


        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        if (gameState == GameState.HOME) {
            drawHomeScreen(g2);
            g2.dispose();
            return;
        }

        g2.drawImage(background, 0, 0, null);
        darken(g2);

        tileM.draw(g2);
        for (Wolf wolf : wolves) wolf.draw(g2);
        for (Golem golem : golems) golem.draw(g2);
        for (Bat bat : bats) bat.draw(g2);
        for (Witch witch : witches) witch.draw(g2);
        player.draw(g2);
        drawHUD(g2);

        if (gameState == GameState.PAUSED) drawPauseScreen(g2);

        g2.dispose();
    }
    

/** 
 *
 * Draw background
 *
 * @param img  the img. 
 * @return BufferedImage
 */
    public BufferedImage drawBackground(BufferedImage img) { 

    	
    	Graphics2D g2 = (Graphics2D) img.getGraphics();
    	
    	float alpha = 0.025f;
    	
    	g2.setColor(Color.black);
    	
    	int start = screenHeight / 15;
    	
    	for (int i = start; i > -start; i --) {
    		
    		Area a = new Area(new Rectangle(-(int) (start * (double) (screenWidth / screenHeight)), -start, screenWidth + 2 * (int) (start * (double) (screenWidth / screenHeight)), screenHeight + 2 * start));
    		a.subtract(new Area(new Ellipse2D.Double((int) (i * (double) (screenWidth / screenHeight)), i, screenWidth - 2 * (int) (i * (double) (screenWidth / screenHeight)), screenHeight - 2 * i)));
    		
    		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
    		g2.fill(a);
    		
    		i -= (int) Math.abs(i / 1.125);
    		
//    		alpha += 1.0 / (start);
//    		if (alpha > 1f) alpha = 1f;
    	}
    	
//    	Area a = new Area(new Rectangle(-start, -(int) (start * (double) (screenHeight / screenWidth)), screenWidth + 2 * start, screenHeight + 2 * (int) (start * (double) (screenHeight / screenWidth))));
//		a.subtract(new Area(new Ellipse2D.Double(start, (int) (start * (double) (screenHeight / screenWidth)), screenWidth - 2 * start, screenHeight - 2 * (int) (start * (double) (screenHeight / screenWidth)))));
//		g2.setColor(Color.white);
//		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
//		g2.fill(a);
    	
    	return img;
    }
    

/** 
 *
 * Darken the image
 *
 * @param g2  the g2. 
 */
    // 7×6 pixel heart grid (Minecraft-style)
    private static final int[][] HEART_PIXELS = {
        {0,1,1,0,1,1,0},
        {1,1,1,1,1,1,1},
        {1,1,1,1,1,1,1},
        {0,1,1,1,1,1,0},
        {0,0,1,1,1,0,0},
        {0,0,0,1,0,0,0},
    };

    public void drawHUD(Graphics2D g2) {

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        // Pixel hearts — 3px per cell, 5px gap between hearts
        // Each heart slot = 2 HP; half-heart = 1 HP; slots 5-6 are overflow (gold)
        int px        = 3;
        int gap       = 5;
        int barX = 16, barY = 16;
        int regularHearts  = Player.MAX_HP / 2;          // 5
        int overflowHearts = (Player.MAX_OVERFLOW_HP - Player.MAX_HP) / 2; // 2
        int totalHearts    = regularHearts + overflowHearts; // 7
        for (int i = 0; i < totalHearts; i++) {
            boolean isOverflow = i >= regularHearts;
            int ox = barX + i * (7 * px + gap);
            boolean isFull = player.hp >= (i + 1) * 2;
            boolean isHalf = !isFull && player.hp >= i * 2 + 1;
            boolean gained = i == (player.hp - 1) / 2 && player.heartFlickerTimer > 0;
            for (int r = 0; r < HEART_PIXELS.length; r++) {
                for (int c = 0; c < HEART_PIXELS[r].length; c++) {
                    if (HEART_PIXELS[r][c] == 0) continue;
                    boolean leftSide = c < 4;
                    boolean showFilled = isFull || (isHalf && leftSide);
                    if (showFilled) {
                        boolean flicker = false;
                        if (gained && player.heartFlickerTimer > 8) {
                            int t = player.heartFlickerTimer;
                            int period = t > 60 ? 2 : t > 30 ? 4 : 8;
                            flicker = (t / period) % 2 == 0;
                        }
                        if (isOverflow) {
                            g2.setColor(flicker
                                ? new Color(80, 50, 0)
                                : r == 0 ? new Color(255, 210, 60) : new Color(220, 160, 20));
                        } else {
                            g2.setColor(flicker
                                ? new Color(60, 15, 15)
                                : r == 0 ? new Color(255, 100, 100) : new Color(220, 30, 30));
                        }
                    } else {
                        if (isOverflow) continue; // don't render empty outline for bonus hearts
                        g2.setColor(new Color(60, 15, 15));
                    }
                    g2.fillRect(ox + c * px, barY + r * px, px, px);
                }
            }
        }

        // Shield bar spans regular hearts only (overflow hearts are bonus, not shieldable)
        int heartsHeight = 6 * px + 4;
        int shieldBarY = barY + heartsHeight;
        int shieldBarW = regularHearts * (7 * px + gap) - gap;
        int shieldBarH = 4;
        g2.setColor(new Color(30, 30, 30));
        g2.fillRect(barX, shieldBarY, shieldBarW, shieldBarH);
        if (player.shieldCooldown > 0) {
            // Depleted — show cooldown progress (fills as cooldown counts down)
            float progress = 1f - (float) player.shieldCooldown / Player.SHIELD_RECHARGE;
            g2.setColor(new Color(60, 60, 90));
            g2.fillRect(barX, shieldBarY, (int)(shieldBarW * progress), shieldBarH);
        } else if (player.shieldRegenDelay > 0) {
            // Released, waiting to regen — bar holds at current level, amber tint
            float fill = (float) player.shieldTimer / Player.SHIELD_MAX;
            g2.setColor(new Color(160, 120, 30));
            g2.fillRect(barX, shieldBarY, (int)(shieldBarW * fill), shieldBarH);
        } else {
            // Available or actively shielding
            float fill = (float) player.shieldTimer / Player.SHIELD_MAX;
            g2.setColor(player.shielding ? new Color(80, 180, 255) : new Color(50, 120, 200));
            g2.fillRect(barX, shieldBarY, (int)(shieldBarW * fill), shieldBarH);
        }

        // Height and highscore — top right
        int currentHeight = Math.max(0, ((tileM.bottomRow - 3) * tileSize - player.worldY) / tileSize);
        if (currentHeight > highScore) highScore = currentHeight;

        g2.setFont(new Font("Monospaced", Font.BOLD, 14));
        g2.setColor(Color.white);
        g2.drawString("Height : " + currentHeight, screenWidth - 155, 30);
        g2.drawString("Best   : " + highScore,     screenWidth - 155, 50);
    }

    private void drawHomeScreen(Graphics2D g2) {
        g2.drawImage(background, 0, 0, null);
        g2.setColor(Color.black);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
        g2.fillRect(0, 0, screenWidth, screenHeight);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        g2.setFont(new Font("Monospaced", Font.BOLD, 64));
        g2.setColor(Color.white);
        FontMetrics fm = g2.getFontMetrics();
        String title = "EXORCIST";
        g2.drawString(title, (screenWidth - fm.stringWidth(title)) / 2, screenHeight / 2 - 20);

        g2.setFont(new Font("Monospaced", Font.BOLD, 18));
        fm = g2.getFontMetrics();
        String prompt = "PRESS  ENTER  TO  PLAY";
        g2.setColor(new Color(200, 200, 200));
        g2.drawString(prompt, (screenWidth - fm.stringWidth(prompt)) / 2, screenHeight / 2 + 40);

        if (highScore > 0) {
            g2.setFont(new Font("Monospaced", Font.PLAIN, 14));
            fm = g2.getFontMetrics();
            String hs = "Best : " + highScore;
            g2.setColor(new Color(140, 140, 140));
            g2.drawString(hs, (screenWidth - fm.stringWidth(hs)) / 2, screenHeight / 2 + 80);
        }
    }

    private void drawPauseScreen(Graphics2D g2) {
        g2.setColor(Color.black);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
        g2.fillRect(0, 0, screenWidth, screenHeight);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        g2.setFont(new Font("Monospaced", Font.BOLD, 48));
        g2.setColor(Color.white);
        FontMetrics fm = g2.getFontMetrics();
        String title = "PAUSED";
        g2.drawString(title, (screenWidth - fm.stringWidth(title)) / 2, screenHeight / 2 - 20);

        g2.setFont(new Font("Monospaced", Font.BOLD, 16));
        fm = g2.getFontMetrics();
        g2.setColor(new Color(200, 200, 200));
        String resume = "ESC  —  Resume";
        String home   = "H  —  Home";
        g2.drawString(resume, (screenWidth - fm.stringWidth(resume)) / 2, screenHeight / 2 + 30);
        g2.drawString(home,   (screenWidth - fm.stringWidth(home))   / 2, screenHeight / 2 + 56);
    }

    public void darken(Graphics2D g2) {

    	
    	float alpha = 0.5f;
    	
    	g2.setColor(Color.black);
    	g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
    	
    	g2.fillRect(0, 0, screenWidth, screenHeight);
    	
    	g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    	
	}

}
