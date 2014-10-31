package com.github.antag99.aquarria;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.github.antag99.aquarria.entity.Entity;
import com.github.antag99.aquarria.entity.EntityType;
import com.github.antag99.aquarria.tile.TileType;
import com.github.antag99.aquarria.world.World;
import com.github.antag99.aquarria.world.WorldGenerator;
import com.github.antag99.aquarria.world.WorldRenderer;
import com.github.antag99.aquarria.world.WorldView;
import com.github.antag99.aquarria.xnb.Steam;

public class Aquarria implements ApplicationListener {
	public static final float PIXELS_PER_METER = 16;
	
	private Batch batch;
	private Stage stage;
	private ScreenViewport viewport;

	private World world;
	private WorldView worldView;
	private WorldRenderer worldRenderer;
	
	private Entity player;
	
	private AquarriaProperties properties;
	private FileHandle configFile;
	private FileHandle terrariaAssets;
	private FileHandle terrariaDirectory;
	
	private FileHandleResolverMultiplexer resolver;
	private AssetManager assetManager;

	@Override
	public void create() {
		batch = new SpriteBatch();
		viewport = new ScreenViewport();
		stage = new Stage(viewport, batch);
		terrariaAssets = Gdx.files.local("assets-terraria");
		
		InternalFileHandleResolver internalFiles = new InternalFileHandleResolver();
		DirectoryFileHandleResolver terrariaFiles = new DirectoryFileHandleResolver(terrariaAssets);
		
		resolver = new FileHandleResolverMultiplexer(internalFiles);
		resolver.addResolver(internalFiles);
		resolver.addResolver(terrariaFiles);
		
		TextureRegionLoader textureRegionLoader = new TextureRegionLoader(resolver, 2048, 2048);
		
		assetManager = new AssetManager(resolver);
		assetManager.setLoader(TextureRegion.class, textureRegionLoader);
		
		configFile = Gdx.files.local("aquarria.json");
		if(configFile.exists()) {
			properties = new Json().fromJson(AquarriaProperties.class, configFile);
		} else {
			properties = new AquarriaProperties();
		}
		
		terrariaDirectory = properties.getTerrariaDirectory();
		
		if(!terrariaAssets.exists() || properties.getForceExtractAssets()) {
			if(terrariaDirectory == null) {
				terrariaDirectory = Steam.findTerrariaDirectory();
				if(terrariaDirectory == null) {
					System.err.println("Error: Terraria directory not found. Edit aquarria.json manually. Exiting.");
					properties.setTerrariaDirectory(new FileHandle("<terraria directory>"));
					Gdx.app.exit();
				}
			} else if(!terrariaDirectory.exists()) {
				System.err.println("Error: The directory " + terrariaDirectory.path() +
						" does not exist. Edit aquarria.json manually. Exiting.");
				Gdx.app.exit();
			}
			
			if(terrariaDirectory != null && terrariaDirectory.exists()) {
				try {
					System.out.println("Extracting assets from " + terrariaDirectory.path());
					long startTime = System.currentTimeMillis();
					ContentExtractor extractor = new ContentExtractor(terrariaDirectory.child("Content"), terrariaAssets);
					extractor.extract();
					long time = System.currentTimeMillis() - startTime;
					System.out.println("Done. Took " + time / 1000f + " seconds");
				} catch(Throwable ex) {
					System.err.println("Error extracting assets. Exiting.");
					ex.printStackTrace();
					Gdx.app.exit();
					return;
				}
			}
			
			properties.setForceExtractAssets(false);
		}

		world = new World(1024, 512);
		new WorldGenerator().generate(world, 0);
		player = new Entity(EntityType.player);
		player.getPosition().set(world.getSpawnPoint());
		world.addEntity(player);
		worldView = new WorldView();
		worldView.setWorld(world);
		worldRenderer = new WorldRenderer();
		worldRenderer.setView(worldView);
		worldRenderer.setFillParent(true);
		stage.addActor(worldRenderer);
		Gdx.input.setInputProcessor(stage);
		
		System.out.print("Loading assets... ");
		for(EntityType entityType : EntityType.getEntityTypes())
			if(entityType.getTexturePath() != null)
				assetManager.load(entityType.getTexturePath(), TextureRegion.class);
		
		for(TileType tileType : TileType.getTileTypes())
			if(tileType.getTexturePath() != null)
				assetManager.load(tileType.getTexturePath(), TextureRegion.class);
		
		assetManager.finishLoading();
		
		for(EntityType entityType : EntityType.getEntityTypes())
			entityType.getTexture(assetManager);
		
		for(TileType tileType : TileType.getTileTypes())
			tileType.getTexture(assetManager);
		
		System.out.println("Done!");
	}

	public AquarriaProperties getProperties() {
		return properties;
	}
	
	public AssetManager getAssetManager() {
		return assetManager;
	}
	
	public FileHandleResolverMultiplexer getResolver() {
		return resolver;
	}

	@Override
	public void dispose() {
		batch.dispose();
		new Json().toJson(properties, AquarriaProperties.class, configFile);
	}

	@Override
	public void render() {
		if(Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
			worldRenderer.setDrawTileGrid(!worldRenderer.getDrawTileGrid());
		}
		if(Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
			worldRenderer.setDrawEntityBoxes(!worldRenderer.getDrawEntityBoxes());
		}
		
		float delta = Gdx.graphics.getDeltaTime();
		world.update(delta);

		OrthographicCamera cam = worldView.getCamera();

		Vector2 playerPosition = player.getPosition();
		cam.position.x = playerPosition.x;
		cam.position.y = playerPosition.y;
		cam.zoom = 0.7f;

		cam.update();
		
		Gdx.gl.glClearColor(0.6f, 0.6f, 1f, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		stage.act();
		stage.draw();
	}

	@Override
	public void resize(int width, int height) {
		OrthographicCamera cam = worldView.getCamera();
		cam.viewportWidth = width / PIXELS_PER_METER;
		cam.viewportHeight = height / PIXELS_PER_METER;
		cam.update();

		viewport.update(width, height, true);
	}

	@Override
	public void resume() {
	}

	@Override
	public void pause() {
	}
}
