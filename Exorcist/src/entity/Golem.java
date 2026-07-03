// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
// Required Notice: Copyright (c) 2026 George Zhang — https://github.com/TheYellowDuck

package entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import main.GamePanel;

public class Golem extends Entity {

    GamePanel gp;

    public int hp = 12;
    public boolean dead = false;

    float xspeed = 0;
    float yspeed = 0;
    boolean onGround = false;

    private enum GolemState { PATROL, CHASE, ATTACK, STUNNED, DYING }
    private GolemState state = GolemState.PATROL;

    private int patrolDir = 1;
    private int attackCooldown = 0;
    public int invincibleFrames = 0;
    private int chaseLostFrames = 0;
    private int stunTimer = 0;
    private int attackLungeDir = 0;
    private int springBackTimer = 0;
    private int attackStartX = 0;
    private boolean attackHitDealt = false;

    public Golem(GamePanel gp, int worldX, int worldY) {
        this.gp = gp;
        this.worldX = worldX;
        this.worldY = worldY;
        this.speed = 2;
        this.direction = 1;
        this.solidArea = new Rectangle(6, 4, 36, 40);
        drawHitBox = false;
        loadImages();
        setAnim(idle, 1e9 / 6.0);
    }

    private void loadImages() {
        try {
            idle   = getSprites(ImageIO.read(getClass().getResourceAsStream("/Golem/noBKG_GolemIdle_strip.png")),   12, 64, 0);
            walk   = getSprites(ImageIO.read(getClass().getResourceAsStream("/Golem/noBKG_GolemWalk_strip.png")),    7, 64, 0);
            attack = getSprites(ImageIO.read(getClass().getResourceAsStream("/Golem/noBKG_GolemAttack_strip.png")), 16, 80, 0);
            death  = getSprites(ImageIO.read(getClass().getResourceAsStream("/Golem/noBKG_GolemDeath_strip.png")),  28, 64, 0);
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
        if (invincibleFrames > 0 || state == GolemState.DYING) return;
        hp -= dmg;
        invincibleFrames = 20;
        if (hp <= 0) {
            state = GolemState.DYING;
            xspeed = 0;
            action = death;
            spriteNum = 0;
            spriteFrameTime = 1e9 / 8.0;
            delta = 0;
            lastTime = System.nanoTime();
            int prevHp = gp.player.hp;
            gp.player.hp = Math.min(Player.MAX_OVERFLOW_HP, gp.player.hp + 4);
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
            if (state == GolemState.PATROL) patrolDir = -patrolDir;
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

    private int golemFeetRow()  { return (worldY + solidArea.y + solidArea.height) / gp.tileSize; }
    private int playerFeetRow() { return (gp.player.worldY + gp.player.solidArea.y + gp.player.solidArea.height) / gp.tileSize; }

    private boolean canSpotPlayer() {
        if (golemFeetRow() != playerFeetRow()) return false;
        int dx = gp.player.worldX - worldX;
        if (direction == 1 && dx < 0) return false;
        if (direction == 3 && dx > 0) return false;
        return Math.abs(dx) < 9 * gp.tileSize;
    }

    private boolean lostPlayer() {
        int rowDiff = playerFeetRow() - golemFeetRow();
        if (rowDiff > 0) return true;
        if (Math.abs(gp.player.worldX - worldX) > 12 * gp.tileSize) return true;
        if (rowDiff == 0) { chaseLostFrames = 0; return false; }
        return ++chaseLostFrames > 40;
    }

    private Rectangle golemSolidRect() {
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
        setAnim(walk, 1e9 / 8.0);
        if (canSpotPlayer()) { chaseLostFrames = 0; state = GolemState.CHASE; }
    }

    private void updateChase() {
        if (springBackTimer > 0) {
            int target = attackStartX - attackLungeDir * (gp.tileSize / 2);
            int diff = target - worldX;
            if (Math.abs(diff) < speed) { worldX = target; xspeed = 0; springBackTimer = 0; }
            else { xspeed = diff > 0 ? speed : -speed; springBackTimer--; }
            direction = attackLungeDir > 0 ? 1 : 3;
            setAnim(idle, 1e9 / 6.0);
            return;
        }
        if (lostPlayer()) {
            state = GolemState.PATROL;
            xspeed = 0;
            setAnim(idle, 1e9 / 6.0);
            return;
        }
        if (golemSolidRect().intersects(playerSolidRect())) {
            if (attackCooldown == 0) {
                xspeed = 0;
                attackStartX = worldX;
                attackLungeDir = gp.player.worldX >= worldX ? 1 : -1;
                state = GolemState.ATTACK;
                attackHitDealt = false;
                action = attack;
                spriteNum = 0;
                spriteFrameTime = 1e9 / 10.0;
                delta = 0;
                lastTime = System.nanoTime();
            } else {
                xspeed = 0;
                setAnim(idle, 1e9 / 6.0);
            }
            return;
        }
        if (!onGround) { xspeed = 0; return; }
        int dx = gp.player.worldX - worldX;
        int moveDir = dx > 0 ? 1 : -1;
        if (!edgeSafe(moveDir)) {
            xspeed = 0;
            setAnim(idle, 1e9 / 6.0);
            return;
        }
        xspeed = moveDir * speed;
        direction = moveDir > 0 ? 1 : 3;
        setAnim(walk, 1e9 / 8.0);
    }

    private void updateAttack() {
        xspeed = 0;
        direction = attackLungeDir > 0 ? 1 : 3;
        if (spriteNum >= attack.length * 3 / 4 && !attackHitDealt && golemSolidRect().intersects(playerSolidRect())) {
            attackHitDealt = true;
            boolean blocked = gp.player.takeDamage(4, worldX);
            if (blocked) {
                springBackTimer = 12;
                stunTimer = 60;
                state = GolemState.STUNNED;
                setAnim(idle, 1e9 / 6.0);
                return;
            }
        }
        if (spriteNum == attack.length - 1) {
            springBackTimer = 0;
            attackCooldown = 42;
            state = GolemState.CHASE;
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
            state = GolemState.PATROL;
            chaseLostFrames = 0;
        }
    }

    private void updateSprite() {
        currTime = System.nanoTime();
        delta += (currTime - lastTime) / spriteFrameTime;
        lastTime = currTime;
        if (delta >= 1) {
            delta--;
            if (state == GolemState.DYING) {
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
            g2.setColor(Color.blue);
            g2.drawRect(screenX + solidArea.x, screenY + solidArea.y, solidArea.width, solidArea.height);
        }
    }
}
