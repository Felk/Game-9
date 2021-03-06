package de.fe1k.game9.components;

import de.fe1k.game9.events.Event;
import de.fe1k.game9.events.EventBeforeRender;
import de.fe1k.game9.events.EventListener;
import de.nerogar.noise.render.deferredRenderer.DeferredRenderer;
import de.nerogar.noise.render.deferredRenderer.Light;
import de.nerogar.noise.util.Color;
import de.nerogar.noise.util.Vector3f;

public class ComponentLight extends Component {

	private DeferredRenderer                 renderer;
	private Light                            light;
	private EventListener<EventBeforeRender> eventBeforeRender;

	public ComponentLight(DeferredRenderer renderer, Color color, float reach, float intensity) {

		this.renderer = renderer;

		light = new Light(new Vector3f(0, 0, 2), color, reach, intensity);
		renderer.getLightContainer().add(light);

		eventBeforeRender = this::updateLight;
		Event.register(EventBeforeRender.class, eventBeforeRender);
	}

	private void updateLight(EventBeforeRender event) {
		light.position.setX(getOwner().getPosition().getX() + Math.abs(getOwner().getScale().getX()) * 0.5f);
		light.position.setY(getOwner().getPosition().getY() + Math.abs(getOwner().getScale().getY()) * 0.5f);
	}

	@Override
	public void destroy() {
		super.destroy();
		renderer.getLightContainer().remove(light);
		Event.unregister(EventBeforeRender.class, eventBeforeRender);
	}
}
