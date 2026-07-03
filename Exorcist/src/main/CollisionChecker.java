// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
// Required Notice: Copyright (c) 2026 George Zhang — https://github.com/TheYellowDuck

package main;

import java.awt.Rectangle;

import entity.Entity;
import tile.Tile;


 /**
 * The class Collision checker
 */ 
public class CollisionChecker {
	
	GamePanel gp;
	

/** 
 *
 * constructor. 
 *
 * @param gp  the gp. 
 */
	public CollisionChecker(GamePanel gp) { 

		
		this.gp = gp;
		
	}
	
	int newPos[][];
	

/** 
 *
 * Check tile collisions
 *
 * @param entity  the entity. 
 * @param xspeed  the xspeed. 
 * @param yspeed  the yspeed. 
 * @return boolean
 */
	public boolean checkTile(Entity entity, float xspeed, float yspeed) { 

		
		newPos = new int[4][4];
		// Safety net: stale values from a prior frame must not teleport the entity
		// if a collision flag is set without an accompanying newWorldX/Y update.
		entity.newWorldX = entity.worldX;
		entity.newWorldY = entity.worldY;

		Tile[] tiles = new Tile[4];
		int[][] tilePos = new int[4][2];
		boolean[] tileCollision = new boolean[4];
		
		int entityLeftWorldX   = entity.worldX + entity.solidArea.x + (int) xspeed;
		int entityRightWorldX  = entity.worldX + entity.solidArea.x + entity.solidArea.width - 1 + (int) xspeed;
		int entityTopWorldY    = entity.worldY + entity.solidArea.y - (int) yspeed;
		int entityBottomWorldY = entity.worldY + entity.solidArea.y + entity.solidArea.height - 1 - (int) yspeed;

		int entityLeftCol   = entityLeftWorldX   / gp.tileSize;
		int entityRightCol  = entityRightWorldX  / gp.tileSize;
		int entityTopRow    = entityTopWorldY    / gp.tileSize;
		int entityBottomRow = entityBottomWorldY / gp.tileSize;

		tiles[0] = gp.tileM.tile[gp.tileM.getTileAt(entityTopRow,    entityLeftCol)];
		tilePos[0][0] = entityLeftCol;  tilePos[0][1] = entityTopRow;

		tiles[1] = gp.tileM.tile[gp.tileM.getTileAt(entityTopRow,    entityRightCol)];
		tilePos[1][0] = entityRightCol; tilePos[1][1] = entityTopRow;

		tiles[2] = gp.tileM.tile[gp.tileM.getTileAt(entityBottomRow, entityLeftCol)];
		tilePos[2][0] = entityLeftCol;  tilePos[2][1] = entityBottomRow;

		tiles[3] = gp.tileM.tile[gp.tileM.getTileAt(entityBottomRow, entityRightCol)];
		tilePos[3][0] = entityRightCol; tilePos[3][1] = entityBottomRow;
		
//		Tile[] tiles = {gp.tileM.tile[upLeftTile], gp.tileM.tile[upRightTile], gp.tileM.tile[bottomLeftTile], gp.tileM.tile[bottomRightTile]};
		
		int count = 0;
		for (int i = 0; i < 4; i ++) {
			if (tiles[i] == null) continue;
			tileCollision[i] = checkIntersect(entity, entityLeftWorldX, entityTopWorldY, tiles[i], tilePos[i][0], tilePos[i][1], i, yspeed);
			if (tileCollision[i]) count ++;
		}
		
		if (count == 0) return false;
		
		if (count >= 3) {
			entity.collisionOnX = true;
			entity.collisionOnY = true;
			// Guard each newPos read so an unset (zero) slot can't corrupt the result
			if (xspeed <= 0) {
				int rx = 0;
				if (tileCollision[0]) rx = Math.max(rx, newPos[0][2]);
				if (tileCollision[2]) rx = Math.max(rx, newPos[2][2]);
				entity.newWorldX = rx - entity.solidArea.x;
			} else {
				int rx = Integer.MAX_VALUE;
				if (tileCollision[1]) rx = Math.min(rx, newPos[1][0]);
				if (tileCollision[3]) rx = Math.min(rx, newPos[3][0]);
				if (rx < Integer.MAX_VALUE)
					entity.newWorldX = rx - entity.solidArea.x - entity.solidArea.width;
			}
			if (yspeed <= 0) {
				int ry = 0;
				if (tileCollision[2]) ry = Math.max(ry, newPos[2][1]);
				if (tileCollision[3]) ry = Math.max(ry, newPos[3][1]);
				entity.newWorldY = ry - entity.solidArea.y - entity.solidArea.height;
			} else {
				int ry = Integer.MAX_VALUE;
				if (tileCollision[0]) ry = Math.min(ry, newPos[0][3]);
				if (tileCollision[1]) ry = Math.min(ry, newPos[1][3]);
				if (ry < Integer.MAX_VALUE)
					entity.newWorldY = ry - entity.solidArea.y;
			}
			return tileCollision[2] && tileCollision[3];
		}
		
		if (count == 2) {
			if (tileCollision[0] && tileCollision[1]) {
				entity.collisionOnY = true;
				entity.newWorldY = Math.min(newPos[0][3], newPos[1][3]) - entity.solidArea.y;
				return false;
			}
			if (tileCollision[2] && tileCollision[3]) {
				entity.collisionOnY = true;
				entity.newWorldY = Math.max(newPos[2][1], newPos[3][1]) - entity.solidArea.y - entity.solidArea.height;
				return true;
			}
			if ((tileCollision[0] && tileCollision[2]) || (tileCollision[1] && tileCollision[3])) {
				entity.collisionOnX = true;
				if (xspeed <= 0) {
					entity.newWorldX = Math.max(newPos[0][2], newPos[2][2]) - entity.solidArea.x;
				}
				else {
					entity.newWorldX = Math.min(newPos[1][0], newPos[3][0]) - entity.solidArea.x - entity.solidArea.width;
				}
				return false;
			}
			if (tileCollision[0] && tileCollision[3]) {
				entity.collisionOnX = true;
				entity.collisionOnY = true;
				if (xspeed <= 0) {
					entity.newWorldX = newPos[0][2] - entity.solidArea.x;
				}
				else {
					entity.newWorldX = newPos[3][0] - entity.solidArea.x - entity.solidArea.width;
				}
				if (yspeed <= 0) {
					entity.newWorldY = newPos[3][1] - entity.solidArea.y - entity.solidArea.height;
				}
				else {
					entity.newWorldY = newPos[0][3] - entity.solidArea.y;
				}
				return yspeed < 0;
			}
			if (tileCollision[1] && tileCollision[2]) {
				entity.collisionOnX = true;
				entity.collisionOnY = true;
				if (xspeed <= 0) {
					entity.newWorldX = newPos[2][2] - entity.solidArea.x;
				}
				else {
					entity.newWorldX = newPos[1][0] - entity.solidArea.x - entity.solidArea.width;
				}
				if (yspeed <= 0) {
					entity.newWorldY = newPos[2][1] - entity.solidArea.y - entity.solidArea.height;
				}
				else {
					entity.newWorldY = newPos[1][3] - entity.solidArea.y;
				}
				return yspeed < 0;
			}
		}
		
		if (yspeed != 0 && xspeed == 0) {
			if (yspeed <= 0) {
				if (tileCollision[2]) {
					entity.collisionOnY = true;
					entity.newWorldY = newPos[2][1] - entity.solidArea.y - entity.solidArea.height;
				}
				if (tileCollision[3]) {
					entity.collisionOnY = true;
					entity.newWorldY = newPos[3][1] - entity.solidArea.y - entity.solidArea.height;
				}
			} else {
				if (tileCollision[0]) {
					entity.collisionOnY = true;
					entity.newWorldY = newPos[0][3] - entity.solidArea.y;
				}
				if (tileCollision[1]) {
					entity.collisionOnY = true;
					entity.newWorldY = newPos[1][3] - entity.solidArea.y;
				}
			}
			return yspeed < 0;
		}
		if (yspeed == 0 && xspeed != 0) {
			if (xspeed <= 0) {
				if (tileCollision[0]) {
					entity.collisionOnX = true;
					entity.newWorldX = newPos[0][2] - entity.solidArea.x;
				}
				if (tileCollision[2]) {
					entity.collisionOnX = true;
					entity.newWorldX = newPos[2][2] - entity.solidArea.x;
				}
			} else {
				if (tileCollision[1]) {
					entity.collisionOnX = true;
					entity.newWorldX = newPos[1][0] - entity.solidArea.x - entity.solidArea.width;
				}
				if (tileCollision[3]) {
					entity.collisionOnX = true;
					entity.newWorldX = newPos[3][0] - entity.solidArea.x - entity.solidArea.width;
				}
			}
			return yspeed < 0;
		}
		
		int prevEntityLeftWorldX = entity.worldX + entity.solidArea.x;
		int prevEntityRightWorldX = entity.worldX + entity.solidArea.x + entity.solidArea.width - 1;
		int prevEntityTopWorldY = entity.worldY + entity.solidArea.y;
		int prevEntityBottomWorldY = entity.worldY + entity.solidArea.y + entity.solidArea.height - 1;
		
		double xTime = 0;
		double yTime = 0;
		
		if (tileCollision[0]) {
			xTime = Math.abs((prevEntityLeftWorldX - ((entityLeftCol + 1) * gp.tileSize - 1) + 0.0) / xspeed);
			yTime = Math.abs((prevEntityTopWorldY - ((entityTopRow + 1) * gp.tileSize - 1) + 0.0) / yspeed);
		}
		if (tileCollision[1]) {
			xTime = Math.abs((prevEntityRightWorldX - (entityRightCol * gp.tileSize) + 0.0) / xspeed);
			yTime = Math.abs((prevEntityTopWorldY - ((entityTopRow + 1) * gp.tileSize - 1) + 0.0) / yspeed);
		}
		if (tileCollision[2]) {
			xTime = Math.abs((prevEntityLeftWorldX - ((entityLeftCol + 1) * gp.tileSize - 1) + 0.0) / xspeed);
			yTime = Math.abs((prevEntityBottomWorldY - (entityBottomRow * gp.tileSize) + 0.0) / yspeed);
		}
		if (tileCollision[3]) {
			xTime = Math.abs((prevEntityRightWorldX - (entityRightCol * gp.tileSize) + 0.0) / xspeed);
			yTime = Math.abs((prevEntityBottomWorldY - (entityBottomRow * gp.tileSize) + 0.0) / yspeed);
		}
		
		if (xTime == yTime) {
			entity.collisionOnX = true;
			entity.collisionOnY = true;
			if (xspeed <= 0) {
				if (tileCollision[0])
					entity.newWorldX = newPos[0][2] - entity.solidArea.x;
				if (tileCollision[2])
					entity.newWorldX = newPos[2][2] - entity.solidArea.x;
			}
			else {
				if (tileCollision[1])
					entity.newWorldX = newPos[1][0] - entity.solidArea.x - entity.solidArea.width;
				if (tileCollision[3])
					entity.newWorldX = newPos[3][0] - entity.solidArea.x - entity.solidArea.width;
			}
			if (yspeed <= 0) {
				if (tileCollision[2])
					entity.newWorldY = newPos[2][1] - entity.solidArea.y - entity.solidArea.height;
				if (tileCollision[3])
					entity.newWorldY = newPos[3][1] - entity.solidArea.y - entity.solidArea.height;
			}
			else {
				if (tileCollision[0])
					entity.newWorldY = newPos[0][3] - entity.solidArea.y;
				if (tileCollision[1])
					entity.newWorldY = newPos[1][3] - entity.solidArea.y;
			}
			return yspeed < 0;
		}
		if (xTime < yTime) {
			entity.collisionOnX = true;
			if (xspeed <= 0) {
				if (tileCollision[0])
					entity.newWorldX = newPos[0][2] - entity.solidArea.x;
				if (tileCollision[2])
					entity.newWorldX = newPos[2][2] - entity.solidArea.x;
			}
			else {
				if (tileCollision[1])
					entity.newWorldX = newPos[1][0] - entity.solidArea.x - entity.solidArea.width;
				if (tileCollision[3])
					entity.newWorldX = newPos[3][0] - entity.solidArea.x - entity.solidArea.width;
			}
			if (yspeed <= 0) {
				if (tileCollision[2])
					entity.newWorldY = newPos[2][1] - entity.solidArea.y - entity.solidArea.height;
				if (tileCollision[3])
					entity.newWorldY = newPos[3][1] - entity.solidArea.y - entity.solidArea.height;
			}
			else {
				if (tileCollision[0])
					entity.newWorldY = newPos[0][3] - entity.solidArea.y;
				if (tileCollision[1])
					entity.newWorldY = newPos[1][3] - entity.solidArea.y;
			}
			return false;
		}
		if (xTime > yTime) {
			entity.collisionOnY = true;
			if (xspeed <= 0) {
				if (tileCollision[0])
					entity.newWorldX = newPos[0][2] - entity.solidArea.x;
				if (tileCollision[2])
					entity.newWorldX = newPos[2][2] - entity.solidArea.x;
			}
			else {
				if (tileCollision[1])
					entity.newWorldX = newPos[1][0] - entity.solidArea.x - entity.solidArea.width;
				if (tileCollision[3])
					entity.newWorldX = newPos[3][0] - entity.solidArea.x - entity.solidArea.width;
			}
			if (yspeed <= 0) {
				if (tileCollision[2])
					entity.newWorldY = newPos[2][1] - entity.solidArea.y - entity.solidArea.height;
				if (tileCollision[3])
					entity.newWorldY = newPos[3][1] - entity.solidArea.y - entity.solidArea.height;
			}
			else {
				if (tileCollision[0])
					entity.newWorldY = newPos[0][3] - entity.solidArea.y;
				if (tileCollision[1])
					entity.newWorldY = newPos[1][3] - entity.solidArea.y;
			}
			return yspeed < 0;
		}
		
		return false;
	}
	

/** 
 *
 * Check intersect
 *
 * @param entity  the entity. 
 * @param xPos  the x pos. 
 * @param yPos  the y pos. 
 * @param tile  the tile. 
 * @param xPosTile  the x pos tile. 
 * @param yPosTile  the y pos tile. 
 * @param i  the index. 
 * @return boolean
 */
	public boolean checkIntersect(Entity entity, int xPos, int yPos, Tile tile, int xPosTile, int yPosTile, int i, float yspeed) {


		if (!tile.collision) return false;
		if (tile.passThrough) {
			// Always pass through while moving up
			if (yspeed > 0) return false;
			// Also pass through while the player's body is still below the platform top
			// (handles yspeed==0 at jump peak when partially inside the platform)
			int currentBottom = entity.worldY + entity.solidArea.y + entity.solidArea.height - 1;
			int platformTop   = yPosTile * gp.tileSize + tile.solidArea.y;
			if (currentBottom > platformTop) return false;
		}
		
		newPos[i][0] = xPosTile * gp.tileSize + tile.solidArea.x;
		newPos[i][1] = yPosTile * gp.tileSize + tile.solidArea.y;
		newPos[i][2] = xPosTile * gp.tileSize + tile.solidArea.x + tile.solidArea.width;
		newPos[i][3] = yPosTile * gp.tileSize + tile.solidArea.y + tile.solidArea.height;
		
		return new Rectangle(xPos, yPos, entity.solidArea.width, entity.solidArea.height)
				.intersects(xPosTile * gp.tileSize + tile.solidArea.x,
						yPosTile * gp.tileSize + tile.solidArea.y,
						tile.solidArea.width, tile.solidArea.height);
		
	}

}
