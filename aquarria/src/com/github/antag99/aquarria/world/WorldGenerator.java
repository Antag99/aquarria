/*******************************************************************************
 * Copyright (c) 2014-2015, Anton Gustafsson
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * 
 * * Neither the name of Aquarria nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package com.github.antag99.aquarria.world;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.utils.Array;
import com.github.antag99.aquarria.Direction;
import com.github.antag99.aquarria.GameRegistry;
import com.github.antag99.aquarria.entity.PlayerEntity;
import com.github.antag99.aquarria.tile.TileType;
import com.github.antag99.aquarria.tile.WallType;
import com.sudoplay.joise.module.Module;

public class WorldGenerator {
	private World world;
	private long seed;
	private Array<WorldGeneratorTask> tasks = new Array<>();

	public WorldGenerator(World world, long seed) {
		this.world = world;
		this.seed = seed;

		addTasks();
	}

	void addTasks() {
		// Clear the world
		tasks.add((generator, seed) -> {
			generator.getWorld().clear();
		});

		// Generate base terrain
		tasks.add(new TerrainGeneratorTask(GameRegistry.getTileType("stone")));

		// Put dirt in the rocks... (This also leaves some rocks that appear to have been put in the dirt)
		tasks.add(new DirtGeneratorTask(GameRegistry.getTileType("stone"), GameRegistry.getTileType("dirt")));

		// Place dirt walls close to the surface
		tasks.add(new SurfaceWallGeneratorTask(GameRegistry.getWallType("dirt")));

		// Grassify the surface
		tasks.add(new SurfaceTileGeneratorTask(GameRegistry.getTileType("dirt"), GameRegistry.getTileType("grass")));

		// Place some trees...
		// tasks.add((generator, seed) -> {
		// Random treeRandom = new Random(seed);
		//
		// for (int i = 5; i < generator.getWidth() - 5; i += 10) {
		// placeTree(i, generator.getSurfaceLevel(i), treeRandom);
		// }
		// });

		// Set the spawnpoint
		tasks.add((generator, seed) -> {
			generator.setSpawnX(generator.getWidth() / 2);
			generator.setSpawnY(Math.max(generator.getSurfaceLevel((int) generator.getSpawnX()),
					generator.getSurfaceLevel((int) generator.getSpawnX() + 1)));
		});
	}

	public World getWorld() {
		return world;
	}

	public long getSeed() {
		return seed;
	}

	public void generate() {
		for (WorldGeneratorTask task : tasks) {
			task.generate(this, getSeed());
		}

		// Pixmap worldPixmap = worldToPixmap(world);
		// PixmapIO.writePNG(Gdx.files.local("debug/world.png"), worldPixmap);
		// worldPixmap.dispose();
	}

	// public void placeTree(int x, int y, Random random) {
	// // Place stubs
	// if (world.getTileType(x - 1, y) == TileType.air &&
	// world.getTileType(x - 1, y - 1).isSolid() &&
	// random.nextBoolean()) {
	// world.setTileType(x - 1, y, TileType.tree);
	// world.setAttached(x - 1, y, Direction.EAST, true);
	// world.setAttached(x - 1, y, Direction.SOUTH, true);
	// }
	// if (world.getTileType(x + 1, y) == TileType.air &&
	// world.getTileType(x + 1, y - 1).isSolid() &&
	// random.nextBoolean()) {
	// world.setTileType(x + 1, y, TileType.tree);
	// world.setAttached(x + 1, y, Direction.WEST, true);
	// world.setAttached(x + 1, y, Direction.SOUTH, true);
	// }
	//
	// // Place trunks
	// int treeHeight = random.nextInt(7) + 8;
	//
	// for (int i = 0; i < treeHeight; ++i) {
	// world.setTileType(x, y + i, TileType.tree);
	// world.setAttached(x, y + i, Direction.SOUTH, true);
	// }
	//
	// // Place branches
	// // How many tiles away the next branch will be placed
	// int branchCounter = 2 + random.nextInt(3);
	// // Direction in which the next branch won't be placed
	// int branchSkip = 0;
	//
	// for (int i = 0; i + 1 < treeHeight; ++i) {
	// if (--branchCounter == 0) {
	// int branchDir = -branchSkip;
	// if (branchDir == 0) {
	// branchDir = random.nextBoolean() ? 1 : -1;
	// }
	//
	// world.setTileType(x + branchDir, y + i, TileType.tree);
	// world.setAttached(x + branchDir, y + i, Direction.get(-branchDir, 0), true);
	//
	// branchCounter = 1 + random.nextInt(3);
	// branchSkip = branchCounter == 1 ? branchDir : 0;
	// }
	// }
	// }

	static Pixmap moduleToPixmap(Module module, int width, int height, float xFrequency, float yFrequency) {
		Pixmap result = new Pixmap(width, height, Format.RGBA8888);
		for (int i = 0; i < width; ++i) {
			for (int j = 0; j < height; ++j) {
				float value = (float) module.get(i * xFrequency, j * yFrequency);
				result.setColor(value, value, value, 1f);
				result.drawPixel(i, j);
			}
		}
		return result;
	}

	// static Pixmap worldToPixmap(World world) {
	// Pixmap result = new Pixmap(world.getWidth(), world.getHeight(), Format.RGBA8888);
	//
	// for (int i = 0; i < world.getWidth(); ++i) {
	// for (int j = 0; j < world.getHeight(); ++j) {
	// Color color;
	// TileType type = world.getTileType(i, j);
	//
	// if (type == TileType.air) {
	// color = Color.CLEAR;
	// } else if (type == TileType.dirt) {
	// color = Color.MAROON;
	// } else if (type == TileType.grass) {
	// color = Color.GREEN;
	// } else if (type == TileType.stone) {
	// color = Color.GRAY;
	// } else {
	// color = Color.PINK;
	// }
	//
	// result.drawPixel(i, world.getHeight() - j + 1, Color.rgba8888(color));
	// }
	// }
	//
	// return result;
	// }

	public float getSpawnX() {
		return world.getSpawnX();
	}

	public float getSpawnY() {
		return world.getSpawnY();
	}

	public void setSpawnX(float spawnX) {
		world.setSpawnX(spawnX);
	}

	public void setSpawnY(float spawnY) {
		world.setSpawnY(spawnY);
	}

	public void checkBounds(int x, int y) {
		world.checkBounds(x, y);
	}

	public boolean inBounds(int x, int y) {
		return world.inBounds(x, y);
	}

	public TileType getTileType(int x, int y) {
		return world.getTileType(x, y);
	}

	public void setTileType(int x, int y, TileType type) {
		world.setTileType(x, y, type);
	}

	public WallType getWallType(int x, int y) {
		return world.getWallType(x, y);
	}

	public void setWallType(int x, int y, WallType type) {
		world.setWallType(x, y, type);
	}

	public int getWidth() {
		return world.getWidth();
	}

	public int getHeight() {
		return world.getHeight();
	}

	public int getSurfaceLevel(int x) {
		return world.getSurfaceLevel(x);
	}

	public void setSurfaceLevel(int x, int level) {
		world.setSurfaceLevel(x, level);
	}

	public int getLiquid(int x, int y) {
		return world.getLiquid(x, y);
	}

	public void setLiquid(int x, int y, int liquid) {
		world.setLiquid(x, y, liquid);
	}

	public boolean isAttached(int x, int y, Direction direction) {
		return world.isAttached(x, y, direction);
	}

	public void setAttached(int x, int y, Direction direction, boolean attached) {
		world.setAttached(x, y, direction, attached);
	}

	public void checkAttachment(int x, int y, PlayerEntity player) {
		world.checkAttachment(x, y, player);
	}

	public void clearAttachment(int x, int y) {
		world.clearAttachment(x, y);
	}
}
