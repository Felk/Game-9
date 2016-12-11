package de.fe1k.game9.entities;

import de.fe1k.game9.components.Component;
import de.fe1k.game9.events.Event;
import de.fe1k.game9.events.EventEntityDestroyed;
import de.fe1k.game9.events.EventEntityMoved;
import de.fe1k.game9.events.EventEntitySpawned;
import de.fe1k.game9.exceptions.ComponentAlreadyExistsException;
import de.fe1k.game9.exceptions.MissingComponentDependenciesException;
import de.fe1k.game9.utils.Vector2i;
import de.nerogar.noise.util.Vector2f;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class Entity {

	private final long id;

	private Vector2f position;
	private float    rotation;
	private Vector2f scale;

	private Entity(long id, Vector2f position) {
		this.id = id;
		this.position = position;
		this.rotation = 0;
		this.scale = new Vector2f(1);
	}

	private void throwOnMissingDependencies() {
		if (!getComponents(this).stream().allMatch(Component::dependenciesSatisfied)) {
			throw new MissingComponentDependenciesException();
		}
	}

	/**
	 * Looks up if the entity has a component by class.
	 *
	 * @param componentClass the component's class
	 * @return true if the entity has a component of that class, false otherwise
	 */
	public <T extends Component> boolean hasComponent(Class<T> componentClass) {
		return Entity.hasComponent(this, componentClass);
	}

	/**
	 * Adds a component to the entity. Removes it from the previous owner first, if it had one.
	 *
	 * @param component component to add
	 * @throws ComponentAlreadyExistsException if this entity already has a component of that class.
	 */
	public void addComponent(Component component) {
		Entity.addComponent(this, component);
	}

	/**
	 * Removes a component from the entity by component class.
	 *
	 * @param componentClass class of the components to remove.
	 * @return the component removed, or null if no component got removed.
	 */
	public <T extends Component> Component removeComponent(Class<T> componentClass) {
		return Entity.removeComponent(this, componentClass);
	}

	/**
	 * Looks up a component by class.
	 *
	 * @param clazz the component's class
	 * @return the component object for that class, or null if the entity doesn't have that component.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Component> T getComponent(Class<T> clazz) {
		return Entity.getComponent(this, clazz);
	}

	public long getId() {
		return id;
	}

	public void teleport(float x, float y) {
		if (position.getX() == x && position.getY() == y) {
			return;
		}
		Vector2f from = position.clone();
		position.setX(x);
		position.setY(y);
		moveLookup(from.getX(), from.getY(), position.getX(), position.getY());
		Event.trigger(new EventEntityMoved(this, from, position.clone()));
	}

	public void teleport(Vector2f position) {
		teleport(position.getX(), position.getY());
	}

	public void move(float x, float y) {
		if (x == 0 && y == 0) {
			return;
		}
		Vector2f from = position.clone();
		position.addX(x);
		position.addY(y);
		moveLookup(from.getX(), from.getY(), position.getX(), position.getY());
		Event.trigger(new EventEntityMoved(this, from, position.clone()));
	}

	/**
	 * Getter for the position of this entity.
	 * DO NOT directly modify the vector returned by this, instead
	 * use the {@link #teleport(float, float)} or {@link #move(float, float)} method instead.
	 *
	 * @return position of this entity
	 */
	public Vector2f getPosition() {
		return position;
	}

	public float getRotation() {
		return rotation;
	}

	public void setRotation(float rotation) {
		this.rotation = rotation;
	}

	public Vector2f getScale() {
		return scale;
	}

	private void destroy() {
		for (Component component : Entity.getComponents(this)) {
			Entity.removeComponent(this, component.getClass());
			component.destroy();
		}
	}

	@Override
	public boolean equals(Object o) {
		// generated by IntelliJ IDEA
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Entity entity = (Entity) o;

		return id == entity.id;
	}

	@Override
	public int hashCode() {
		// generated by IntelliJ IDEA
		return (int) (id ^ (id >>> 32));
	}

	@Override
	public String toString() {
		return "Entity{" +
				"id=" + id +
				'}';
	}

	////////////////// STATIC STUFF //////////////////

	private static class ComponentMap extends HashMap<Class<? extends Component>, Map<Entity, Component>> {}

	private static ComponentMap      componentMap;
	private static Map<Long, Entity> entities;
	private static Random            uniqueIdRandom;

	static {
		componentMap = new ComponentMap();
		entities = new HashMap<>();
		uniqueIdRandom = new Random();
	}

	public static Entity getById(long id) {
		return entities.get(id);
	}

	public static Stream<Entity> getAll() {
		return entities.values().stream();
	}

	/**
	 * Returns all entities that have all the given components specified by classes.
	 *
	 * @param componentClasses list of component classes to check for
	 * @return stream of entities that have all the components
	 */
	@SafeVarargs
	public static Set<Entity> getAllWithComponents(Class<? extends Component>... componentClasses) {
		Set<Entity> entitiesWithComponents = new HashSet<>();
		for (Class<? extends Component> componentClass : componentClasses) {
			Map<Entity, Component> entityMap = componentMap.get(componentClass);
			if (entityMap != null) {
				entitiesWithComponents.addAll(entityMap.keySet());
			}
		}
		return entitiesWithComponents;
	}

	/**
	 * Returns all components of the given class that match the given predicate.
	 *
	 * @param componentClass the component's class
	 * @param predicate the predicate to evaluate
	 * @return collection of components of that class matching the predicate
	 */
	public static <T extends Component> Collection<T> getComponents(Class<T> componentClass, Predicate<T> predicate) {
		@SuppressWarnings("unchecked") Map<Entity, T> components = (Map<Entity, T>) componentMap.get(componentClass);
		Collection<T> matches = new ArrayList<>();
		if (components == null) {
			return matches;
		}
		for (T component : components.values()) {
			if (predicate.test(component)) {
				matches.add(component);
			}
		}
		return matches;
	}


	/**
	 * Returns all components of the given class
	 *
	 * @param componentClass the component's class
	 * @return collection of components of that class
	 */
	public static <T extends Component> Collection<T> getComponents(Class<T> componentClass) {
		@SuppressWarnings("unchecked") Map<Entity, T> components = (Map<Entity, T>) componentMap.get(componentClass);
		if (components == null) {
			return new ArrayList<>();
		}
		return components.values();
	}

	/**
	 * Returns the first components of the given class that match the given predicate
	 *
	 * @param componentClass the component's class
	 * @param predicate the predicate to evaluate
	 * @return one components of that class matching the predicate, or null if none found
	 */
	public static <T extends Component> T getFirstComponent(Class<T> componentClass, Predicate<T> predicate) {
		@SuppressWarnings("unchecked") Map<Entity, T> components = (Map<Entity, T>) componentMap.get(componentClass);
		if (components == null || components.isEmpty()) {
			return null;
		}
		for (T component : components.values()) {
			if (predicate.test(component)) {
				return component;
			}
		}
		return null;
	}

	/**
	 * Returns the first components of the given class
	 *
	 * @param componentClass the component's class
	 * @return one component of that class, or null if none found
	 */
	public static <T extends Component> T getFirstComponent(Class<T> componentClass) {
		@SuppressWarnings("unchecked") Map<Entity, T> components = (Map<Entity, T>) componentMap.get(componentClass);
		if (components == null || components.isEmpty()) {
			return null;
		}
		return components.values().iterator().next();
	}

	public static Entity spawn(Vector2f position) {
		Entity entity = new Entity(getUniqueId(), position);
		entities.put(entity.getId(), entity);
		entity.addLookup(position.getX(), position.getY());
		Event.trigger(new EventEntitySpawned(entity));
		return entity;
	}

	public static void despawn(long entityId) {
		Entity removedEntity = entities.remove(entityId);
		Event.trigger(new EventEntityDestroyed(removedEntity));
		removedEntity.removeLookup(removedEntity.position.getX(), removedEntity.position.getY());
		removedEntity.destroy();
	}

	private static List<Component> getComponents(Entity entity) {
		List<Component> components = new ArrayList<>();
		for (Map<Entity, Component> entityComponentMap : componentMap.values()) {
			Component component = entityComponentMap.get(entity);
			if (component != null) {
				components.add(component);
			}
		}
		return components;
	}

	private static void addComponent(Entity entity, Component component) {
		Class<? extends Component> componentClass = component.getClass();
		Map<Entity, Component> components = componentMap.computeIfAbsent(componentClass, k -> new HashMap<>());
		if (components.containsKey(entity)) {
			throw new ComponentAlreadyExistsException();
		}
		components.put(entity, component);
		Entity previousOwner = component.getOwner();
		component.setOwner(entity);
		if (previousOwner != null) {
			previousOwner.removeComponent(component.getClass());
		} else {
			component.init();
		}
		entity.throwOnMissingDependencies();
	}

	private static <T extends Component> T removeComponent(Entity entity, Class<T> componentClass) {
		@SuppressWarnings("unchecked") Map<Entity, T> components = (Map<Entity, T>) componentMap.get(componentClass);
		if (components == null) {
			return null;
		}
		T removedComponent = components.remove(entity);
		entity.throwOnMissingDependencies();
		return removedComponent;
	}

	private static boolean hasComponent(Entity entity, Class<? extends Component> componentClass) {
		Map<Entity, Component> components = componentMap.get(componentClass);
		return components != null && components.containsKey(entity);
	}

	private static <T extends Component> T getComponent(Entity entity, Class<T> componentClass) {
		@SuppressWarnings("unchecked") Map<Entity, T> components = (Map<Entity, T>) componentMap.get(componentClass);
		if (components == null) {
			return null;
		}
		return components.get(entity);
	}

	public static void despawnAll() {
		while (!entities.isEmpty()) {
			despawn(entities.values().iterator().next().getId());
		}
	}

	private static long getUniqueId() {
		return uniqueIdRandom.nextLong();
	}

	////////////////// Entity Lookup Code //////////////////

	private static Map<Vector2i, Set<Entity>> entityLookup = new HashMap<>();
	private static Vector2i temp = new Vector2i();

	public static Set<Entity> getAt(int x, int y) {
		Vector2i pos = getTempVector(x, y);
		if (!entityLookup.containsKey(pos)) {
			// clone temp to avoid mutation of the HashMap key
			entityLookup.put(pos.clone(), new HashSet<>());
		}
		return entityLookup.get(pos);
	}

	public static Set<Entity> getAt(float x, float y) {
		return getAt((int) Math.floor(x), (int) Math.floor(y));
	}

	public static Entity getFirstAt(float x, float y) {
		return getAt(x, y).stream().findFirst().orElse(null);
	}

	public static Entity getFirstAt(float x, float y, Predicate<Entity> predicate) {
		return getAt(x, y).stream().filter(predicate).findFirst().orElse(null);
	}

	private void addLookup(int x, int y) {
		Vector2i pos = getTempVector(x, y);
		if (!entityLookup.containsKey(pos)) {
			// clone temp to avoid mutation of the HashMap key
			entityLookup.put(pos.clone(), new HashSet<>());
		}
		entityLookup.get(pos).add(this);
	}

	private void addLookup(float x, float y) {
		addLookup((int) Math.floor(x), (int) Math.floor(y));
	}

	private void removeLookup(int x, int y) {
		Vector2i pos = getTempVector(x, y);
		if (entityLookup.containsKey(pos)) {
			entityLookup.get(pos).remove(this);
		}
	}

	private void removeLookup(float x, float y) {
		removeLookup((int) Math.floor(x), (int) Math.floor(y));
	}

	private void moveLookup(int fromX, int fromY, int toX, int toY) {
		if (fromX == toX && fromY == toY) return;
		removeLookup(fromX, fromY);
		addLookup(toX, toY);
	}

	private void moveLookup(float fromX, float fromY, float toX, float toY) {
		moveLookup((int) Math.floor(fromX), (int) Math.floor(fromY), (int) Math.floor(toX), (int) Math.floor(toY));
	}

	private static Vector2i getTempVector(int x, int y) {
		temp.setX(x);
		temp.setY(y);
		return temp;
	}

}
