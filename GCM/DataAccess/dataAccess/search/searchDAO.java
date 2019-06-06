package dataAccess.search;

import java.util.List;

import maps.Map;

public interface searchDAO {

	List<Map> getMapsByCityName(String cityName);

	List<Map> getMapsBySiteName(String siteName);

	/**
	 * by description of a city or site (may be partial)
	 */
	List<Map> getMapsByDescription(String description);

}
