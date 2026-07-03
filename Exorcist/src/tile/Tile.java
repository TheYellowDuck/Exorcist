// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
// Required Notice: Copyright (c) 2026 George Zhang — https://github.com/TheYellowDuck

package tile;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;


 /**
 * The class Tile
 */ 
public class Tile {
	
	public BufferedImage image;
	public boolean collision = false;
	public boolean passThrough = false;
	public Rectangle solidArea;
//	public int width, height;

}
