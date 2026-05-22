package entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import main.GamePanel;

public class Wolf extends Entity {

    GamePanel gp;

    public int hp = 6;
    public boolean dead = false;

    float xspeed = 0;
    float yspeed = 0;
    boolean onGround = false;

    private enum WolfState { PATROL, CHASE, ATTACK, STUNNED, DYING }
    private WolfState state = WolfState.PATROL;

    private int patrolDir = 1;
    private int attackCooldown = 0;
    public int invincibleFrames = 0;
    private int chaseLostFrames = 0;
    private int stunTimer = 0;
    private int attackLungeDir = 0;
    private int springBackTimer = 0;
    private int attackStartX = 0;

    public Wolf(GamePanel gp, int worldX, int worldY) {
        this.gp = gp;
        this.worldX = worldX;
        this.worldY = worldY;
        this.speed = 5;
        this.direction = 1;
        this.solidArea = new Rectangle(10, 29, 27, 18);
        drawHitBox = false;
        loadImages();
        setAnim(idle, 1e9 / 6.0);
    }

    private void loadImages() {
        try {
            idle   = getSprites(ImageIO.read(getClass().getResourceAsStream("/Wolf/noBKG_WolfIdle_strip.png")),   12, 64, 0);
            walk   = getSprites(ImageIO.read(getClass().getResourceAsStream("/Wolf/noBKG_WolfRun_strip.png")),     8,  64, 0);
            attack = getSprites(ImageIO.read(getClass().getResourceAsStream("/Wolf/noBKG_WolfAttack_strip.png")), 16, 64, 0);
            death  = getSprites(ImageIO.read(getClass().getResourceAsStream("/Wolf/noBKG_WolfDeath_strip.png")),  18, 64, 0);
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
        if (invincibleFrames > 0 || state == WolfState.DYING) return;
        hp -= dmg;
        invincibleFrames = 15;
        if (hp <= 0) {
            state = WolfState.DYING;
            xspeed = 0;
            action = death;
            spriteNum = 0;
            spriteFrameTime = 1e9 / 8.0;
            delta = 0;
            lastTime = System.nanoTime();
            int prevHp = gp.player.hp;
            gp.player.hp = Math.min(Player.MAX_OVERFLOW_HP, gp.player.hp + 2);
            if (gp.player.hp > prevHp) gp.player.heartFlickerTimer = 90;
        }
    }

    public void update() {
        if (invincibleFrames > 0) invincibleFrames--;
        if (attackCooldown > 0) attackCooldown--;

        switch (state) {
            case PATROL:  updatePatrol();  break;
            case CHASE:   updateChase();   break;
            case ATTACK:  updateAttack();  break;
            case STUNNED: updateStunned(); break;
            case DYING:   xspeed = 0;     break;
        }

        collisionOnX = false;
        collisionOnY = false;
        onGround = gp.cChecker.checkTile(this, xspeed, yspeed);

        if (collisionOnX) {
            worldX = newWorldX;
            if (state == WolfState.PATROL) patrolDir = -patrolDir;
        } else {
            worldX += xspeed;
        }
        if (collisionOnY) worldY = newWorldY;
        else              worldY -= yspeed;

        if (onGround)                    yspeed = 0;
        else if (gp.frameCount % 2 == 0) yspeed -= 1f;

        updateSprite();
    }

    // Returns false if taking one step in dir would leave the platform edge
    private boolean edgeSafe(int dir) {
        int nextX = worldX + dir * speed;
        int checkCol = dir > 0
            ? (nextX + solidArea.x + solidArea.width) / gp.tileSize
            : (nextX + solidArea.x) / gp.tileSize;
        int checkRow = (worldY + solidArea.y + solidArea.height) / gp.tileSize;
        return gp.tileM.getTileAt(checkRow, checkCol) != 0;
    }

    private int wolfFeetRow()   { return (worldY + solidArea.y + solidArea.height) / gp.tileSize; }
    private int playerFeetRow() { return (gp.player.worldY + gp.player.solidArea.y + gp.player.solidArea.height) / gp.tileSize; }

    // Strict: same ground row, wolf is facing the player, within 7 tiles
    private boolean canSpotPlayer() {
        if (wolfFeetRow() != playerFeetRow()) return false;
        int dx = gp.player.worldX - worldX;
        if (direction == 1 && dx < 0) return false;
        if (direction == 3 && dx > 0) return false;
        return Math.abs(dx) < 7 * gp.tileSize;
    }

    private boolean lostPlayer() {
        int rowDiff = playerFeetRow() - wolfFeetRow(); // positive = player is lower
        // Instant give-up: player dropped to a lower platform, or way out of range
        if (rowDiff > 0) return true;
        if (Math.abs(gp.player.worldX - worldX) > 10 * gp.tileSize) return true;
        // Timer give-up: player is above (jumping); tolerate for 40 frames so a
        // normal jump on this platform doesn't break the chase
        if (rowDiff == 0) { chaseLostFrames = 0; return false; }
        return ++chaseLostFrames > 40;
    }

    private Rectangle wolfSolidRect() {
        return new Rectangle(worldX + solidArea.x, worldY + solidArea.y, solidArea.width, solidArea.height);
    }

    private Rectangle playerSolidRect() {
        return new Rectangle(
            gp.player.worldX + gp.player.solidArea.x,
            gp.player.worldY + gp.player.solidArea.y,
            gp.player.solidArea.width, gp.player.solidArea.height);
    }

    private void updatePatrol() {
        if (!edgeSafe(patrolDir)) patrolDir = -patrolDir;
        xspeed = patrolDir * speed;
        direction = patrolDir > 0 ? 1 : 3;
        setAnim(walk, 1e9 / 10.0);

        if (canSpotPlayer()) { chaseLostFrames = 0; state = WolfState.CHASE; }
    }

    private void updateChase() {
        // Spring back to exactly where the attack started
        if (springBackTimer > 0) {
            int target = attackStartX - attackLungeDir * (gp.tileSize / 2);
            int diff = target - worldX;
            if (Math.abs(diff) < speed) { worldX = target; xspeed = 0; springBackTimer = 0; }
            else { xspeed = diff > 0 ? speed : -speed; springBackTimer--; }
            direction = attackLungeDir > 0 ? 1 : 3;
            setAnim(idle, 1e9 / 6.0);
            return;
        }
        int dx = gp.player.worldX - worldX;

        if (lostPlayer()) {
            state = WolfState.PATROL;
            xspeed = 0;
            setAnim(idle, 1e9 / 6.0);
            return;
        }
        // In melee range — attack if ready, otherwise hold position
        if (wolfSolidRect().intersects(playerSolidRect())) {
            if (attackCooldown == 0) {
                xspeed = 0;
                attackStartX = worldX;
                attackLungeDir = gp.player.worldX >= worldX ? 1 : -1;
                state = WolfState.ATTACK;
                action = attack;
                spriteNum = 0;
                spriteFrameTime = 1e9 / 16.0;
                delta = 0;
                lastTime = System.nanoTime();
            } else {
                xspeed = 0;
                setAnim(idle, 1e9 / 6.0);
            }
            return;
        }
        if (!onGround) { xspeed = 0; return; }
        int moveDir = dx > 0 ? 1 : -1;
        if (!edgeSafe(moveDir)) {
            xspeed = 0;
            setAnim(idle, 1e9 / 6.0);
            return;
        }
        xspeed = moveDir * speed;
        direction = moveDir > 0 ? 1 : 3;
        setAnim(walk, 1e9 / 10.0);
    }

    private void updateAttack() {
        xspeed = 0;
        direction = attackLungeDir > 0 ? 1 : 3;

        if (wolfSolidRect().intersects(playerSolidRect())) {
            boolean blocked = gp.player.takeDamage(2, worldX);
            if (blocked) {
                springBackTimer = 12;
                stunTimer = 90;
                state = WolfState.STUNNED;
                setAnim(idle, 1e9 / 6.0);
                return;
            }
        }
        if (spriteNum == attack.length - 1) {
            springBackTimer = 0;
            attackCooldown = 28;
            state = WolfState.CHASE;
            action = idle;
            spriteNum = 0;
            spriteFrameTime = 1e9 / 6.0;
            delta = 0;
            lastTime = System.nanoTime();
        }
    }

    private void updateStunned() {
        if (springBackTimer > 0) {
            int target = attackStartX - attackLungeDir * (gp.tileSize / 2);
            int diff = target - worldX;
            if (Math.abs(diff) < speed) { worldX = target; xspeed = 0; springBackTimer = 0; }
            else { xspeed = diff > 0 ? speed : -speed; springBackTimer--; }
        } else {
            xspeed = 0;
        }
        setAnim(idle, 1e9 / 6.0);
        if (--stunTimer <= 0) {
            state = WolfState.PATROL;
            chaseLostFrames = 0;
        }
    }

    private void updateSprite() {
        currTime = System.nanoTime();
        delta += (currTime - lastTime) / spriteFrameTime;
        lastTime = currTime;
        if (delta >= 1) {
            delta--;
            if (state == WolfState.DYING) {
                if (spriteNum < action.length - 1) spriteNum++;
                else {
                    dead = true;
                }
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

        if (invincibleFrames > 0 && (invincibleFrames / 4) % 2 == 0) return;

        BufferedImage frame = action[spriteNum];
        if (direction == 1) g2.drawImage(frame, screenX, screenY, tileSize, tileSize, null);
        else                g2.drawImage(flip(frame), screenX, screenY, tileSize, tileSize, null);

        if (drawHitBox) {
            g2.setColor(Color.red);
            g2.drawRect(screenX + solidArea.x, screenY + solidArea.y, solidArea.width, solidArea.height);
        }
    }
}
