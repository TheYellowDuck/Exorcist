// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
// Required Notice: Copyright (c) 2026 George Zhang — https://github.com/TheYellowDuck

package entity;

import java.awt.Color;
//import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
//import java.awt.Image;
import java.awt.image.BufferedImage;
//import java.awt.image.BufferedImage;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

import main.GamePanel;
import main.KeyHandler;


 /**
 * The class Player extends entity
 */ 
public class Player extends Entity {
    
    GamePanel gp;
    KeyHandler keyH;
    
    public int screenX;
    public int screenY;
    
    int jumpCount;
    int maxJump;
    
    int jumpHeight;
    float xspeed;
    float yspeed;
    
    boolean jumping = false;
    boolean clickJump = false;
    boolean attacking = false;
    public boolean shielding = false;
    public int hp = 10;
    public static final int MAX_HP = 10;
    public static final int MAX_OVERFLOW_HP = MAX_HP + 4; // 2 extra overflow hearts
    private int invincibleFrames = 0;
    private boolean attackHitDealt = false;
    private int knockbackDir = 0;
    private int knockbackTimer = 0;
    private Wolf firstHitWolf = null;
    private Golem firstHitGolem = null;
    private Bat firstHitBat = null;
    private Witch firstHitWitch = null;
    private List<CurseEffect> curses = new ArrayList<>();
    private int curseFlicker = 0;
    public boolean playerDying = false;
    private int deathTimer = 0;
    private int regenTimer = 0;
    private int fallPeakY = 0;
    private boolean wasOnGround = true;

    public static final int SHIELD_MAX      = 300; // 5 s at 60 fps
    public static final int SHIELD_RECHARGE = 600; // 10 s cooldown after depletion
    public int shieldTimer      = SHIELD_MAX;
    public int shieldCooldown   = 0;  // depleted cooldown (10 s)
    public int shieldRegenDelay = 0;  // delay before regen starts (1 s after release)
    public int heartFlickerTimer = 0;

