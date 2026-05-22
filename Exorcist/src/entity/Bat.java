package entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import main.GamePanel;

public class Bat extends Entity {

    GamePanel gp;

    public int hp = 2;
    public boolean dead = false;

    float xspeed = 0;
    float yspeed = 0;

    private enum BatState { IDLE, CHASE, DYING }
    private BatState state = BatState.IDLE;

    public int invincibleFrames = 0;
    private int attackCooldown = 0;
    private int wanderTimer = 0;
    private float wanderXspeed = 0;
    private float wanderYspeed = 0;

    public Bat(GamePanel gp, int worldX, int worldY) {
        this.gp = gp;
        this.worldX = worldX;
        this.worldY = worldY;
        this.speed = 3;
        this.direction = 1;
        this.solidArea = new Rectangle(6, 24, 24, 24);
        drawHitBox = false;
        loadImages();
        setAnim(idle, 1e9 / 8.0);
    }

    private void loadImages() {
        try {
            idle   = getSprites(ImageIO.read(getClass().getResourceAsStream("/Bat/noBKG_BatFlight_strip.png")),  8, 64, 0);
            attack = getSprites(ImageIO.read(getClass().getResourceAsStream("/Bat/noBKG_BatAttack_strip.png")), 10, 64, 0);
            death  = getSprites(ImageIO.read(getClass().getResourceAsStream("/Bat/noBKG_BatDeath_strip.png")),  10, 64, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setAnim(BufferedImage[] anim, double fps) {
        if (action == anim) return;
        action = anim;
        spriteNum = 0;
        spriteFrameTime = fps;
        delta = 0;
        lastTime = System.nanoTime();
    }

    public void takeDamage(int dmg) {
        if (invincibleFrames > 0 || state == BatState.DYING) return;
        hp -= dmg;
        invincibleFrames = 12;
        if (hp <= 0) {
            state = BatState.DYING;
            xspeed = 0;
            yspeed = 0;
            action = death;
            spriteNum = 0;
            spriteFrameTime = 1e9 / 8.0;
            delta = 0;
            lastTime = System.nanoTime();
            int prevHp = gp.player.hp;
            gp.player.hp = Math.min(Player.MAX_OVERFLOW_HP, gp.player.hp + 1);
            if (gp.player.hp > prevHp) gp.player.heartFlickerTimer = 90;
        }
    }

    public void update() {
        if (invincibleFrames > 0) invincibleFrames--;
        if (attackCooldown > 0) attackCooldown--;

        switch (state) {
            case IDLE:  updateIdle();  break;
            case CHASE: updateChase(); break;
            case DYING: updateDying(); break;
        }

        updateSprite();
    }

    private Rectangle batSolidRect() {
        return new Rectangle(worldX + solidArea.x, worldY + solidArea.y, solidArea.width, solidArea.height);
    }

    private Rectangle playerSolidRect() {
        return new Rectangle(
            gp.player.worldX + gp.player.solidArea.x,
            gp.player.worldY + gp.player.solidArea.y,
            gp.player.solidArea.width, gp.player.solidArea.height);
    }

    private void updateIdle() {
        if (wanderTimer <= 0) {
            double angle = Math.random() * 2 * Math.PI;
            wanderXspeed = (float)(Math.cos(angle) * speed);
            wanderYspeed = (float)(Math.sin(angle) * speed);
            wanderTimer = 60 + (int)(Math.random() * 120);
        }
        wanderTimer--;

        worldX += (int) wanderXspeed;
        worldY -= (int) wanderYspeed; // worldY -= yspeed: positive = up

        // Bounce off world side walls and floor
        if (worldX <= gp.tileSize) {
            worldX = gp.tileSize; wanderXspeed = Math.abs(wanderXspeed); wanderTimer = 0;
        } else if (worldX >= (gp.tileM.cols - 2) * gp.tileSize) {
            worldX = (gp.tileM.cols - 2) * gp.tileSize; wanderXspeed = -Math.abs(wanderXspeed); wanderTimer = 0;
        }
        if (worldY >= gp.tileM.bottomRow * gp.tileSize) {
            worldY = gp.tileM.bottomRow * gp.tileSize - 1; wanderYspeed = Math.abs(wanderYspeed); wanderTimer = 0;
        }

        direction = wanderXspeed >= 0 ? 1 : 3;
        setAnim(idle, 1e9 / 8.0);

        int dx = gp.player.worldX - worldX;
        int dy = gp.player.worldY - worldY;
        if (Math.sqrt(dx * dx + dy * dy) < 6 * gp.tileSize) {
            state = BatState.CHASE;
        }
    }

    private void updateChase() {
        int dx = gp.player.worldX - worldX;
        int dy = gp.player.worldY - worldY;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist > 10 * gp.tileSize) {
            state = BatState.IDLE;
            xspeed = 0;
            yspeed = 0;
            return;
        }

        if (dist > 0) {
            xspeed = (float)(dx / dist * speed);
            yspeed = -(float)(dy / dist * speed); // worldY -= yspeed; negative yspeed = move down
        }
        direction = dx >= 0 ? 1 : 3;
        setAnim(idle, 1e9 / 8.0);

        worldX += (int) xspeed;
        worldY -= (int) yspeed;
        worldX = Math.max(gp.tileSize, Math.min((gp.tileM.cols - 2) * gp.tileSize, worldX));

        if (attackCooldown == 0 && batSolidRect().intersects(playerSolidRect())) {
            gp.player.takeDamage(1, worldX);
            attackCooldown = 60;
        }
    }

    private void updateDying() {
        if (gp.frameCount % 2 == 0) yspeed -= 1f;
        collisionOnX = false;
        collisionOnY = false;
        boolean onGround = gp.cChecker.checkTile(this, 0, yspeed);
        if (collisionOnY) worldY = newWorldY;
        else              worldY -= (int) yspeed;
        if (onGround) yspeed = 0;
    }

    private void updateSprite() {
        currTime = System.nanoTime();
        delta += (currTime - lastTime) / spriteFrameTime;
        lastTime = currTime;
        if (delta >= 1) {
            delta--;
            if (state == BatState.DYING) {
                if (spriteNum < action.length - 1) spriteNum++;
                else dead = true;
            } else {
                spriteNum = (spriteNum + 1) % action.length;
            }
        }
    }

    public void draw(Graphics2D g2) {
        int screenX = worldX - gp.player.worldX + gp.player.screenX;
        int screenY = worldY - gp.player.worldY + gp.player.screenY;

        if (screenX + tileSize < 0 || screenX > gp.screenWidth) return;
        if (screenY + tileSize < 0 || screenY > gp.screenHeight) return;

        if (invincibleFrames > 0 && (invincibleFrames / 3) % 2 == 0) return;

        BufferedImage frame = action[spriteNum];
        if (direction == 1) g2.drawImage(frame, screenX, screenY, tileSize, tileSize, null);
        else                g2.drawImage(flip(frame), screenX, screenY, tileSize, tileSize, null);

        if (drawHitBox) {
            g2.setColor(Color.cyan);
            g2.drawRect(screenX + solidArea.x, screenY + solidArea.y, solidArea.width, solidArea.height);
        }
    }
}
