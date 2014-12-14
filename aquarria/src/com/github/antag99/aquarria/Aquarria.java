/*******************************************************************************
 * Copyright (c) 2014, Anton Gustafsson
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
package com.github.antag99.aquarria;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.github.antag99.aquarria.ui.IngameScreen;
import com.github.antag99.aquarria.util.DirectoryFileHandleResolver;
import com.github.antag99.aquarria.util.FileHandleResolverMultiplexer;
import com.github.antag99.aquarria.util.TextureAtlasLoader;
import com.github.antag99.aquarria.util.TextureRegionLoader;
import com.github.antag99.aquarria.xnb.Steam;

public class Aquarria extends Game {
	private Batch batch;
	private Stage stage;
	private ScreenViewport viewport;

	private AquarriaProperties properties;
	private FileHandle configFile;
	private FileHandle terrariaAssets;
	private FileHandle terrariaDirectory;

	private FileHandleResolverMultiplexer resolver;
	private AssetManager assetManager;
	private IngameScreen ingameScreen;

	@Override
	public void create() {
		batch = new SpriteBatch();
		viewport = new ScreenViewport();
		stage = new Stage(viewport, batch);
		terrariaAssets = Gdx.files.local("assets-terraria");

		DirectoryFileHandleResolver terrariaFiles = new DirectoryFileHandleResolver(terrariaAssets);
		InternalFileHandleResolver internalFiles = new InternalFileHandleResolver();

		resolver = new FileHandleResolverMultiplexer(internalFiles);
		resolver.addResolver(internalFiles);
		resolver.addResolver(terrariaFiles);

		assetManager = new AssetManager(resolver);

		TextureRegionLoader textureRegionLoader = new TextureRegionLoader(resolver, 2048, 2048);
		// Replace libgdx's default TextureAtlasLoader
		TextureAtlasLoader textureAtlasLoader = new TextureAtlasLoader(resolver, 2048, 2048);

		assetManager.setLoader(TextureRegion.class, textureRegionLoader);
		assetManager.setLoader(TextureAtlas.class, textureAtlasLoader);

		configFile = Gdx.files.local("aquarria.json");
		if (configFile.exists()) {
			properties = new Json().fromJson(AquarriaProperties.class, configFile);
		} else {
			properties = new AquarriaProperties();
		}

		terrariaDirectory = properties.getTerrariaDirectory();

		if (!terrariaAssets.exists()) {
			if (terrariaDirectory == null) {
				terrariaDirectory = Steam.findTerrariaDirectory();
				if (terrariaDirectory == null) {
					System.err.println("Error: Terraria directory not found. Edit aquarria.json manually. Exiting.");
					properties.setTerrariaDirectory(new FileHandle("<terraria directory>"));
					Gdx.app.exit();
					return;
				}
			} else if (!terrariaDirectory.exists()) {
				System.err.println("Error: The directory " + terrariaDirectory.path() +
						" does not exist. Edit aquarria.json manually. Exiting.");
				Gdx.app.exit();
				return;
			}

			if (terrariaDirectory != null && terrariaDirectory.exists()) {
				try {
					System.out.println("Extracting assets from " + terrariaDirectory.path());
					long startTime = System.currentTimeMillis();
					ContentExtractor extractor = new ContentExtractor(terrariaDirectory.child("Content"), terrariaAssets);
					extractor.extract();
					long time = System.currentTimeMillis() - startTime;
					System.out.println("Done. Took " + time / 1000f + " seconds");
				} catch (Throwable ex) {
					System.err.println("Error extracting assets. Exiting.");
					ex.printStackTrace();
					Gdx.app.exit();
					return;
				}
			}
		}

		Gdx.input.setInputProcessor(stage);

		ingameScreen = new IngameScreen(this);

		System.out.print("Loading assets... ");

		GameRegistry.loadAssets(assetManager);
		ingameScreen.load();

		assetManager.finishLoading();

		GameRegistry.initialize();
		ingameScreen.initialize();

		System.out.println("Done!");

		setScreen(ingameScreen);
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
		super.dispose();
		batch.dispose();
		new Json().toJson(properties, AquarriaProperties.class, configFile);
	}

	public Stage getStage() {
		return stage;
	}

	public Batch getBatch() {
		return batch;
	}

	@Override
	public void render() {
		Gdx.gl.glClearColor(0.6f, 0.6f, 1f, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		super.render();

		stage.act();
		stage.draw();
	}

	@Override
	public void resize(int width, int height) {
		viewport.update(width, height, true);
		super.resize(width, height);
	}
}
