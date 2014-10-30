package com.github.antag99.aquarria.world;

import static com.badlogic.gdx.math.MathUtils.ceil;
import static com.badlogic.gdx.math.MathUtils.clamp;
import static com.badlogic.gdx.math.MathUtils.floor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.github.antag99.aquarria.entity.Entity;
import com.github.antag99.aquarria.tile.TileType;

public class WorldRenderer extends Widget {

	private WorldView view;
	private boolean drawTileGrid = false;
	private boolean drawEntityBoxes = false;
	private ShapeRenderer shapeRenderer = null;

	public WorldRenderer() {
	}

	public void setDrawEntityBoxes(boolean drawEntityBoxes) {
		this.drawEntityBoxes = drawEntityBoxes;
	}

	public void setDrawTileGrid(boolean drawTileGrid) {
		this.drawTileGrid = drawTileGrid;
	}

	public boolean getDrawEntityBoxes() {
		return drawEntityBoxes;
	}

	public boolean getDrawTileGrid() {
		return drawTileGrid;
	}

	@Override
	public void draw(Batch batch, float parentAlpha) {
		super.draw(batch, parentAlpha);

		OrthographicCamera cam = view.getCamera();
		batch.setProjectionMatrix(cam.combined);
		World world = view.getWorld();

		float width = cam.viewportWidth * cam.zoom;
		float height = cam.viewportHeight * cam.zoom;

		float margin = Math.max(width, height);

		int startX = clamp(floor(cam.position.x - margin), 0, world.getWidth() - 1);
		int startY = clamp(floor(cam.position.y - margin), 0, world.getHeight() - 1);

		int endX = clamp(ceil(cam.position.x + margin), 0, world.getWidth());
		int endY = clamp(ceil(cam.position.y + margin), 0, world.getHeight());

		for (int i = startX; i < endX; ++i) {
			for (int j = startY; j < endY; ++j) {
				TileType type = world.getTileType(i, j);
				TextureRegion texture = type.getTexture();

				if (texture != null) {
					batch.draw(texture, i, j, 1f, 1f);
				}
			}
		}

		for (Entity entity : world.getEntities()) {
			entity.getView().draw(batch);
		}

		boolean useShapeRenderer = drawEntityBoxes || drawTileGrid;
		if (useShapeRenderer) {
			if (shapeRenderer == null) {
				shapeRenderer = new ShapeRenderer();
			}
			batch.end();
			shapeRenderer.setProjectionMatrix(cam.combined);
			shapeRenderer.begin(ShapeType.Line);
		}

		if (drawEntityBoxes) {
			shapeRenderer.setColor(Color.GREEN);
			for (Entity entity : world.getEntities()) {
				shapeRenderer.rect(entity.getPosition().x, entity.getPosition().y,
						entity.getType().getWidth(), entity.getType().getHeight());
			}
		}

		if (drawTileGrid) {
			shapeRenderer.setColor(Color.RED);
			for (int i = startX; i < endX; ++i) {
				shapeRenderer.line(i, startY, i, endY);
			}
			for (int j = startY; j < endY; ++j) {
				shapeRenderer.line(startX, j, endX, j);
			}
		}

		if (useShapeRenderer) {
			shapeRenderer.end();
			batch.begin();
		}
	}

	public WorldView getView() {
		return view;
	}

	public void setView(WorldView view) {
		this.view = view;
	}
}