    BufferedImage[] jump, roll, shield;


/** 
 *
 * Player Constructor
 *
 * @param gp  the gp. 
 * @param keyH  the key H. 
 */
    public Player(GamePanel gp, KeyHandler keyH) { 


        this.gp = gp;
        this.keyH = keyH;
        
        screenX = (gp.screenWidth - gp.tileSize) / 2;
        screenY = (gp.screenHeight - gp.tileSize) / 2;
        
        solidArea = new Rectangle(10, 12, 27, 35);
        
        drawHitBox = false;

        getPlayerImage();
        setDefaultValues();

    }


/** 
 *
 * Sets the default values
 *
 */
    public void setDefaultValues() { 


        worldX = (gp.tileM.cols / 2) * gp.tileSize;
        worldY = (gp.tileM.bottomRow - 3) * gp.tileSize;
        speed = 4;
        direction = 1;
        setIdle();
//        setJump();
        
        jumpCount = 0;
        maxJump = 2;
        attacking = false;

        jumpHeight = 48;
        xspeed = 0;
        yspeed = 0;
        
        newWorldX = worldX;
        newWorldY = worldY;

        wasOnGround = true;
        regenTimer = 0;
        fallPeakY = worldY;

    }


/** 
 *
 * Gets the player image
 *
 */
    public void getPlayerImage() { 


        // BufferedImage idleTemp, walkTemp, attackTemp, deathTemp;

        try{
            
            idle = getSprites(ImageIO.read(getClass().getResourceAsStream("/Knight/noBKG_KnightIdle_strip.png")), 15, 64, 0);
            walk = getSprites(ImageIO.read(getClass().getResourceAsStream("/Knight/noBKG_KnightRun_strip.png")), 8, 96, 16);
            attack = getSprites(ImageIO.read(getClass().getResourceAsStream("/Knight/noBKG_KnightAttack_strip.png")), 22, 144,
                    new int[] {31,31,31,30,30,40,38,38,36,23,35,39,39,38,44,41,44,73,67,58,58,58});
            death = getSprites(ImageIO.read(getClass().getResourceAsStream("/Knight/noBKG_KnightDeath_strip.png")), 15, 96, 16);
            
            jump   = getSprites(ImageIO.read(getClass().getResourceAsStream("/Knight/noBKG_KnightJumpAndFall_strip.png")), 14, 144, new int[] {32, 32, 32, 36, 44, 53, 62, 75, 84, 88, 97, 95, 93, 93});
            shield = getSprites(ImageIO.read(getClass().getResourceAsStream("/Knight/noBKG_KnightShield_strip.png")), 7, 96, 16);

        } catch(IOException e) {
            e.printStackTrace();
        }

    }
    

/** 
 *
 * Sets to idle animation
 *
 */
    public void setIdle() { 

    	action = idle;
        spriteNum = 0;
        spriteFrameTime = 1e9 / 3.0;
        delta = 0;
        lastTime = System.nanoTime();
    }

/** 
 *
 * Sets to walk animation
 *
 */
    public void setWalk() { 

    	action = walk;
        spriteNum = 0;
        spriteFrameTime = 1e9 / 8.0;
        delta = 0;
        lastTime = System.nanoTime();
    }

/** 
 *
 * Sets to attack animation
 *
 */
    public void setAttack() { 

    	action = attack;
        spriteNum = 0;
        spriteFrameTime = 1e9 / 22.0;
        delta = 0;
        lastTime = System.nanoTime();
    }

/** 
 *
 * Sets to death animation
 *
 */
    public void setDeath() { 

    	action = death;
        spriteNum = 0;
        spriteFrameTime = 1e9 / 5.0;
        delta = 0;
        lastTime = System.nanoTime();
    }
    

/** 
 *
 * Sets to jump animation
 *
 */
    public void setJump() {

    	action = jump;
        spriteNum = 2;
        spriteFrameTime = 1e9 / 16.0;
        delta = 0;
        lastTime = System.nanoTime();
    }

    public void setShield() {

        action = shield;
        spriteNum = 0;
        spriteFrameTime = 1e9 / 10.0;
        delta = 0;
        lastTime = System.nanoTime();
    }


/** 
 *
 * Updates the player
 *
 */
    public void update() {

    	if (invincibleFrames > 0) invincibleFrames--;
    	if (heartFlickerTimer > 0) heartFlickerTimer--;
    	if (curseFlicker > 0) curseFlicker--;

    	// Process active curses — bypass invincibility frames
    	curses.removeIf(c -> c.timer <= 0 || c.source.dead);
    	for (CurseEffect c : curses) {
    	    c.timer--;
    	    if (--c.damageTimer <= 0) {
    	        c.damageTimer = 60;
    	        if (!playerDying) {
    	            hp = Math.max(0, hp - 1);
    	            regenTimer = 0;
    	            curseFlicker = 10;
    	            if (hp <= 0) {
    	                playerDying = true;
    	                deathTimer = 80;
    	                invincibleFrames = deathTimer + 1;
    	                attacking = false;
    	                setDeath();
    	            }
    	        }
    	    }
    	}

    	// Regen half heart every 20 seconds, only up to regular max
    	if (!playerDying && hp < MAX_HP) {
    		if (++regenTimer >= 1200) { hp++; regenTimer = 0; }
    	} else {
    		regenTimer = 0;
    	}

    	updatePlayer();

    	if (attacking) checkAttackHits();

    	updatePos();

    	if (attacking && spriteNum == attack.length - 1) {
    		if (firstHitWolf  != null) { firstHitWolf.takeDamage(1);  firstHitWolf  = null; }
    		if (firstHitGolem != null) { firstHitGolem.takeDamage(1); firstHitGolem = null; }
    		if (firstHitBat   != null) { firstHitBat.takeDamage(1);   firstHitBat   = null; }
    		if (firstHitWitch != null) { firstHitWitch.takeDamage(1); firstHitWitch = null; }
    		attacking = false;
    		attackHitDealt = false;
    	}

        updateSprite();

    }


