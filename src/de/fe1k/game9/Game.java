package de.fe1k.game9;

import de.fe1k.game9.commands.Console;
import de.fe1k.game9.components.ComponentPlayer;
import de.fe1k.game9.entities.Entity;
import de.fe1k.game9.events.Event;
import de.fe1k.game9.events.EventAfterRender;
import de.fe1k.game9.events.EventBeforeRender;
import de.fe1k.game9.events.EventUpdate;
import de.fe1k.game9.network.Network;
import de.fe1k.game9.states.GameState;
import de.fe1k.game9.states.StateIngame;
import de.fe1k.game9.states.StateMainMenu;
import de.fe1k.game9.systems.*;
import de.nerogar.noise.Noise;
import de.nerogar.noise.render.GLWindow;
import de.nerogar.noise.render.OrthographicCamera;
import de.nerogar.noise.render.RenderHelper;
import de.nerogar.noise.render.deferredRenderer.DeferredRenderer;
import de.nerogar.noise.util.Timer;

import java.util.ArrayList;
import java.util.List;

public class Game {

	private static final float ZOOM = 1/32f;

	public static  GLWindow           window;
	public static  DeferredRenderer   renderer;
	private        OrthographicCamera camera;
	private static Timer              timer;  // TODO properly distinguish between static and non-static stuff
	private        long               lastFpsUpdate;
	private        List<GameSystem>   systems;

	private Console console;

	public Game() {
		timer = new Timer();
		systems = new ArrayList<>();
		Noise.init("noiseSettings.json");
		setUpWindow();
		setUpCamera();
		setUpRenderer();
		setUpSystems();
		console = new Console(window);
		GameState.transition(new StateMainMenu());
	}

	private void setUpSystems() {
		systems.add(new SystemPhysics());
		systems.add(new SystemCallbacks());
		systems.add(new SystemDeathAnimation(renderer));
		systems.add(new SystemKillOnCollision());
		systems.add(new SystemParticles(renderer));

		systems.forEach(GameSystem::start);
	}

	private void setUpRenderer() {
		renderer = new DeferredRenderer(window.getWidth(), window.getHeight());

		renderer.setSunLightBrightness(0);
		renderer.setAmbientOcclusionEnabled(false);
		renderer.setAntiAliasingEnabled(false);
		renderer.setMinAmbientBrightness(0.1f);
	}

	private void setUpWindow() {
		window = new GLWindow("Game-9", 1280, 720, true, 0, null, null);
		window.setSizeChangeListener((int width, int height) -> {
			renderer.setFrameBufferResolution(width, height);
			camera.setAspect((float) width / height);
			camera.setHeight((float) height * ZOOM);
			console.updateProjectionMatrix(width, height);
		});
	}

	private void setUpCamera() {
		camera = new OrthographicCamera((float) window.getHeight() * ZOOM, (float) window.getWidth() / window.getHeight(), 100, -100);
		camera.setXYZ(10, 10, 10);
	}

	private void displayFPS() {
		if (System.nanoTime() - lastFpsUpdate > 1_000_000_000 / 5) {
			lastFpsUpdate = System.nanoTime();

			float fps = Math.round(timer.getFrequency() * 10f) / 10f;
			float time = Math.round(timer.getCalcTime() * 1000000f) / 1000f;

			window.setTitle("FPS: " + fps + " -> frame time: " + time);
		}
	}

	private void mainloop() {
		GLWindow.updateAll();
		float targetDelta = 1 / 60f;
		timer.update(targetDelta);
		displayFPS();
		boolean shouldUpdate = GameState.getCurrent() instanceof StateIngame;
		//boolean shouldUpdate = Network.isStarted() && (!Network.isServer() || Network.getClients().size() > 0);
		Event.trigger(new EventUpdate(shouldUpdate ? targetDelta : 0));
		Event.trigger(new EventBeforeRender(window, targetDelta, timer.getRuntime()));

		ComponentPlayer player = Entity.getFirstComponent(ComponentPlayer.class);

		if (player != null) {
			camera.setX(player.getOwner().getPosition().getX());
			camera.setY(player.getOwner().getPosition().getY());
		}

		renderer.render(camera);
		console.render();
		Event.trigger(new EventAfterRender(window, targetDelta, timer.getRuntime()));
		window.bind();
		RenderHelper.blitTexture(renderer.getColorOutput());
	}

	public void run() {
		while (!window.shouldClose()) {
			mainloop();
		}
		shutdown();
	}

	private void shutdown() {
		if (Network.isStarted()) Network.shutdown();
		//Entity.despawnAll();  // too slow
		systems.forEach(GameSystem::stop);
	}

	public static double getRunTime() {
		return timer.getRuntime();
	}
}
