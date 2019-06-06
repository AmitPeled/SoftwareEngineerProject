/**
 * 
 */
package maps;

import java.io.Serializable;
import java.util.SortedSet;

/**
 * Contains references (by ID) to the maps and sites that are associated to the
 * City
 *
 */
public final class City implements Serializable {
	private static final long serialVersionUID = 1L;

	private int id;
	private String name;
	private String descriptionString;
	private SortedSet<Integer> maps;
	private SortedSet<Integer> sites;

	/**
	 * default empty maps and sites
	 */
	public City(int id, String name, String description) throws IllegalArgumentException {
		this(id, name, description, null, null);
	}

	public City(int id, String name, String description, SortedSet<Integer> maps, SortedSet<Integer> sites) {
		if (id <= 0)
			throw new IllegalArgumentException("id has to be a positive number");
		this.id = id;
		this.name = name;
		this.descriptionString = description;
		this.maps = maps;
		this.sites = sites;
	}

	public int getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public String getDescription() {
		return this.descriptionString;
	}

	public SortedSet<Integer> getMapIds() {
		return this.maps;
	}

	public SortedSet<Integer> getSiteIds() {
		return this.sites;
	}
}