    // Returns true if the hit was blocked by the shield
    public boolean takeDamage(int dmg, int attackerX) {
        if (invincibleFrames > 0 || playerDying) return false;
        if (shielding) {
            boolean attackerInFront = (direction == 1 && attackerX > worldX)
                                   || (direction == 3 && attackerX < worldX);
            if (attackerInFront) { invincibleFrames = 20; return true; }
        }
        hp = Math.max(0, hp - dmg);
        regenTimer = 0;
        knockbackDir = worldX >= attackerX ? 1 : -1;
        knockbackTimer = 4;
        yspeed = Math.max(yspeed, 3f);
        if (hp <= 0) {
            playerDying = true;
            deathTimer = 80;
            invincibleFrames = deathTimer + 1;
            attacking = false;
            setDeath();
        } else {
            invincibleFrames = 60;
        }
        return false;
    }

    private void checkAttackHits() {
        if (spriteNum < 2 || spriteNum > 6) { attackHitDealt = false; return; }
        if (attackHitDealt) return;
        int reach = solidArea.width + gp.tileSize * 5 / 4; // 2.25-tile reach
        int extX = direction == 1
            ? worldX + solidArea.x
            : worldX + solidArea.x - gp.tileSize * 3 / 2;
        Rectangle hitbox = new Rectangle(extX, worldY + solidArea.y, reach, solidArea.height);
        for (Wolf wolf : gp.wolves) {
            Rectangle wolfBox = new Rectangle(
                wolf.worldX + wolf.solidArea.x, wolf.worldY + wolf.solidArea.y,
                wolf.solidArea.width, wolf.solidArea.height);
            if (hitbox.intersects(wolfBox)) {
                wolf.takeDamage(1);
                firstHitWolf = wolf;
                attackHitDealt = true;
                break;
            }
        }
        if (!attackHitDealt) {
            for (Golem golem : gp.golems) {
                Rectangle golemBox = new Rectangle(
                    golem.worldX + golem.solidArea.x, golem.worldY + golem.solidArea.y,
                    golem.solidArea.width, golem.solidArea.height);
                if (hitbox.intersects(golemBox)) {
                    golem.takeDamage(1);
                    firstHitGolem = golem;
                    attackHitDealt = true;
                    break;
                }
            }
        }
        if (!attackHitDealt) {
            for (Bat bat : gp.bats) {
                Rectangle batBox = new Rectangle(
                    bat.worldX + bat.solidArea.x, bat.worldY + bat.solidArea.y,
                    bat.solidArea.width, bat.solidArea.height);
                if (hitbox.intersects(batBox)) {
                    bat.takeDamage(1);
                    firstHitBat = bat;
                    attackHitDealt = true;
                    break;
                }
            }
        }
        if (!attackHitDealt) {
            for (Witch witch : gp.witches) {
                Rectangle witchBox = new Rectangle(
                    witch.worldX + witch.solidArea.x, witch.worldY + witch.solidArea.y,
                    witch.solidArea.width, witch.solidArea.height);
                if (hitbox.intersects(witchBox)) {
                    witch.takeDamage(1);
                    firstHitWitch = witch;
                    attackHitDealt = true;
                    break;
                }
            }
        }
    }
    

/** 
 *
 * Update player position
 *
 */
    public void updatePlayer() {

        if (playerDying) {
            deathTimer--;
            if (deathTimer <= 0) {
                playerDying = false;
                hp = MAX_HP;
                yspeed = 0;
                jumping = false;
                jumpCount = 0;
                clickJump = false;
                attacking = false;
                attackHitDealt = false;
                knockbackTimer = 0;
                wasOnGround = true;
                regenTimer = 0;
                fallPeakY = worldY;
                curses.clear();
                setIdle();
            } else {
                worldY -= (int) yspeed;
                if (gp.frameCount % 2 == 0) yspeed -= 1f;
                // Clamp to world floor and walls — border still applies during death
                int floorY = gp.tileM.bottomRow * gp.tileSize - solidArea.y - solidArea.height;
                if (worldY > floorY) { worldY = floorY; yspeed = 0; }
                worldX = Math.max(gp.tileSize - solidArea.x,
                    Math.min((gp.tileM.cols - 1) * gp.tileSize - solidArea.x - solidArea.width, worldX));
            }
            return;
        }

        if (keyH.upPressed) {
        	if (!clickJump && jumpCount < maxJump) {
        		yspeed = 7;
        		if (jumpCount > 0)
        			yspeed += 2;
        		jumpCount ++;
        		jumping = true;
        	}
        	clickJump = true;
        }
        else {
        	clickJump = false;
        }

        // Shield: active while held, timer > 0, and not on cooldown
        boolean wasShielding = shielding;
        boolean wantShield = keyH.shieldPressed && !jumping && shieldCooldown == 0 && shieldTimer > 0;
        if (wantShield) {
            shielding = true;
            attacking = false;
            attackHitDealt = false;
            firstHitWolf  = null;
            firstHitGolem = null;
            firstHitBat   = null;
            firstHitWitch = null;
            if (action != shield) setShield();
            shieldRegenDelay = 0;
            shieldTimer--;
            if (shieldTimer == 0) { shielding = false; shieldCooldown = SHIELD_RECHARGE; }
        } else {
            shielding = false;
            if (wasShielding && shieldTimer > 0) shieldRegenDelay = 60; // 1 s delay on release
            if (shieldCooldown > 0) {
                shieldCooldown--;
                if (shieldCooldown == 0) shieldTimer = SHIELD_MAX;
            } else if (shieldTimer < SHIELD_MAX) {
                if (shieldRegenDelay > 0) shieldRegenDelay--;
                else shieldTimer++;
            }
        }

        if (!shielding && keyH.attackPressed && !attacking) {
        	attacking = true;
        	firstHitWolf  = null;
        	firstHitGolem = null;
        	firstHitBat   = null;
        	firstHitWitch = null;
        	setAttack();
        }

        xspeed = 0;

        if (knockbackTimer > 0) {
            xspeed = knockbackDir * speed * 2;
            knockbackTimer--;
        } else if (shielding) {
            // no horizontal movement while shielding
        } else if (!keyH.recent.isEmpty()) {
            if (keyH.recent.get(0) == 3) {
                xspeed -= speed;
                direction = 3;
            }
            if (keyH.recent.get(0) == 1) {
                xspeed += speed;
                direction = 1;
            }
            if (action != walk && !jumping && !attacking && !shielding) {
	            setWalk();
            }
        }
        else if (yspeed == 0 && !jumping) {
        	if (action != idle && !attacking && !shielding) {
	            setIdle();
            }
        }

        collisionOnX = false;
        collisionOnY = false;

        boolean onGround = gp.cChecker.checkTile(this, xspeed, yspeed);

        if (collisionOnX) {
        	worldX = newWorldX;
        }
        else {
	        worldX += xspeed;
        }
        if (collisionOnY) {
        	worldY = newWorldY;
        }
        else {
	        worldY -= yspeed;
        }

        if (onGround) {
        	jumpCount = 0;
        	clickJump = false;
        	yspeed = 0;
        	jumping = false;
        }
        else if (gp.frameCount % 2 == 0) {
        	yspeed -= 1f;
        }

        // Fall damage — track peak Y while airborne, deal 1 heart on landing > 10 tiles
        if (wasOnGround && !onGround) {
        	fallPeakY = worldY;
        }
        if (!wasOnGround && onGround) {
        	int fallDist = worldY - fallPeakY;
        	if (fallDist > 10 * gp.tileSize) takeDamage(2, worldX);
        }
        wasOnGround = onGround;

        if (!attacking) {
	        if (yspeed > 0 && action != jump) {
	        	setJump();
	        }
	        if (yspeed < -1f) {
	        	action = jump;
	        	spriteNum = 8;
	        	delta = 0;
	        	lastTime = System.nanoTime();
	        }
        }
    }
    

/** 
 *
 * Update screen position
 *
 */
    public void updatePos() { 

    			
		int tileWorldXL = 0;
		int tileScreenXL = tileWorldXL - worldX + (gp.screenWidth - gp.tileSize) / 2;

		int tileWorldXR = (gp.tileM.cols - 1) * gp.tileSize;
		int tileScreenXR = tileWorldXR - worldX + (gp.screenWidth - gp.tileSize) / 2;

		// Map is infinite upward — only clamp camera at the bottom (ground)
		int tileWorldYD = gp.tileM.bottomRow * gp.tileSize;
		int tileScreenYD = tileWorldYD - worldY + (gp.screenHeight - gp.tileSize) / 2;

		if (tileScreenXL > 0)
			screenX = worldX;
		else if (tileScreenXR < gp.screenWidth - gp.tileSize)
			screenX = gp.screenWidth - gp.tileSize + worldX - tileWorldXR;
		else
			screenX = (gp.screenWidth - gp.tileSize) / 2;

		if (tileScreenYD < gp.screenHeight - gp.tileSize)
			screenY = gp.screenHeight - gp.tileSize + worldY - tileWorldYD;
		else
			screenY = (gp.screenHeight - gp.tileSize) / 2;
    }
    

/** 
 *
 * Update sprite frame
 *
 */
    public void updateSprite() {

    	currTime = System.nanoTime();

        delta += (currTime - lastTime) / spriteFrameTime;
        lastTime = currTime;

        if (delta >= 1) {
            delta--;
            if (playerDying && spriteNum >= action.length - 1) return;
            if (shielding    && spriteNum >= action.length - 1) return;
            spriteNum = (spriteNum + 1) % action.length;
        }
    }


/** 
 *
 * Draws the player
 *
 * @param g2  the g2. 
 */
    public void draw(Graphics2D g2) {

        if (playerDying && (deathTimer / 3) % 2 == 0) return;
        if (!playerDying && invincibleFrames > 0 && (invincibleFrames / 4) % 2 == 0) return;
        if (!playerDying && curseFlicker > 0 && (curseFlicker / 2) % 2 == 0) return;

    	if (direction == 1)
    		g2.drawImage(action[spriteNum], screenX, screenY, tileSize, tileSize, null);
    	if (direction == 3)
    		g2.drawImage(flip(action[spriteNum]), screenX, screenY, tileSize, tileSize, null);
    	if (drawHitBox) {
	    	g2.setColor(Color.white);
	    	g2.drawRect(screenX + solidArea.x, screenY + solidArea.y, solidArea.width, solidArea.height);
    	}
    }

    public void fullReset() {
        hp = MAX_HP;
        playerDying = false;
        invincibleFrames = 0;
        knockbackTimer = 0;
        knockbackDir = 0;
        attacking = false;
        attackHitDealt = false;
        shielding = false;
        shieldTimer = SHIELD_MAX;
        shieldCooldown = 0;
        shieldRegenDelay = 0;
        heartFlickerTimer = 0;
        curseFlicker = 0;
        curses.clear();
        firstHitWolf = null;
        firstHitGolem = null;
        firstHitBat = null;
        firstHitWitch = null;
        setDefaultValues();
    }

    public void addCurse(Witch witch) {
        for (CurseEffect c : curses) {
            if (c.source == witch) { c.timer = 300; return; }
        }
        curses.add(new CurseEffect(witch));
    }

    public static class CurseEffect {
        public Witch source;
        int timer = 300;
        int damageTimer = 60;
        CurseEffect(Witch w) { source = w; }
    }

}
