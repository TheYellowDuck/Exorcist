// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
// Required Notice: Copyright (c) 2026 George Zhang — https://github.com/TheYellowDuck

package tile;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;

import main.GamePanel;


 /**
 * The class Tile manager
 */
public class TileManager {

	GamePanel gp;
	public Tile[] tile;

	public HashMap<Integer, int[]> mapRows = new HashMap<>();
	public final int cols = 44;
	public final int bottomRow = 149;

	private int generatedTopRow;
	private int curPlatRow, curLeft, curRight;
	private java.util.Random rand;
	private int distJumpChain = 0;
	public List<int[]> pendingSpawns = new ArrayList<>();
	public List<int[]> pendingGolemSpawns = new ArrayList<>();
	public List<int[]> pendingWitchSpawns = new ArrayList<>();


/**
 *
 * the constructor.
 *
 * @param gp  the gp.
 */
	public TileManager(GamePanel gp) {


		this.gp = gp;

		tile = new Tile[8];

		getTileImage();
		initMap();

	}


/**
 *
 * Returns the tile number at the given row/col.
 * Handles boundary walls, ground, and ungenerated (empty) rows transparently.
 *
 * @param row  tile row (can be negative for infinite upward)
 * @param col  tile column
 * @return 0 = empty, 1 = solid, 2 = pass-through
 */
	public int getTileAt(int row, int col) {

		if (col == 0 || col == cols - 1) return 1;
		if (col < 0 || col >= cols) return 0;
		if (row >= bottomRow) return 1;
		int[] rowData = mapRows.get(row);
		if (rowData == null) return 0;
		return rowData[col];

	}


