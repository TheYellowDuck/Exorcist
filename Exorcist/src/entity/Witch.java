package entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import main.GamePanel;
import main.Sound;

public class Witch extends Entity {

    GamePanel gp;

    public int hp = 4;
    public boolean dead = false;

    float xspeed = 0;
    float yspeed = 0;
    boolean onGround = false;

    private enum WitchState { PATROL, CHASE, ATTACK, DYING }
    private WitchState state = WitchState.PATROL;

    private int patrolDir = 1;
    private int attackCooldown = 0;
    public int invincibleFrames = 0;
    private int chaseLostFrames = 0;
    private boolean attackApplied = false;
    private boolean attackSoundPlayed = false;

    public Witch(GamePanel gp, int worldX, int worldY) {
        this.gp = gp;
        this.worldX = worldX;
        this.worldY = worldY;
        this.speed = 2;
        this.direction = 1;
        this.solidArea = new Rectangle(10, 12, 28, 36);
        drawHitBox = false;
        loadImages();
        setAnim(idle, 1e9 / 7.0);
    }

    private void loadImages() {
        try {
            idle   = getSprites(ImageIO.read(getClass().getResourceAsStream("/Witch/noBKG_WitchIdle_strip.png")),  7, 64, 0);
            walk   = getSprites(ImageIO.read(getClass().getResourceAsStream("/Witch/noBKG_WitchWalk_strip.png")),  8, 64, 0);
            attack = getSprites(ImageIO.read(getClass().getResourceAsStream("/Witch/noBKG_WitchThrow_strip.png")), 18, 64, 0);
            death  = getSprites(ImageIO.read(getClass().getResourceAsStream("/Witch/noBKG_WitchDeath_strip.png")), 12, 64, 0);
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
        if (invincibleFrames > 0 || state == WitchState.DYING) return;
        hp -= dmg;
        invincibleFrames = 15;
        if (hp <= 0) {
            state = WitchState.DYING;
            xspeed = 0;
            action = death;
            spriteNum = 0;
            spriteFrameTime = 1e9 / 8.0;
            delta = 0;
            lastTime = System.nanoTime();
            int prevHp = gp.player.hp;
            gp.player.hp = Math.min(Player.MAX_OVERFLOW_HP, gp.player.hp + 6);
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
            case DYING:   xspeed = 0;     break;
        }

        collisionOnX = false;
        collisionOnY = false;
        onGround = gp.cChecker.checkTile(this, xspeed, yspeed);

        if (collisionOnX) {
            worldX = newWorldX;
            if (state == WitchState.PATROL) patrolDir = -patrolDir;
        } else {
            worldX += xspeed;
        }
        if (collisionOnY) worldY = newWorldY;
        else              worldY -= yspeed;

        if (onGround)                    yspeed = 0;
        else if (gp.frameCount % 2 == 0) yspeed -= 1f;

        updateSprite();
    }

    private boolean edgeSafe(int dir) {
        int nextX = worldX + dir * speed;
        int checkCol = dir > 0
            ? (nextX + solidArea.x + solidArea.width) / gp.tileSize
            : (nextX + solidArea.x) / gp.tileSize;
        int checkRow = (worldY + solidArea.y + solidArea.height) / gp.tileSize;
        return gp.tileM.getTileAt(checkRow, checkCol) != 0;
    }

    private int witchFeetRow()  { return (worldY + solidArea.y + solidArea.height) / gp.tileSize; }
    private int playerFeetRow() { return (gp.player.worldY + gp.player.solidArea.y + gp.player.solidArea.height) / gp.tileSize; }

    private boolean canSpotPlayer() {
        if (witchFeetRow() != playerFeetRow()) return false;
        int dx = gp.player.worldX - worldX;
        if (direction == 1 && dx < 0) return false;
        if (direction == 3 && dx > 0) return false;
        return Math.abs(dx) < 9 * gp.tileSize;
    }

    private boolean playerInAttackRange() {
        if (witchFeetRow() != playerFeetRow()) return false;
        int dx = gp.player.worldX - worldX;
        if (direction == 1 && dx < 0) return false;
        if (direction == 3 && dx > 0) return false;
        return Math.abs(dx) < 7 * gp.tileSize;
    }

    private boolean lostPlayer() {
        int rowDiff = playerFeetRow() - witchFeetRow();
        if (rowDiff > 0) return true;
        if (Math.abs(gp.player.worldX - worldX) > 12 * gp.tileSize) return true;
        if (rowDiff == 0) { chaseLostFrames = 0; return false; }
        return ++chaseLostFrames > 40;
    }

    private void updatePatrol() {
        if (!edgeSafe(patrolDir)) patrolDir = -patrolDir;
        xspeed = patrolDir * speed;
        direction = patrolDir > 0 ? 1 : 3;
        setAnim(walk, 1e9 / 8.0);
        if (canSpotPlayer()) { chaseLostFrames = 0; state = WitchState.CHASE; }
    }

    private void updateChase() {
        if (lostPlayer()) {
            state = WitchState.PATROL;
            xspeed = 0;
            setAnim(idle, 1e9 / 7.0);
            return;
        }
        int dx = gp.player.worldX - worldX;
        direction = dx > 0 ? 1 : 3;

        if (attackCooldown == 0 && playerInAttackRange()) {
            state = WitchState.ATTACK;
            attackApplied = false;
            attackSoundPlayed = false;
            action = attack;
            spriteNum = 0;
            spriteFrameTime = 1e9 / 12.0;
            delta = 0;
            lastTime = System.nanoTime();
            xspeed = 0;
            return;
        }

        if (!onGround) { xspeed = 0; return; }
        int moveDir = dx > 0 ? 1 : -1;
        if (Math.abs(dx) < 5 * gp.tileSize) {
            // Too close — back away
            int backDir = -moveDir;
            if (edgeSafe(backDir)) {
                xspeed = backDir * speed;
                setAnim(walk, 1e9 / 8.0);
            } else {
                xspeed = 0;
                setAnim(idle, 1e9 / 7.0);
            }
            return;
        }
        if (!edgeSafe(moveDir)) {
            xspeed = 0;
            setAnim(idle, 1e9 / 7.0);
            return;
        }
        xspeed = moveDir * speed;
        setAnim(walk, 1e9 / 8.0);
    }

    private void updateAttack() {
        xspeed = 0;
        if (!attackSoundPlayed && spriteNum >= attack.length / 2) {
            attackSoundPlayed = true;
            Sound.play("/Witch/curse.wav");
        }
        if (spriteNum >= attack.length * 3 / 4 && !attackApplied && playerInAttackRange()) {
            attackApplied = true;
            gp.player.addCurse(this);
        }
        if (spriteNum == attack.length - 1) {
            // Cooldown = curse duration so she can't re-cast while curse is active
            attackCooldown = attackApplied ? 300 : 30;
            state = WitchState.CHASE;
            setAnim(idle, 1e9 / 7.0);
        }
    }

    private void updateSprite() {
        currTime = System.nanoTime();
        delta += (currTime - lastTime) / spriteFrameTime;
        lastTime = currTime;
        if (delta >= 1) {
            delta--;
            if (state == WitchState.DYING) {
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

        if (invincibleFrames > 0 && (invincibleFrames / 4) % 2 == 0) return;

        BufferedImage frame = action[spriteNum];
        if (direction == 1) g2.drawImage(frame, screenX, screenY, tileSize, tileSize, null);
        else                g2.drawImage(flip(frame), screenX, screenY, tileSize, tileSize, null);

        if (drawHitBox) {
            g2.setColor(new Color(180, 0, 255));
            g2.drawRect(screenX + solidArea.x, screenY + solidArea.y, solidArea.width, solidArea.height);
        }
    }
}