	private int[] getOrCreateRow(int row) {
		return mapRows.computeIfAbsent(row, k -> new int[cols]);
	}


/**
 *
 * Initialises the map: wide starting platform and first chunk of platforms.
 *
 */
	public void initMap() {

		rand = new java.util.Random();

		int startRow = bottomRow - 2;
		int[] startPlatRow = getOrCreateRow(startRow);
		int mid = cols / 2;
		for (int c = mid - 3; c <= mid + 2; c++) startPlatRow[c] = 2;

		curPlatRow = startRow;
		curLeft    = mid - 3;
		curRight   = mid + 2;
		generatedTopRow = startRow;

		generateUpTo(startRow - 60);

	}


/**
 *
 * Generates platforms upward until curPlatRow reaches targetRow.
 * Safe to call repeatedly — picks up from the last generated platform.
 *
 * @param targetRow  highest (smallest-numbered) row to generate to
 */
	public void generateUpTo(int targetRow) {

		while (curPlatRow > targetRow) {

			// t goes 0→1 over the first 130 rows of climbing, then stays at 1
			float t = Math.min(1.0f, (float)(bottomRow - curPlatRow) / 130f);

			// ~25% chance of a "one-hop" platform in easy/medium zone.
			// Single jump rises ~56 px — enough to clear 1 row (48 px) of gap.
			boolean oneHop = t < 0.50f && rand.nextFloat() < 0.25f;
			// Distance jump: wide horizontal gap, can be chained together.
			boolean distJump = !oneHop && (distJumpChain > 0
					? rand.nextFloat() < 0.70f
					: rand.nextFloat() < 0.20f);
			if (!distJump) distJumpChain = 0;

			int vertGap, width, maxGap, physMaxGap;
			if (oneHop) {
				vertGap    = 1;                  // 48 px — cleared by single jump
				width      = 1 + rand.nextInt(2);// 1–2 tiles
				maxGap     = 1;                  // 0–1 tile horizontal gap
				physMaxGap = 1;                  // single-jump horizontal limit ~1.5 tiles
			} else if (distJump) {
				vertGap    = rand.nextFloat() < 0.5f ? 0 : 1; // 50% purely horizontal, 50% slight rise
				width      = vertGap == 0 ? 1 + rand.nextInt(2) : 2 + rand.nextInt(2); // horizontal: 1–2, rise: 2–3
				maxGap     = 3 + rand.nextInt(2);              // 3–4 tile horizontal gap
				physMaxGap = 5;
				if (distJumpChain == 0) distJumpChain = 2 + rand.nextInt(3); // start chain of 2–4
				else distJumpChain--;
			} else if (t < 0.25f) {
				vertGap    = 2;
				width      = 2 + rand.nextInt(2);// 2–3 tiles
				maxGap     = 2;                  // 0–2 tile edge gap
				physMaxGap = 4;
			} else if (t < 0.50f) {
				vertGap    = 2;
				width      = 1 + rand.nextInt(2);// 1–2 tiles
				maxGap     = 3;                  // 0–3 tile edge gap
				physMaxGap = 4;
			} else if (t < 0.75f) {
				vertGap    = 2;
				width      = 1 + rand.nextInt(2);// 1–2 tiles
				maxGap     = 3;
				physMaxGap = 4;
			} else {
				vertGap    = 2;
				width      = 1;                  // 1 tile
				maxGap     = 4;                  // up to 4 (physics limit ~3.8 tiles)
				physMaxGap = 4;
			}

			int nextRow = curPlatRow - vertGap;

			// Bias strongly toward the opposite side of the map so platforms
			// spread across the full width rather than clustering near the start.
			int curCenter = (curLeft + curRight) / 2;
			boolean goRight = rand.nextFloat() < (curCenter <= cols / 2 ? 0.72f : 0.28f);

			int gap = rand.nextInt(maxGap + 1);  // actual edge gap this step
			int newLeft = goRight
					? curRight + 1 + gap
					: curLeft  - 1 - gap - width;

			// Physics hard clamp based on jump type
			int physStart = Math.max(1,           curLeft  - physMaxGap - width);
			int physEnd   = Math.min(cols - 2 - width, curRight + physMaxGap);
			newLeft = Math.max(physStart, Math.min(physEnd, newLeft));

			boolean spawnWolf  = rand.nextFloat() < 0.12f;
			boolean spawnGolem = rand.nextFloat() < 0.05f;
			boolean spawnWitch = rand.nextFloat() < 0.03f;
			if (spawnGolem) width = Math.max(width, 7 + rand.nextInt(2));
			if (spawnWitch) width = Math.max(width, 5 + rand.nextInt(2));
			if (spawnWolf)  width = Math.max(width, 3);

			int newRight = Math.min(cols - 2, newLeft + width - 1);
			int[] row = getOrCreateRow(nextRow);
			for (int c = newLeft; c <= newRight; c++) row[c] = 2;

			curPlatRow = nextRow;
			curLeft    = newLeft;
			curRight   = newRight;
			if (nextRow < generatedTopRow) generatedTopRow = nextRow;

			if (spawnGolem) pendingGolemSpawns.add(new int[]{newLeft * gp.tileSize, (nextRow - 1) * gp.tileSize});
			if (spawnWitch) pendingWitchSpawns.add(new int[]{newLeft * gp.tileSize, (nextRow - 1) * gp.tileSize});
			if (spawnWolf)  pendingSpawns.add(new int[]{newLeft * gp.tileSize, (nextRow - 1) * gp.tileSize});
		}

	}


/**
 *
 * Called every game frame; triggers more generation when the player
 * climbs within 30 rows of the highest generated platform.
 *
 */
	public void update() {

		int playerRow = gp.player.worldY / gp.tileSize;
		if (playerRow < generatedTopRow + 30) {
			generateUpTo(generatedTopRow - 60);
		}

	}


/**
 *
 * Gets the tile images
 *
 */
	public void getTileImage() {


		try {

			tile[0] = new Tile();
			tile[0].image = ImageIO.read(getClass().getResourceAsStream("/Tiles/Blank.png"));

			tile[1] = new Tile();
			tile[1].image = darken(ImageIO.read(getClass().getResourceAsStream("/Tiles/TileSet.png")).getSubimage(0, 0, 48, 48), 0.5f);
			tile[1].collision = true;
			tile[1].solidArea = new Rectangle(0, 0, 48, 48);

			tile[2] = new Tile();
			tile[2].image = darken(ImageIO.read(getClass().getResourceAsStream("/Tiles/TileSet.png")).getSubimage(0, 48, 48, 16), 0.5f);
			tile[2].collision = true;
			tile[2].passThrough = true;
			tile[2].solidArea = new Rectangle(0, 0, 48, 16);

			tile[4] = new Tile();
			tile[4].image = ImageIO.read(getClass().getResourceAsStream("/Tiles/TileSet.png")).getSubimage(48, 32, 16, 16);

			tile[5] = new Tile();
			tile[5].image = darken(ImageIO.read(getClass().getResourceAsStream("/Tiles/TileSet.png")).getSubimage(0, 0, 48, 48), 0.75f);

			tile[6] = new Tile();
			tile[6].image = darken(ImageIO.read(getClass().getResourceAsStream("/Tiles/TileSet.png")).getSubimage(0, 48, 48, 16), 0.75f);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}


/**
 *
 * Draw
 *
 * @param g2  the g2.
 */
	public void draw(Graphics2D g2) {


		int startCol = (gp.player.worldX - gp.player.screenX) / gp.tileSize;
		int startRow = (gp.player.worldY - gp.player.screenY) / gp.tileSize;
		int endCol   = (int) Math.ceil((gp.player.worldX + (gp.screenWidth  - gp.player.screenX) + 0.0) / gp.tileSize);
		int endRow   = (int) Math.ceil((gp.player.worldY + (gp.screenHeight - gp.player.screenY) + 0.0) / gp.tileSize);

		for (int worldRow = startRow; worldRow <= endRow; worldRow++) {
			for (int worldCol = startCol; worldCol <= endCol; worldCol++) {

				int tileNum = getTileAt(worldRow, worldCol);
				if (tileNum == 0 || tile[tileNum] == null) continue;

				int worldXpx = worldCol * gp.tileSize;
				int worldYpx = worldRow * gp.tileSize;
				int screenX  = worldXpx - gp.player.worldX + gp.player.screenX;
				int screenY  = worldYpx - gp.player.worldY + gp.player.screenY;

				g2.drawImage(tile[tileNum].image, screenX, screenY,
						tile[tileNum].image.getWidth(), tile[tileNum].image.getHeight(), null);
			}
		}

	}


/**
 *
 * Darken
 *
 * @param img  the img.
 * @param alpha  the alpha.
 * @return BufferedImage
 */
	public BufferedImage darken(BufferedImage img, float alpha) {


		Graphics2D g2 = img.createGraphics();

    	g2.setColor(Color.black);
    	g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

    	g2.fillRect(0, 0, img.getWidth(), img.getHeight());

    	g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

    	return img;
	}

}
