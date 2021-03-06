package database.execution;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TreeSet;

import approvalReports.ActionTaken;
import approvalReports.ObjectsEnum;
import approvalReports.cityApprovalReports.CitySubmission;
import approvalReports.mapApprovalReports.MapSubmission;
import approvalReports.priceApprovalReports.PriceSubmission;
import approvalReports.sitesApprovalReports.SiteSubmission;
import approvalReports.tourApprovalReports.TourSubmission;
import dataAccess.customer.PurchaseHistory;
import dataAccess.generalManager.Report;
import dataAccess.search.CityMaps;
import dataAccess.users.PurchaseDetails;
import database.metadata.DatabaseMetaData;
import database.metadata.DatabaseMetaData.Tables;
import database.objectParse.IParseObjects;
import database.serverObjects.MapSubmissionContent;
import database.objectParse.Status;
import maps.City;
import maps.Map;
import maps.Site;
import maps.Tour;
import queries.RequestState;
import users.User;
import users.UserReport;
import users.UserType;

/**
 * @author amit
 *
 */
@SuppressWarnings({ "serial", "unchecked" })
public class GcmDataExecutor implements
          IGcmDataExecute/* , IGcmCustomerExecutor, */ /* IGcmEditorExecutor, *//* IGcmContentManagerExecutor */ {
	 IExecuteQueries queryExecutor;
	 IParseObjects   objectParser;

	 public GcmDataExecutor(IExecuteQueries queryExecutor, IParseObjects objectParser) {
		  this.queryExecutor = queryExecutor;
		  this.objectParser = objectParser;
	 }

	 @Override
	 public boolean addUser(String username, String password, User user) throws SQLException {
		  return addUserToTable(username, password, user, DatabaseMetaData.getTableName(Tables.customerUsers));
	 }

	 public boolean addUserToTable(String username, String password, User user, String tableName) throws SQLException {
		  if (queryExecutor.selectColumnsByValue(tableName, "username", username, "*").isEmpty()) {
			   List<Object> userList = new ArrayList<Object>() {
					{
						 add(username);
						 add(password);
					}
			   };
			   userList.addAll(objectParser.getUserFieldsList(user));
			   queryExecutor.insertToTable(tableName, userList);
			   return true;
		  }
		  return false;
	 }

	 @Override
	 public RequestState verifyUser(String username, String password) throws SQLException {
		  if (username != null && password != null) {

			   List<Object> valuesList = new ArrayList<Object>() {
					{
						 add(username);
						 add(password);
					}
			   };
			   List<String> namesList = new ArrayList<String>() {
					{
						 add("username");
						 add("password");
					}
			   };
			   boolean isEditor = !queryExecutor
			             .selectColumnsByValues(DatabaseMetaData.getTableName(Tables.editorUsers), namesList,
			                       valuesList, "username, password")
			             .isEmpty();
			   boolean isCustomer = !queryExecutor
			             .selectColumnsByValues(DatabaseMetaData.getTableName(Tables.customerUsers), namesList,
			                       valuesList, "username, password")
			             .isEmpty();
			   boolean isCManager = !queryExecutor
			             .selectColumnsByValues(DatabaseMetaData.getTableName(Tables.contentManagerUsers), namesList,
			                       valuesList, "username, password")
			             .isEmpty();
			   boolean isGManager = !queryExecutor
			             .selectColumnsByValues(DatabaseMetaData.getTableName(Tables.generalManagerUsers), namesList,
			                       valuesList, "username, password")
			             .isEmpty();

			   if (username.equals("editor") && password.equals("editor") || isEditor) {
					return RequestState.editor;
			   } else if (username.equals("c-manager") && password.equals("c-manager") || isCManager) {
					return RequestState.contentManager;
			   } else if (username.equals("manager") && password.equals("manager") || isGManager) {
					return RequestState.manager;
			   } else if (isCustomer)
					return RequestState.customer;
		  }
		  return RequestState.wrongDetails;

	 }

	 @Override
	 public User getUserDetails(String username) throws SQLException {
		  List<List<Object>> rows = queryExecutor.selectColumnsByValue(
		            DatabaseMetaData.getTableName(Tables.customerUsers), "username", username, "*");
		  rows.addAll(queryExecutor.selectColumnsByValue(DatabaseMetaData.getTableName(Tables.editorUsers), "username",
		            username, "*"));
		  rows.addAll(queryExecutor.selectColumnsByValue(DatabaseMetaData.getTableName(Tables.contentManagerUsers),
		            "username", username, "*"));
		  rows.addAll(queryExecutor.selectColumnsByValue(DatabaseMetaData.getTableName(Tables.generalManagerUsers),
		            "username", username, "*"));
		  if (rows.isEmpty())
			   return null;
		  else
			   return objectParser.getUser(rows.get(0));
	 }

	 private boolean userExists(String username) throws SQLException {
		  return !queryExecutor.selectColumnsByValue(DatabaseMetaData.getTableName(Tables.customerUsers), "username",
		            username, "*").isEmpty()
		            || !queryExecutor.selectColumnsByValue(DatabaseMetaData.getTableName(Tables.editorUsers),
		                      "username", username, "*").isEmpty()
		            || !queryExecutor.selectColumnsByValue(DatabaseMetaData.getTableName(Tables.contentManagerUsers),
		                      "username", username, "*").isEmpty()
		            || !queryExecutor.selectColumnsByValue(DatabaseMetaData.getTableName(Tables.generalManagerUsers),
		                      "username", username, "*").isEmpty();
	 }

	 @Override
	 public RequestState editUser(String oldUsername, String oldPassword, User user, String newPassword)
	           throws SQLException {
		  if (user == null)
			   return null;

		  if ((!user.getUsername().equals(oldUsername)) && userExists(user.getUsername())) { // username already exists
			   return RequestState.usernameAlreadyExists;
		  }
		  RequestState requestState = RequestState.wrongDetails;

		  if (updateUserRow(Tables.customerUsers, oldUsername, oldPassword, user, newPassword))
			   requestState = RequestState.customer;
		  else if (updateUserRow(Tables.editorUsers, oldUsername, oldPassword, user, newPassword))
			   requestState = RequestState.editor;
		  else if (updateUserRow(Tables.contentManagerUsers, oldUsername, oldPassword, user, newPassword))
			   requestState = RequestState.contentManager;
		  else if (updateUserRow(Tables.contentManagerUsers, oldUsername, oldPassword, user, newPassword))
			   requestState = RequestState.contentManager;
		  return requestState;
	 }

	 private boolean updateUserRow(Tables table, String oldUsername, String oldPassword, User user, String password)
	           throws SQLException {
		  List<List<Object>> rows = queryExecutor.selectColumnsByValues(DatabaseMetaData.getTableName(table),
		            new ArrayList<String>() {
			             {
					          add("username");
					          add("password");
			             }
		            }, new ArrayList<Object>() {
			             {
					          add(oldUsername);
					          add(oldPassword);
			             }
		            }, "*");
		  if (!rows.isEmpty()) {
			   updateUser(table, oldUsername, user, password);
			   return true;
		  }
		  return false;
	 }

	 // private void updateUserRow(Tables table, String oldUsername, User user)
	 // throws SQLException {
//			List<List<Object>> rows = queryExecutor.selectColumnsByValue(DatabaseMetaData.getTableName(table), "username",
//			        oldUsername, "*");
//			if (!rows.isEmpty()) {
//				String oldPassword = (String) rows.get(0).get(1);
//				updateUser(table, oldUsername, user, oldPassword);
//			}
	 // }

	 private void updateUser(Tables table, String oldUsername, User user, String password) throws SQLException {
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(table), "username", oldUsername);
		  List<Object> userRow = new ArrayList<>();
		  String newUsername = user.getUsername();
		  userRow.add(newUsername);
		  System.out.println("inserting new password: " + password);
		  userRow.add(password);
		  userRow.addAll(objectParser.getUserFieldsList(user));
		  queryExecutor.insertToTable(DatabaseMetaData.getTableName(table), userRow);
		  queryExecutor.updateTableColumn(DatabaseMetaData.getTableName(Tables.mapsDownloadHistory), "username",
		            newUsername, "username", oldUsername);
		  queryExecutor.updateTableColumn(DatabaseMetaData.getTableName(Tables.purchaseHistory), "username",
		            newUsername, "username", oldUsername);
		  queryExecutor.updateTableColumn(DatabaseMetaData.getTableName(Tables.customerPurchaseDetails), "username",
		            newUsername, "username", oldUsername);
	 }

	 @Override
	 public int addMapToCity(int cityId, Map mapDescription, File mapFile/* , String pathToFilesFolder */)
	           throws SQLException {
		  return addMapToCityByStatus(cityId, mapDescription, mapFile, Status.ADD);
	 }

	 public int addMapToCityByStatus(int cityId, Map mapDescription, File mapFile, Status status) throws SQLException {
		  if (mapDescription != null) {
			   int mapId = addMapDetailsToCity(cityId, mapDescription, status);
			   addMapFile(cityId, mapId, mapFile, status);
			   return mapId;
		  }
		  return -1;
	 }

	 private int addMapDetailsToCity(int cityId, Map mapDescription, Status status) throws SQLException {
		  int id = mapDescription.getId();
		  if (status == Status.ADD) {
			   id = queryExecutor.insertAndGenerateId(DatabaseMetaData.getTableName(Tables.mapsMetaDetails),
			             objectParser.getMapMetaFieldsList(mapDescription), status);
		  } else {
			   queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.mapsMetaDetails),
			             objectParser.getMapMetaFieldsList(mapDescription), status);
		  }
		  int mapId = id;
		  List<Object> cityRow = new ArrayList<Object>() {
			   {
					add(cityId);
					add(mapId);
			   }
		  };
		  queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.citiesMapsIds), cityRow, status);
		  return mapId;

	 }

	 private void addMapFile(int cityId, int mapId, File mapFile, Status status) throws SQLException {
		  List<Object> mapFileRow = new ArrayList<>();
		  try {
			   byte[] fileBytes = new byte[(int) mapFile.length()];
			   FileInputStream fileInputStream = new FileInputStream(mapFile);
			   fileInputStream.read(fileBytes);
			   fileInputStream.close();
			   mapFileRow = new ArrayList<Object>() {
					{
						 add(mapId);
						 add(fileBytes);
					}
			   };
		  } catch (FileNotFoundException e) {
			   e.printStackTrace();
		  } catch (IOException e) {
			   e.printStackTrace();
		  }
		  queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.mapsFiles), mapFileRow, status);
	 }

	 @Override
	 public Map getMapDetails(int mapId) throws SQLException {
		  return getMapDetails(mapId, Status.PUBLISH);

	 }

	 public Map getMapDetails(int mapId, Status status) throws SQLException {
		  List<List<Object>> metaDetailsRows = queryExecutor.selectColumnsByValue(
		            DatabaseMetaData.getTableName(Tables.mapsMetaDetails), "mapId", mapId, "*", status);
		  if (metaDetailsRows.isEmpty())
			   return null;
		  else {
			   List<Integer> mapSitesIds = toIdList(queryExecutor.selectColumnsByValue(
			             DatabaseMetaData.getTableName(Tables.mapsSites), "mapId", mapId, "siteId", status));
			   List<Site> mapSites = new ArrayList<>();
			   for (int siteId : mapSitesIds) {
					Site site = getSite(siteId, status);
					if (site != null)
						 mapSites.add(site);
			   }
			   List<Integer> mapToursIds = toIdList(queryExecutor.selectColumnsByValue(
			             DatabaseMetaData.getTableName(Tables.mapsTours), "mapId", mapId, "tourId", status));
			   List<Tour> mapTours = new ArrayList<>();
			   for (int tourId : mapToursIds) {
					Tour tour = getTour(tourId, status);
					if (tour != null)
						 mapTours.add(tour);
			   }
			   return objectParser.getMap(metaDetailsRows.get(0), mapSites, mapTours);
		  }
	 }

	 public List<Map> getMapsDetails(int mapId, Status status) throws SQLException {
		  List<List<Object>> metaDetailsRows = queryExecutor.selectColumnsByValue(
		            DatabaseMetaData.getTableName(Tables.mapsMetaDetails), "mapId", mapId, "*", status);
		  if (metaDetailsRows.isEmpty())
			   return null;
		  else {
			   List<Integer> mapSitesIds = toIdList(queryExecutor.selectColumnsByValue(
			             DatabaseMetaData.getTableName(Tables.mapsSites), "mapId", mapId, "siteId", status));
			   List<Site> mapSites = new ArrayList<>();
			   for (int siteId : mapSitesIds) {
					Site site = getSite(siteId, status);
					if (site != null)
						 mapSites.add(site);
			   }
			   List<Integer> mapToursIds = toIdList(queryExecutor.selectColumnsByValue(
			             DatabaseMetaData.getTableName(Tables.mapsTours), "mapId", mapId, "tourId", status));
			   List<Tour> mapTours = new ArrayList<>();
			   for (int tourId : mapToursIds) {
					Tour tour = getTour(tourId, status);
					if (tour != null)
						 mapTours.add(tour);
			   }
			   List<Map> maps = new ArrayList<>();
			   for (List<Object> mapRow : metaDetailsRows) {
					maps.add(objectParser.getMap(mapRow, mapSites, mapTours));
			   }
			   return maps;
		  }
	 }

	 public boolean ownActiveSubsicription(int cityId, String username) throws SQLException {
		  return queryExecutor.selectColumnsByValues(DatabaseMetaData.getTableName(Tables.purchaseHistory),
		            Arrays.asList("username", "cityId"), Arrays.asList(cityId, username), "*").isEmpty();
	 }

	 public boolean hadPurchasedCityInPast(int cityId, String username) throws SQLException {
		 return  queryExecutor.betweenDatesAnd2Conditions(
		            DatabaseMetaData.getTableName(Tables.purchaseHistory), "*", new Date(0), "occurrenceDate",
		                      new java.sql.Date(Calendar.getInstance().getTime().getTime()),"cityId", "username",cityId,username).isEmpty();
	 }

	 @Override
	 public Site getSite(int siteId) throws SQLException {
		  return getSite(siteId, Status.PUBLISH);
	 }

	 public Site getSite(int siteId, Status status) throws SQLException {
		  List<List<Object>> siteRows = queryExecutor.selectColumnsByValue(DatabaseMetaData.getTableName(Tables.sites),
		            "siteId", siteId, "*", status);
		  if (siteRows.isEmpty())
			   return null;
		  else
			   return objectParser.getSite(siteRows.get(0)); // only one site row correspond to this id
	 }

	 @Override
	 public Tour getTour(int tourId) throws SQLException {
		  return getTour(tourId, Status.PUBLISH);
	 }

	 public Tour getTour(int tourId, Status status) throws SQLException {
		  List<List<Object>> tourRows = queryExecutor.selectColumnsByValue(
		            DatabaseMetaData.getTableName(Tables.toursMetaDetails), "tourId", tourId, "*", status);
		  if (tourRows.isEmpty())
			   return null;
		  else {
			   List<List<Object>> siteIdsAndDurances = queryExecutor.selectColumnsByValue(
			             DatabaseMetaData.getTableName(Tables.tourSitesIdsAndDurance), "tourId", tourId,
			             "siteId, siteDurance", status);

			   List<Integer> siteIds = (List<Integer>) (Object) toListOfColumnNum(siteIdsAndDurances, 1);
			   List<Integer> siteDurances = (List<Integer>) (Object) toListOfColumnNum(siteIdsAndDurances, 2);
			   List<Site> sites = getSitesByIds(siteIds, status);
			   return objectParser.getTour(tourRows.get(0), sites, siteDurances); // only one site row correspond to
			                                                                      // this
			                                                                      // id
		  }
	 }

	 private List<Site> getSitesByIds(List<Integer> sitesId, Status status) throws SQLException {
		  List<Site> sites = new ArrayList<>();
		  for (int siteId : sitesId) {
			   Site site = getSite(siteId, status);
			   if (site != null)
					sites.add(site);
		  }
		  return sites;
	 }

	 @Override
	 public byte[] getMapFile(int mapId) throws SQLException {
		  return getMapFile(mapId, Status.PUBLISH);
	 }

	 public byte[] getMapFile(int mapId, Status status) throws SQLException {
		  List<List<Object>> rows = queryExecutor.selectColumnsByValue(DatabaseMetaData.getTableName(Tables.mapsFiles),
		            "mapId", mapId, "mapFile", status);
		  if (rows.isEmpty())
			   return null;
		  else {
			   try {
					return (byte[]) rows.get(0).get(0); // only one row correspond to this id
			   } catch (Exception e) {
					return null;
			   }

		  }
	 }

	 @Override
	 public void deleteCityEdit(int cityId) throws SQLException {
		  int cityRowSize = DatabaseMetaData.getTableColumnsSize(Tables.citiesMetaDetails);
		  List<Object> cityDeletionRow = nullList(cityRowSize);
		  cityDeletionRow.set(0, cityId);
		  queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.citiesMapsIds), cityDeletionRow,
		            Status.DELETE);
	 }

	 @Override
	 public void deleteMapEdit(int mapId) throws SQLException {
		  City city = getCityByMapId(mapId);
		  if (city != null)
			   queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.citiesMapsIds),
			             Arrays.asList(city.getId(), mapId), Status.DELETE);
	 }

	 List<Object> nullList(int size) {
		  List<Object> list = new ArrayList<>();
		  while (--size >= 0)
			   list.add(null);
		  return list;
	 }

	 public void deleteMapByStatus(int mapId, Status status) throws SQLException {
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.mapsMetaDetails), "mapId", mapId,
		            status);
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.mapsFiles), "mapId", mapId, status);
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.mapsSites), "mapId", mapId, status);
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.citiesMapsIds), "mapId", mapId,
		            status);
	 }

	 @Override
	 public int addCity(City city) throws SQLException {
		  return addCity(city, Status.ADD, true);
	 }

	 public int addCity(City city, Status status, boolean defaultPrices) throws SQLException {
		  int id = city.getId();
		  List<Object> cityRow;
		  if (defaultPrices)
			   cityRow = objectParser.getCityFieldsWithDefualtPrice(city);
		  else
			   cityRow = objectParser.getCityFields(city);
		  if (status == Status.ADD)
			   id = queryExecutor.insertAndGenerateId(DatabaseMetaData.getTableName(Tables.citiesMetaDetails), cityRow,
			             status);
		  else
			   queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.citiesMetaDetails), cityRow, status);

		  // TODO change city to contain full objects
		  // for (Map map : city.getMaps()) {
		  // queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.citiesMapsIds),
		  // new ArrayList<Object>() {
		  // {
		  // add(id);
		  // add(map.getId());
		  // }
		  // });
		  // }
		  // for (Site site : city.getSites()) {
		  // queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.citiesMetaDetails),
		  // new ArrayList<Object>() {
		  // {
		  // add(id);
		  // add(site.getId());
		  // }
		  // });
		  return id;
	 }

//	 private static byte[] getBytes(Object object) {
//		  byte[] objectBytes = null;
//		  try {
//			   ByteArrayOutputStream bos = new ByteArrayOutputStream();
//			   ObjectOutputStream oos = new ObjectOutputStream(bos);
//			   oos.writeObject(object);
//			   oos.close();
//			   bos.close();
//			   objectBytes = bos.toByteArray();
//		  } catch (Exception e) {
//			   e.printStackTrace();
//			   return objectBytes;
//		  }
//		  return objectBytes;
//	 }

//	 private static Object getObject(byte[] bytes) {
//		  Object object = bytes;
//		  if (bytes != null) {
//			   ByteArrayInputStream bais;
//			   ObjectInputStream ins;
//			   try {
//					bais = new ByteArrayInputStream(bytes);
//					if (bais != null) {
//						 ins = new ObjectInputStream(bais);
//						 object = (Object) ins.readObject();
//						 ins.close();
//					}
//			   } catch (Exception e) {
//					e.printStackTrace();
//			   }
//		  }
//		  return object;
//
//	 }

	 // Returns the contents of the file in a byte array.
	 // private static byte[] getBytesFromFile(File file) throws IOException {
	 // // Get the size of the file
	 // long length = file.length();
	 //
	 // // You cannot create an array using a long type.
	 // // It needs to be an int type.
	 // // Before converting to an int type, check
	 // // to ensure that file is not larger than Integer.MAX_VALUE.
	 // if (length > Integer.MAX_VALUE) {
	 // // File is too large
	 // throw new IOException("File is too large!");
	 // }
	 //
	 // // Create the byte array to hold the data
	 // byte[] bytes = new byte[(int) length];
	 //
	 // // Read in the bytes
	 // int offset = 0;
	 // int numRead = 0;
	 //
	 // InputStream is = new FileInputStream(file);
	 // try {
	 // while (offset < bytes.length && (numRead = is.read(bytes, offset,
	 // bytes.length - offset)) >= 0) {
	 // offset += numRead;
	 // }
	 // } finally {
	 // is.close();
	 // }
	 //
	 // // Ensure all the bytes have been read in
	 // if (offset < bytes.length) {
	 // throw new IOException("Could not completely read file " + file.getName());
	 // }
	 // return bytes;
	 // }

	 @Override
	 public int addNewSiteToCity(int cityId, Site site) throws SQLException {
		  return addNewSiteToCityByStatus(cityId, site, Status.ADD);
	 }

	 public int addNewSiteToCityByStatus(int cityId, Site site, Status status) throws SQLException {
		  int id = site.getId();
		  if (status == Status.ADD)
			   id = queryExecutor.insertAndGenerateId(DatabaseMetaData.getTableName(Tables.sites),
			             objectParser.getSiteFieldsList(site), status);
		  else {
			   queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.sites),
			             objectParser.getSiteFieldsList(site), status);
			   // system.err.println("!!. site is PUBLISH. id=" + getSite(id).getId());
		  }
		  int siteId = id;
		  queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.citiesSitesIds), new ArrayList<Object>() {
			   {
					add(cityId);
					add(siteId);
			   }
		  }, status);
		  return siteId;
	 }

	 @Override
	 public void addExistingSiteToMap(int mapId, int siteId) throws SQLException {
		  addSiteToMapByStatus(mapId, siteId, Status.ADD);
	 }

	 private void addSiteToMapByStatus(int mapId, int siteId, Status status) throws SQLException {
		  Site site = getSite(siteId);
		  if (site != null) {
			   // queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.sites),
			   // objectParser.getSiteFieldsList(site), status);
			   queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.mapsSites), new ArrayList<Object>() {
					{
						 add(mapId);
						 add(siteId);

					}
			   }, status);
		  }
	 }

	 @Override
	 public void deleteSiteFromCityEdit(int siteId) throws SQLException {
		  City city = getCityBySite(siteId);
		  if (city != null)
			   queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.citiesSitesIds),
			             Arrays.asList(city.getId(), siteId), Status.DELETE);
	 }

	 @Override
	 public void deleteSiteFromMapEdit(int mapId, int siteId) throws SQLException {
		  queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.mapsTours), Arrays.asList(mapId, siteId),
		            Status.DELETE);
	 }

	 @Override
	 public void deleteSiteFromTourEdit(int tourId, int siteId) throws SQLException {
		  queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.tourSitesIdsAndDurance),
		            Arrays.asList(tourId, siteId), Status.DELETE);

	 }

	 @Override
	 public void deleteTourFromMapEdit(int mapId, int tourId) throws SQLException {
		  queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.mapsTours), Arrays.asList(mapId, tourId),
		            Status.DELETE);

	 }

	 @Override
	 public void deleteTourFromCity(int tourId) throws SQLException {
		  City city = getCityByTourId(tourId);
		  if (city != null)
			   queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.citiesTours),
			             Arrays.asList(city.getId(), tourId), Status.DELETE);
	 }

	 public void deleteFromTables(int id, Status status) throws SQLException {
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.mapsSites), "siteId", id, status);
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.mapsSites), "mapId", id, status);
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.citiesSitesIds), "siteId", id,
		            status);
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.citiesSitesIds), "cityId", id,
		            status);
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.sites), "siteId", id, status);
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.citiesMapsIds), "mapId", id, status);
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.citiesMapsIds), "cityId", id, status);
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.citiesMetaDetails), "cityId", id,
		            status);
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.citiesTours), "tourId", id, status);
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.citiesTours), "cityId", id, status);
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.mapsFiles), "mapId", id, status);
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.mapsMetaDetails), "mapId", id,
		            status);
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.mapsTours), "mapId", id, status);
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.mapsTours), "tourId", id, status);
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.tourSitesIdsAndDurance), "siteId", id,
		            status);
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.tourSitesIdsAndDurance), "tourId", id,
		            status);
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.toursMetaDetails), "tourId", id,
		            status);
	 }

	 public void delete(int id) throws SQLException {
		  deleteCity(id);
		  deleteMap(id);
		  deleteTour(id);
		  deleteSite(id);
	 }

	 private void deleteCity(int id) throws SQLException {
		  for (Status status : Status.values()) {
			   deleteCity(id, status);
		  }

	 }

	 private void deleteCity(int id, Status status) throws SQLException {
		  City city = getCityById(id, status);
		  if (city != null) {
			   queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.citiesMetaDetails), "cityId", id,
			             status);
			   queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.citiesMapsIds), "cityId", id,
			             status);
			   queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.citiesTours), "cityId", id,
			             status);
			   queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.citiesTours), "cityId", id,
			             status);
			   for (int mapId : city.getMapsId())
					deleteMap(mapId);
			   for (int tour : city.getToursId())
					deleteTour(tour);
			   for (int site : city.getSitesId())
					deleteSite(site);
		  }
	 }

	 private void deleteMap(int id) throws SQLException {
		  Map map = getMapDetails(id);
		  if (map != null) {
			   queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.citiesMapsIds), "mapId", id);
			   queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.mapsMetaDetails), "mapId", id);
			   queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.mapsFiles), "mapId", id);
			   queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.mapsTours), "mapId", id);
			   queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.mapsSites), "mapId", id);
			   for (Tour tour : map.getTours())
					deleteTour(tour.getId());
			   for (Site site : map.getSites())
					deleteSite(site.getId());
		  }
	 }

	 private void deleteTour(int id) throws SQLException {
		  Tour tour = getTour(id);
		  if (tour != null) {
			   queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.toursMetaDetails), "tourId", id);
			   queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.mapsTours), "tourId", id);
			   queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.citiesTours), "tourId", id);
			   queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.tourSitesIdsAndDurance),
			             "tourId", id);
			   for (Site site : tour.getSites()) {
					deleteSite(site.getId());
			   }
		  }
	 }

	 private void deleteSite(int id) throws SQLException {
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.sites), "siteId", id);
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.citiesSitesIds), "siteId", id);
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.mapsSites), "siteId", id);
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.tourSitesIdsAndDurance), "siteId",
		            id);
	 }

	 private void deleteSiteFromTour(int mapId, int siteId) throws SQLException {
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.tourSitesIdsAndDurance), "siteId",
		            siteId);
	 }

	 private void deleteSiteFromMap(int mapId, int siteId) throws SQLException {
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.mapsSites), "siteId", siteId);
	 }

	 private void deleteTourFromMap(int mapId, int siteId, Status status) throws SQLException {
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.mapsTours), "siteId", siteId, status);
	 }

	 public void deleteFromTables(int id) throws SQLException {
		  for (Status status : Status.values())
			   deleteFromTables(id, status);
	 }

	 public void deleteSiteByStatus(int siteId, Status status) throws SQLException {
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.mapsSites), "siteId", siteId);
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.citiesSitesIds), "siteId", siteId);
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.sites), "siteId", siteId);
	 }

	 private CityMaps getMapsByCityField(String fieldName, Object fieldVal, boolean withPartialField)
	           throws SQLException {
		  return getMapsByCityField(fieldName, fieldVal, withPartialField, Status.PUBLISH);
	 }

	 private CityMaps getMapsByCityField(String fieldName, Object fieldVal, boolean withPartialField, Status status)
	           throws SQLException {
		  List<Integer> cityIds;
		  if (withPartialField) {

			   cityIds = toIdList(queryExecutor.selectColumnsByPartialValue(
			             DatabaseMetaData.getTableName(Tables.citiesMetaDetails), fieldName,
			             "%" + (String) fieldVal + "%", "cityId", Status.PUBLISH));
		  } else {
			   cityIds = toIdList(
			             queryExecutor.selectColumnsByValue(DatabaseMetaData.getTableName(Tables.citiesMetaDetails),
			                       fieldName, fieldVal, "cityId", Status.PUBLISH));
		  }
		  if (!cityIds.isEmpty()) {
			   City city = getCityById(cityIds.get(0));
			   List<Map> maps = new ArrayList<>();
			   List<List<Object>> mapIdRows = new ArrayList<>();
			   for (int cityId : cityIds) {
					mapIdRows.addAll(
					          queryExecutor.selectColumnsByValue(DatabaseMetaData.getTableName(Tables.citiesMapsIds),
					                    "cityId", cityId, "mapId", Status.PUBLISH));

			   }
			   List<Integer> mapsIds = toIdList(mapIdRows);
			   for (int mapId : mapsIds) {
					maps.add(getMapDetails(mapId, Status.PUBLISH));
			   }
			   return new CityMaps(city.getId(), city.getName(), city.getDescription(), city.getPrices(), maps);

		  } else
			   return null;
	 }

	 private CityMaps getMapsBySiteField(String fieldName, Object fieldVal, boolean withPartialField)
	           throws SQLException {

		  List<Integer> sitesIds;
		  if (withPartialField)
			   sitesIds = toIdList(
			             queryExecutor.selectColumnsByPartialValue(DatabaseMetaData.getTableName(Tables.sites),
			                       fieldName, "%" + (String) fieldVal + "%", "siteId", Status.PUBLISH));
		  else
			   sitesIds = toIdList(queryExecutor.selectColumnsByValue(DatabaseMetaData.getTableName(Tables.sites),
			             fieldName, fieldVal, "siteId", Status.PUBLISH));

		  if (!sitesIds.isEmpty()) {
			   City city = getCityBySite(sitesIds.get(0));

			   List<Map> maps = new ArrayList<>();
			   List<List<Object>> mapIdRows = new ArrayList<>();
			   for (int siteId : sitesIds) {
					mapIdRows.addAll(queryExecutor.selectColumnsByValue(DatabaseMetaData.getTableName(Tables.mapsSites),
					          "siteId", siteId, "mapId", Status.PUBLISH));
			   }
			   List<Integer> mapsIds = toIdList(mapIdRows);
			   for (int mapId : mapsIds) {
					maps.add(getMapDetails(mapId));
			   }
			   return new CityMaps(city.getId(), city.getName(), city.getDescription(), city.getPrices(), maps);
		  } else
			   return null;

	 }

	 private List<Integer> toIdList(List<List<Object>> idRows) {
		  List<Integer> idList = new ArrayList<>();
		  for (List<Object> idRow : idRows) {
			   int id = (int) idRow.get(0);
			   if (!idList.contains(id))
					idList.add(id);
		  }
		  return idList;
	 }

	 private List<Object> toListOfColumnNum(List<List<Object>> listRows, int column) {
		  List<Object> rows = new ArrayList<>();
		  for (List<Object> row : listRows)
			   rows.add(row.get(column - 1));
		  return rows;
	 }

	 @Override
	 public CityMaps getMapsByCityName(String cityName) throws SQLException {
		  return getMapsByCityField("cityName", cityName, false);
	 }

	 @Override
	 public CityMaps getMapsBySiteName(String siteName) throws SQLException {
		  return getMapsBySiteField("siteName", siteName, false);
	 }

	 @Override
	 public CityMaps getMapsByDescription(String description) throws SQLException {
		  CityMaps cityMaps = getMapsByCityField("cityDescription", description, true);
		  if (cityMaps == null)
			   cityMaps = getMapsBySiteField("siteDescription", description, true);
		  return cityMaps;
	 }

	 @Override
	 public CityMaps getMapsBySiteAndCityNames(String cityName, String siteName) throws SQLException {
		  CityMaps cityMaps = getMapsByCityField("cityName", cityName, false);
		  if (cityMaps == null)
			   cityMaps = getMapsBySiteField("siteName", siteName, false);
		  return cityMaps;
	 }

	 @Override
	 public List<Site> getCitySites(int cityId) throws SQLException {
		  return getCitySitesByStatus(cityId, Status.PUBLISH);
	 }

	 public List<Site> getCitySitesByStatus(int cityId, Status status) throws SQLException {
		  List<Integer> sitesId = toIdList(queryExecutor.selectColumnsByValue(
		            DatabaseMetaData.getTableName(Tables.citiesSitesIds), "cityId", cityId, "siteId", status));
		  return getSitesByIds(sitesId, status);
	 }

	 @Override
	 public void updateMap(int mapId, Map newMap) throws SQLException {
		  List<Object> mapRow = objectParser.getMapMetaFieldsList(newMap);
		  mapRow.set(0, mapId);
		  queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.mapsMetaDetails), mapRow, Status.UPDATE);
//		City city = getCityByMapId(mapId);
//		if (city != null) {
//			Status status = Status.UPDATE;
//			int cityId = city.getId();
//			List<Object> mapRow = objectParser.getMapMetaFieldsList(newMap);
//			mapRow.set(0, mapId);
//			queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.mapsMetaDetails),
//			        objectParser.getMapMetaFieldsList(newMap), status);
//			List<Object> cityRow = new ArrayList<Object>() {
//				{
//					add(cityId);
//					add(mapId);
//				}
//			};
//			queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.citiesMapsIds), cityRow, status);
//		}
	 }

	 @Override
	 public void UpdateSite(int siteId, Site newSite) throws SQLException {
		  List<Object> siteRow = objectParser.getSiteFieldsList(newSite);
		  siteRow.set(0, siteId);
		  // deleteSite(siteId);
		  queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.sites), siteRow, Status.UPDATE);
	 }

	 // private void deleteSite(int siteId) throws SQLException {
	 // queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.sites),
	 // "siteId", siteId);
	 // }

	 @Override
	 public City getCityByMapId(int mapId) throws SQLException {
		  City city = getCityByMapId(mapId, Status.PUBLISH);
		  return city != null ? city : getCityByMapId(mapId, Status.ADD);
	 }

	 public City getCityByMapId(int mapId, Status status) throws SQLException {
		  List<List<Object>> lists = queryExecutor.selectColumnsByValue(
		            DatabaseMetaData.getTableName(Tables.citiesMapsIds), "mapId", mapId, "cityId", status);
		  if (lists.isEmpty())
			   return null;
		  else
			   return getCityById((int) lists.get(0).get(0));
	 }

	 @Override
	 public City getCityById(int cityId) throws SQLException {
		  City city = getCityById(cityId, Status.PUBLISH);
		  if (city == null)
			   city = getCityById(cityId, Status.ADD);
		  return city;
	 }

	 private City getCityById(int cityId, Status status) throws SQLException {
		  List<List<Object>> list = queryExecutor.selectColumnsByValue(
		            DatabaseMetaData.getTableName(Tables.citiesMetaDetails), "cityId", cityId, "*", status);
		  if (!list.isEmpty()) {
			   List<Integer> maps = toIdList(queryExecutor.selectColumnsByValue(
			             DatabaseMetaData.getTableName(Tables.citiesMapsIds), "cityId", cityId, "mapId", status));
			   List<Integer> tours = toIdList(queryExecutor.selectColumnsByValue(
			             DatabaseMetaData.getTableName(Tables.citiesTours), "cityId", cityId, "tourId", status));
			   List<Integer> sites = toIdList(queryExecutor.selectColumnsByValue(
			             DatabaseMetaData.getTableName(Tables.citiesSitesIds), "cityId", cityId, "siteId", status));
			   return objectParser.getCity(list.get(0), new TreeSet<>(maps), new TreeSet<>(tours),
			             new TreeSet<>(sites));
		  } else
			   return null;

	 }

	 @Override
	 public void addExistingSiteToTour(int tourId, int siteId, int durnace) throws SQLException {
		  addSiteToTourByStatus(tourId, siteId, durnace, Status.ADD);
	 }

	 public void addSiteToTourByStatus(int tourId, int siteId, int durnace, Status status) throws SQLException {
		  Site site = getSite(siteId);
		  if (site != null) {
			   // queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.sites),
			   // objectParser.getSiteFieldsList(site), status);
			   queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.tourSitesIdsAndDurance),
			             new ArrayList<Object>() {
					          {
						           add(tourId);
						           add(siteId);
						           add(durnace);

					          }
			             }, status);
		  }
	 }

	 @Override
	 public int addNewTourToCity(int cityId, Tour tour) throws SQLException {
		  return addNewTourToCity(cityId, tour, Status.ADD);
	 }

	 public int addNewTourToCity(int cityId, Tour tour, Status status) throws SQLException {
		  int id = tour.getId();
		  if (status == Status.ADD)
			   id = queryExecutor.insertAndGenerateId(DatabaseMetaData.getTableName(Tables.toursMetaDetails),
			             objectParser.getTourMetaFieldsList(tour), status);
		  else
			   queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.toursMetaDetails),
			             objectParser.getTourMetaFieldsList(tour), status);
		  int tourId = id;
		  queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.citiesTours), new ArrayList<Object>() {
			   {
					add(cityId);
					add(tourId);
			   }
		  }, status);
		  return tourId;
	 }

	 @Override
	 public void updateCity(int cityId, City city) throws SQLException {
		  // queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.citiesMetaDetails),
		  // "cityId", cityId);
		  List<Object> cityRow = objectParser.getCityFieldsWithDefualtPrice(city);
		  cityRow.set(0, city);
		  queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.citiesMetaDetails), cityRow, Status.UPDATE);
	 }

	 @Override
	 public void updateTour(int tourId, Tour tour) throws SQLException {
		  // queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.citiesMetaDetails),
		  // "cityId", cityId);
		  List<Object> tourRow = objectParser.getTourMetaFieldsList(tour);
		  tourRow.set(0, tourId);
		  queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.toursMetaDetails), tourRow, Status.UPDATE);
	 }

	 @Override
	 public void addExistingTourToMap(int mapId, int tourId) throws SQLException {
		  addExistingTourToMap(mapId, tourId, Status.ADD);
	 }

	 public void addExistingTourToMap(int mapId, int tourId, Status status) throws SQLException {
		  queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.mapsTours), new ArrayList<Object>() {
			   {
					add(mapId);
					add(tourId);
			   }
		  }, status);
	 }

	 List<Map> getMapsByStatus(Status status) throws SQLException {
		  List<Integer> mapIds = toIdList(
		            queryExecutor.selectColumnsByValue(DatabaseMetaData.getTableName(Tables.mapsMetaDetails), "status",
		                      DatabaseMetaData.getStatus(status), "*"));
		  return toMapsByIds(mapIds, status);
	 }

	 List<City> getCitiesByStatus(Status status) throws SQLException {
		  List<Integer> cityIds = toIdList(
		            queryExecutor.selectColumnsByValue(DatabaseMetaData.getTableName(Tables.citiesMetaDetails),
		                      "status", DatabaseMetaData.getStatus(status), "cityId"));
		  List<City> cityObjects = new ArrayList<>();
		  cityIds.forEach((cityId) -> {
			   try {
					City city = getCityById(cityId, status);
					if (cityId != null)
						 cityObjects.add(city);
			   } catch (SQLException e) {
					e.printStackTrace();
			   }
		  });
		  // //system.err.println(cityObjects.get(0).getDescription());
		  return cityObjects;
	 }

	 List<Site> getSitesByStatus(Status status) throws SQLException {
		  List<Integer> siteIds = toIdList(queryExecutor.selectColumnsByValue(
		            DatabaseMetaData.getTableName(Tables.sites), "status", DatabaseMetaData.getStatus(status), "*"));
		  List<Site> sites = new ArrayList<>();
		  siteIds.forEach((siteId) -> {
			   try {
					Site site = getSite(siteId, status);
					if (site != null) {
						 sites.add(site);
					}
			   } catch (SQLException e) {
					e.printStackTrace();
			   }
		  });
		  return sites;
	 }

	 @Override
	 public List<SiteSubmission> getSiteSubmissions() throws SQLException {
		  List<SiteSubmission> siteSubmissions = new ArrayList<>();
		  for (ActionTaken actionTaken : ActionTaken.values())
			   siteSubmissions.addAll(getSiteSubmissionsByAction(actionTaken));
		  return siteSubmissions;
	 }

	 @Override
	 public List<TourSubmission> getTourSubmissions() throws SQLException {
		  List<TourSubmission> tourSubmissions = new ArrayList<>();
		  for (ActionTaken actionTaken : ActionTaken.values())
			   tourSubmissions.addAll(getTourSubmissionsByAction(actionTaken));
		  return tourSubmissions;
	 }

	 @Override
	 public List<MapSubmissionContent> getMapSubmissions() throws SQLException {
		  List<MapSubmissionContent> mapSubmissions = new ArrayList<>();
		  for (ActionTaken actionTaken : ActionTaken.values())
			   mapSubmissions.addAll(getMapSubmissionsByAction(actionTaken));
		  return mapSubmissions;
	 }

	 public List<SiteSubmission> getSiteSubmissionsByAction(ActionTaken actionTaken) throws SQLException {
		  Status status = toStatus(actionTaken);
		  List<SiteSubmission> siteSubmissions = new ArrayList<>();
		  if (actionTaken == ActionTaken.ADD || actionTaken == ActionTaken.DELETE) {
			   List<List<Object>> tourIdsAndSiteIds = queryExecutor.selectColumnsByValue(
			             DatabaseMetaData.getTableName(Tables.tourSitesIdsAndDurance), "status",
			             DatabaseMetaData.getStatus(status), "tourId, siteId");
			   List<List<Object>> mapIdsAndSiteIds = queryExecutor.selectColumnsByValue(
			             DatabaseMetaData.getTableName(Tables.mapsSites), "status", DatabaseMetaData.getStatus(status),
			             "mapId, siteId");
			   List<List<Object>> cityIdsAndSiteIds = queryExecutor.selectColumnsByValue(
			             DatabaseMetaData.getTableName(Tables.citiesSitesIds), "status",
			             DatabaseMetaData.getStatus(status), "cityId, siteId");
			   Status statusToFetchBy = actionTaken == ActionTaken.ADD ? Status.ADD : Status.PUBLISH;
			   for (List<Object> list : tourIdsAndSiteIds) {
					int tourId = (int) list.get(0);
					Site site = getSite((int) list.get(1), statusToFetchBy);
					if (site != null)
						 siteSubmissions.add(new SiteSubmission(tourId, ObjectsEnum.TOUR, site, actionTaken));
			   }
			   for (List<Object> list : mapIdsAndSiteIds) {
					int tourId = (int) list.get(0);
					Site site = getSite((int) list.get(1), statusToFetchBy);
					if (site != null)
						 siteSubmissions.add(new SiteSubmission(tourId, ObjectsEnum.MAP, site, actionTaken));
			   }
			   for (List<Object> list : cityIdsAndSiteIds) {
					int tourId = (int) list.get(0);
					Site site = getSite((int) list.get(1), statusToFetchBy);
					if (site != null)
						 siteSubmissions.add(new SiteSubmission(tourId, ObjectsEnum.CITY, site, actionTaken));
			   }
		  } else if (actionTaken == ActionTaken.UPDATE) {
			   List<Site> sites = getSitesByStatus(status);
			   for (Site site : sites) {
					City city = getCityBySite(site.getId());
					if (city != null) {
						 siteSubmissions.add(new SiteSubmission(city.getId(), ObjectsEnum.CITY, site, actionTaken));
						 // system.err.println("~");
					}
			   }
		  }
		  return siteSubmissions;

	 }

	 public List<TourSubmission> getTourSubmissionsByAction(ActionTaken actionTaken) throws SQLException {
		  // citiesSites, mapSites, tourSites
		  Status status = toStatus(actionTaken);
		  List<TourSubmission> tourSubmissions = new ArrayList<>();
		  if (actionTaken == ActionTaken.ADD || actionTaken == ActionTaken.DELETE) {
			   List<List<Object>> mapIdsAndTourIds = queryExecutor.selectColumnsByValue(
			             DatabaseMetaData.getTableName(Tables.mapsTours), "status", DatabaseMetaData.getStatus(status),
			             "mapId, tourId");
			   List<List<Object>> cityIdsAndTourIds = queryExecutor.selectColumnsByValue(
			             DatabaseMetaData.getTableName(Tables.citiesTours), "status",
			             DatabaseMetaData.getStatus(status), "cityId, tourId");

			   Status statusToFetchBy = actionTaken == ActionTaken.ADD ? Status.ADD : Status.PUBLISH;

			   for (List<Object> list : mapIdsAndTourIds) {
					int tourId = (int) list.get(0);
					Tour site = getTour((int) list.get(1), statusToFetchBy);
					if (site != null)
						 tourSubmissions.add(new TourSubmission(tourId, ObjectsEnum.MAP, site, actionTaken));
			   }
			   for (List<Object> list : cityIdsAndTourIds) {
					int tourId = (int) list.get(0);
					Tour site = getTour((int) list.get(1), statusToFetchBy);
					if (site != null)
						 tourSubmissions.add(new TourSubmission(tourId, ObjectsEnum.CITY, site, actionTaken));
			   }
		  } else if (actionTaken == ActionTaken.UPDATE) {
			   List<Tour> tours = getToursByStatus(status);
			   for (Tour tour : tours) {
					City city = getCityByTourId(tour.getId());
					if (city != null)
						 tourSubmissions.add(new TourSubmission(city.getId(), ObjectsEnum.CITY, tour, actionTaken));
			   }
		  }
		  return tourSubmissions;

	 }

//  for retrieval each DELETE edit, fetch published. for each delete edit action: add relevant row in db. 
//	 for each discardment/approval - delete the specific row/delete the whole
//	  object existance.
	 @Override
	 public List<CitySubmission> getCitySubmissions() throws SQLException {
		  List<CitySubmission> citySubmissions = new ArrayList<>();
		  for (ActionTaken actionTaken : ActionTaken.values()) {
			   citySubmissions.addAll(getCitySubmissions(actionTaken));
		  }
		  return citySubmissions;
	 }

	 public List<CitySubmission> getCitySubmissions(ActionTaken actionTaken) throws SQLException {
		  Status status = toStatus(actionTaken);
		  List<CitySubmission> citySubmissions = new ArrayList<>();

		  if (actionTaken != ActionTaken.UPDATE) {
			   List<List<Object>> citiesEditionRows = queryExecutor.selectColumnsByValue(
			             DatabaseMetaData.getTableName(Tables.citiesMetaDetails), "status",
			             DatabaseMetaData.getStatus(status), "cityId");
			   // if DELETE, we want the published version of the map.
			   Status statusToFetchBy = actionTaken == ActionTaken.ADD ? Status.ADD : Status.PUBLISH;
			   for (List<Object> cityEdition : citiesEditionRows) {
					int cityId = (int) cityEdition.get(0);
					City city = getCityById(cityId, statusToFetchBy);
					if (city != null)
						 citySubmissions.add(new CitySubmission(city, actionTaken));
			   }
		  } else {
			   List<City> updatedCities = getCitiesByStatus(status);
			   for (City city : updatedCities) {
					if (city != null)
						 citySubmissions.add(new CitySubmission(city, actionTaken));
			   }
		  }
		  return citySubmissions;
	 }

	 public List<MapSubmissionContent> getMapSubmissionsByAction(ActionTaken actionTaken) throws SQLException {
		  Status status = toStatus(actionTaken);
		  List<MapSubmissionContent> mapSubmissions = new ArrayList<>();
		  if (actionTaken != ActionTaken.UPDATE) {
			   List<List<Object>> cityIdsAndMapIds = queryExecutor.selectColumnsByValue(
			             DatabaseMetaData.getTableName(Tables.citiesMapsIds), "status",
			             DatabaseMetaData.getStatus(status), "cityId, mapId");
			   // if DELETE, we want the published version of the map.
			   Status statusToFetchBy = actionTaken == ActionTaken.ADD ? Status.ADD : Status.PUBLISH;
			   for (List<Object> list : cityIdsAndMapIds) {
					int cityId = (int) list.get(0);
					int mapId = (int) list.get(1);
					Map map = getMapDetails(mapId, statusToFetchBy);
					byte[] file = getMapFile(mapId, statusToFetchBy);
					if (map != null && file != null)
						 mapSubmissions.add(new MapSubmissionContent(cityId, map, file, actionTaken));
			   }
		  } else {
			   List<Map> updatedMaps = getMapsByStatus(status);
			   for (Map map : updatedMaps) {
					City city = getCityByMapId(map.getId());
					byte[] mapFile = getMapFile(map.getId());
					if (city != null && mapFile != null)
						 mapSubmissions.add(new MapSubmissionContent(city.getId(), map, mapFile, actionTaken));
			   }
		  }
		  return mapSubmissions;
	 }

//	 private ActionTaken toAction(Status status) {
//		  switch (status) {
//			   case ADD:
//					return ActionTaken.ADD;
//			   case DELETE:
//					return ActionTaken.DELETE;
//			   case UPDATE:
//					return ActionTaken.UPDATE;
//			   default:
//					return null;
//		  }
//	 }

	 private Status toStatus(ActionTaken actionTaken) {
		  switch (actionTaken) {
			   case ADD:
					return Status.ADD;
			   case DELETE:
					return Status.DELETE;
			   case UPDATE:
					return Status.UPDATE;
			   default:
					return null;
		  }
	 }

	 private List<Tour> getToursByStatus(Status status) throws SQLException {
		  List<Integer> tourIds = toIdList(
		            queryExecutor.selectColumnsByValue(DatabaseMetaData.getTableName(Tables.toursMetaDetails), "status",
		                      DatabaseMetaData.getStatus(status), "*"));
		  List<Tour> tours = new ArrayList<>();
		  tourIds.forEach((tourId) -> {
			   try {
					Tour tour = getTour(tourId, status);
					if (tour != null)
						 tours.add(tour);
			   } catch (SQLException e) {
					e.printStackTrace();
			   }
		  });
		  return tours;
	 }

	 List<Map> getMapsObjectContainedIn(int objectId, Status status) {
		  List<Integer> mapsIds = new ArrayList<>();
		  return toMapsByIds(mapsIds, status);
	 }

	 List<Map> toMapsByIds(List<Integer> mapsIds, Status status) {
		  List<Map> mapsObjects = new ArrayList<>();
		  if (status != Status.UPDATE)
			   mapsIds.forEach((mapId) -> {
					try {
						 Map map = getMapDetails(mapId, status);
						 if (map != null)
							  mapsObjects.add(map);
					} catch (SQLException e) {
						 e.printStackTrace();
					}
			   });
		  else {
			   mapsIds.forEach((mapId) -> {
					try {
						 List<Map> maps = getMapsDetails(mapId, status);
						 if (!maps.isEmpty())
							  mapsObjects.addAll(maps);
					} catch (SQLException e) {
						 e.printStackTrace();
					}
			   });
		  }
		  return mapsObjects;
	 }

//	 private List<City> toCities(List<Integer> citiesIds, Status status) {
//		  List<City> cities = new ArrayList<>();
//		  citiesIds.forEach((cityId) -> {
//			   try {
//					cities.add(getCityById(cityId));
//			   } catch (SQLException e) {
//					e.printStackTrace();
//			   }
//		  });
//		  return cities;
//	 }

	 private List<Tour> getToursByIds(List<Integer> tourIds, Status status) {
		  List<Tour> tours = new ArrayList<>();
		  tourIds.forEach((tourId) -> {
			   try {
					Tour tour = getTour(tourId, status);
					if (tour != null) {
						 tours.add(tour);
					}
			   } catch (SQLException e) {
					e.printStackTrace();
			   }
		  });
		  return tours;
	 }

	 private int getSiteDurance(int tourId, int siteId) throws SQLException {
		  List<List<Object>> list = queryExecutor.selectColumnsByValue(
		            DatabaseMetaData.getTableName(Tables.tourSitesIdsAndDurance), "tourId, siteId",
		            new ArrayList<Object>() {
			             {
					          add(tourId);
					          add(siteId);
			             }
		            }, "siteDurance");
		  if (list.isEmpty())
			   return -1;
		  else
			   return (int) list.get(0).get(0);
	 }

	 // private void addSiteToMapsByStatus(int siteId, List<Map> maps, Status status)
	 // {
	 // maps.forEach((map) -> {
	 // try {
	 // addSiteToMapByStatus(map.getId(), siteId, status);
	 // } catch (SQLException e) {
	 // e.printStackTrace();
	 // }
	 // });
	 // }
	 //
	 // private void addSiteToToursByStatus(int siteId, int durance, List<Tour>
	 // tours, Status status) {
	 // tours.forEach((tour) -> {
	 // try {
	 // addSiteToTourByStatus(tour.getId(), siteId, durance, status);
	 // } catch (SQLException e) {
	 // e.printStackTrace();
	 // }
	 // });
	 // }

	 @Override
	 public void actionTourEdit(TourSubmission tourSubmission, boolean action) throws SQLException {
		  ActionTaken actionTaken = tourSubmission.getAction();
		  ObjectsEnum containingObjectType = tourSubmission.getContainingObjectType();
		  int containingId = tourSubmission.getContainingObjectID();
		  Tour tour = tourSubmission.getTour();
		  int tourId = tour.getId();
		  deleteFromTables(tour.getId(), toStatus(actionTaken));
		  Status publish = Status.PUBLISH;
		  if (actionTaken == ActionTaken.ADD) {
			   if (containingObjectType == ObjectsEnum.MAP) {
					deleteTourFromMap(containingId, tourId, Status.ADD);
					if (action) {
						 addExistingTourToMap(containingId, tourId, publish);
					}
			   } else if (containingObjectType == ObjectsEnum.CITY) {
					deleteTour(tourId);
					if (action) {
						 addNewTourToCity(containingId, tour, publish);
					}
			   }
		  } else if (actionTaken == ActionTaken.UPDATE) {
			   if (action) {
					updateTourDetails(tourId, tour);
			   } else {
					eraseTourUpdateEdit(tour);
			   }
		  } else {
			   if (action) {
					deleteTour(tourId);
			   } else {
					eraseDeleteEdit(containingId, containingObjectType, tourId, ObjectsEnum.TOUR);
			   }
		  }

	 }

	 private void updateTourDetails(int tourId, Tour tour) throws SQLException {
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.toursMetaDetails), "tourId", tourId);
		  List<Object> tourRow = objectParser.getTourMetaFieldsList(tour);
		  tourRow.set(0, tourId);
		  queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.toursMetaDetails), tourRow, Status.PUBLISH);
	 }

	 @Override
	 public List<User> actionMapEdit(MapSubmission mapSubmission, boolean action) throws SQLException {
		  List<User> usersHoldingMap = new ArrayList<>();
		  ActionTaken actionTaken = mapSubmission.getAction();
		  int containingId = mapSubmission.getContainingCityID();
		  Map map = mapSubmission.getMap();
		  File file = mapSubmission.getMapFile();
		  int mapId = map.getId();
		  usersHoldingMap = getUsersHoldingMap(mapId);
		  Status publish = Status.PUBLISH;
		  if (actionTaken == ActionTaken.ADD) {
			   delete(mapId);
			   if (action) {
					addMapToCityByStatus(containingId, map, file, publish);
			   }
		  } else if (actionTaken == ActionTaken.UPDATE) {
			   if (action) {
					updateMapDetails(mapId, map);
			   } else {
					eraseMapUpdateEdit(map);
			   }
		  } else {
			   if (action)
					deleteMap(mapId);
			   else
					eraseDeleteEdit(containingId, ObjectsEnum.CITY, mapId, ObjectsEnum.MAP);
		  }
//		  return delete(id);
//   }else
//
//	 {
//		  if (actionTaken == ActionTaken.UPDATE) {
//			   eraseMapUpdate(map);
//		  } else
//			   deleteFromTables(id);
//	 }
		  return usersHoldingMap;
	 }

	 private void eraseMapUpdateEdit(Map map) throws SQLException {
		  List<Object> mapRow = objectParser.getMapMetaFieldsList(map);
		  List<String> mapColumnsNames = objectParser.getMapMetaFieldsNames();
		  queryExecutor.deleteValuesFromTable(DatabaseMetaData.getTableName(Tables.mapsMetaDetails), mapColumnsNames,
		            mapRow, Status.UPDATE);
	 }

	 private void eraseCityUpdateEdit(City city) throws SQLException {
		  List<Object> row = objectParser.getCityFields(city);
		  List<String> columnsNames = objectParser.getCityMetaFieldsNames();
		  queryExecutor.deleteValuesFromTable(DatabaseMetaData.getTableName(Tables.citiesMetaDetails), columnsNames,
		            row, Status.UPDATE);
	 }

	 private void eraseTourUpdateEdit(Tour tour) throws SQLException {
		  List<Object> row = objectParser.getTourMetaFieldsList(tour);
		  List<String> columnsNames = objectParser.getTourMetaFieldsNames();
		  queryExecutor.deleteValuesFromTable(DatabaseMetaData.getTableName(Tables.toursMetaDetails), columnsNames, row,
		            Status.UPDATE);
	 }

	 private void eraseSiteUpdateEdit(Site site) throws SQLException {
		  List<Object> row = objectParser.getSiteFieldsList(site);
		  List<String> columnsNames = objectParser.getSiteFieldsNames();
		  queryExecutor.deleteValuesFromTable(DatabaseMetaData.getTableName(Tables.sites), columnsNames, row,
		            Status.UPDATE);
	 }

	 private void updateMapDetails(int id, Map map) throws SQLException {
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.mapsMetaDetails), "mapId", id);
		  List<Object> mapRow = objectParser.getMapMetaFieldsList(map);
		  mapRow.set(0, id);
		  queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.mapsMetaDetails), mapRow, Status.PUBLISH);
	 }

	 private List<User> getUsersHoldingMap(int id) throws SQLException {
		  List<User> users = new ArrayList<>();
		  List<List<Object>> lists = queryExecutor.selectColumnsByValue(
		            DatabaseMetaData.getTableName(Tables.mapsDownloadHistory), "mapId", id, "username");
		  for (List<Object> list : lists) {
			   String username = (String) list.get(0);
			   users.add(getUserDetails(username));
		  }
		  return users;
	 }

	 @Override
	 public void actionCityEdit(CitySubmission citySubmission, boolean action) throws SQLException {
		  ActionTaken actionTaken = citySubmission.getAction();
		  City city = citySubmission.getCity();
		  int cityId = city.getId();
		  if (actionTaken == ActionTaken.ADD) {
			   deleteCity(cityId, Status.ADD);
			   if (action)
					addCity(city, Status.PUBLISH, true);
		  } else if (actionTaken == ActionTaken.UPDATE) {
			   // prices aren't changed. the can be changed only by manager procedure
			   if (action)
					updateCityDetails(city);
			   else
					eraseCityUpdateEdit(city);
		  } else {
			   if (action) {
					deleteCity(cityId);
			   } else {
					eraseDeleteEdit(cityId, ObjectsEnum.CITY, cityId, ObjectsEnum.CITY);
			   }

		  }
	 }

	 /**
	  * function to use every time deletion action had benn disapproved (i.e, erase
	  * this specific pending delete edits in the right tables)
	  * 
	  * @param idToEraseFrom
	  * @param objectTypeToEraseFrom
	  * @param idToErase
	  * @param objectTypeToErase
	  * @throws SQLException
	  */
	 private void eraseDeleteEdit(int idToEraseFrom, ObjectsEnum objectTypeToEraseFrom, int idToErase,
	           ObjectsEnum objectTypeToErase) throws SQLException {
		  Status deleteStatus = Status.DELETE;
		  if (objectTypeToErase == ObjectsEnum.CITY)
			   queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.citiesMetaDetails), "cityId",
			             idToErase, deleteStatus);
		  else if (objectTypeToErase == ObjectsEnum.MAP) {
			   queryExecutor.deleteValuesFromTable(DatabaseMetaData.getTableName(Tables.citiesMapsIds),
			             Arrays.asList("cityId", "mapId"), Arrays.asList(idToEraseFrom, idToErase), deleteStatus);
		  } else if (objectTypeToErase == ObjectsEnum.TOUR) {
			   if (objectTypeToEraseFrom == ObjectsEnum.CITY) {
					queryExecutor.deleteValuesFromTable(DatabaseMetaData.getTableName(Tables.citiesTours),
					          Arrays.asList("cityId", "tourId"), Arrays.asList(idToEraseFrom, idToErase, deleteStatus));
			   } else if (objectTypeToEraseFrom == ObjectsEnum.MAP) {
					queryExecutor.deleteValuesFromTable(DatabaseMetaData.getTableName(Tables.mapsTours),
					          Arrays.asList("mapId", "tourId"), Arrays.asList(idToEraseFrom, idToErase, deleteStatus));
			   }
		  } else if (objectTypeToErase == ObjectsEnum.SITE) {
			   if (objectTypeToEraseFrom == ObjectsEnum.CITY) {
					queryExecutor.deleteValuesFromTable(DatabaseMetaData.getTableName(Tables.citiesSitesIds),
					          Arrays.asList("cityId", "siteId"), Arrays.asList(idToEraseFrom, idToErase, deleteStatus));
			   } else if (objectTypeToEraseFrom == ObjectsEnum.MAP) {
					queryExecutor.deleteValuesFromTable(DatabaseMetaData.getTableName(Tables.mapsSites),
					          Arrays.asList("mapId", "siteId"), Arrays.asList(idToEraseFrom, idToErase, deleteStatus));
			   } else if (objectTypeToEraseFrom == ObjectsEnum.TOUR) {
					queryExecutor.deleteValuesFromTable(DatabaseMetaData.getTableName(Tables.tourSitesIdsAndDurance),
					          Arrays.asList("tourId", "siteId"), Arrays.asList(idToEraseFrom, idToErase, deleteStatus));
			   }
		  }
	 }

	 private void updateCityDetails(City city) throws SQLException {
		  int cityId = city.getId();
		  deleteFromTables(cityId);
		  City publishedCity = getCityById(cityId);
		  city.setPrices(publishedCity.getPrices());
		  addCity(city, Status.PUBLISH, false);
	 }

	 @Override
	 public void actionSiteEdit(SiteSubmission siteSubmission, boolean action) throws SQLException {
		  ActionTaken actionTaken = siteSubmission.getAction();
		  ObjectsEnum objectsEnum = siteSubmission.getContainingObjectType();
		  int containingId = siteSubmission.getContainingObjectID();
		  Site site = siteSubmission.getSite();
		  int siteId = site.getId();
		  Status publish = Status.PUBLISH;
		  if (actionTaken == ActionTaken.ADD) {
			   if (objectsEnum == ObjectsEnum.TOUR) {
					deleteSiteFromTour(containingId, siteId);
					if (action) {
						 addSiteToTourByStatus(containingId, siteId, getSiteDurance(containingId, siteId), publish);
					}
			   } else if (objectsEnum == ObjectsEnum.MAP) {
					deleteSiteFromMap(containingId, siteId);
					if (action) {
						 addSiteToMapByStatus(containingId, siteId, publish);
					}
			   } else if (objectsEnum == ObjectsEnum.CITY) {
					deleteSite(siteId);
					if (action) {
						 addNewSiteToCityByStatus(containingId, site, publish);
					}
			   }
		  } else if (actionTaken == ActionTaken.UPDATE) {
			   if (action) {
					updateSiteDetails(siteId, site);
			   } else {
					eraseSiteUpdateEdit(site);
			   }
		  } else if (actionTaken == ActionTaken.DELETE) {
			   if (action) {
					deleteSite(siteId);
			   } else {
					eraseDeleteEdit(containingId, objectsEnum, siteId, ObjectsEnum.SITE);
			   }
		  }
	 }

	 @Override
	 public void updateSite(int siteId, Site site) throws SQLException {
		  updateSiteDetails(siteId, site);
	 }

	 private void updateSiteDetails(int siteId, Site site) throws SQLException {
		  queryExecutor.deleteValueFromTable(DatabaseMetaData.getTableName(Tables.sites), "siteId", siteId);
		  List<Object> siteRow = objectParser.getSiteFieldsList(site);
		  siteRow.set(0, siteId);
		  queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.sites), siteRow, Status.PUBLISH);
	 }

	 @Override
	 public void editCityPrice(int cityId, double newPrice) throws SQLException {
		  // TODO Auto-generated method stub
	 }

//	 @Override
//	 public double getMembershipPrice(int cityId, int timeInterval, String username) throws SQLException {
//		  // need to check if he buy that map befor -> 10% disscount
//
//		  List<List<Object>> checkForDisscount = queryExecutor.selectColumnsByValue("purchaseDeatailsHistory",
//		            "username", username, "purchaseDate");
//		  List<List<Object>> list = queryExecutor.selectColumnsByValue("citysesPrices", "cityId", cityId,
//		            "Month" + timeInterval);
//		  if (list.isEmpty()) {
//			   return -1;
//		  }
//
//		  if (checkForDisscount.isEmpty()) {
//			   return (double) list.get(0).get(0);
//		  } else {
//			   return (double) list.get(0).get(0) * 0.9;
//		  }
//	 }

	 @Override
	 public boolean repurchaseMembershipBySavedDetails(int cityId, int timeInterval, String username)
	           throws SQLException {

		  // need to check if patment is good -> nevr happen

		  // subsciption
		  if (timeInterval > 0) {
			   List<Object> pDetails = new ArrayList<Object>() {
					{

						 int days = 30 * timeInterval;
						 java.sql.Date startDate = new java.sql.Date(Calendar.getInstance().getTime().getTime());
						 java.sql.Date endDate = addDays(startDate, days);
						 add(username);
						 add(cityId);
						 add(startDate);
						 add(false);
						 add(timeInterval);
						 add(endDate);
					}
			   };
			   try {
					String columnToUpdate = "subscribes";
					queryExecutor.insertToTable("purchaseDeatailsHistory", pDetails);

					notifyManagerReportColumn(cityId, columnToUpdate);

			   } catch (SQLException e) {
					return false;
			   }
		  } else {
			   // oneTimePurchase
			   List<Object> pDetails = new ArrayList<Object>() {
					{

						 int days = 30 * timeInterval;
						 java.sql.Date startDate = new java.sql.Date(Calendar.getInstance().getTime().getTime());
						 java.sql.Date endDate = addDays(startDate, days);
						 add(username);
						 add(cityId);
						 add(startDate);
						 add(true);
						 add(timeInterval);
						 add(endDate);
					}
			   };
			   try

			   {
					String tableUPDATE = "oneTimePurchase";
					queryExecutor.insertToTable("purchaseDeatailsHistory", pDetails);
					notifyManagerReportColumn(cityId, tableUPDATE);
			   } catch (SQLException e) {
					return false;
			   }

		  }

		  // if seccuss
		  return true;
	 }

	 @Override
	 public byte[] downloadMap(int mapId, String username) throws SQLException {
		  City city = getCityByMapId(mapId);
		  if (city != null && verifyPurchasedCity(username, city.getId())) {
			   byte[] mapFile = getMapFile(mapId, Status.PUBLISH);
			   queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.mapsDownloadHistory),
			             new ArrayList<Object>() {
					          {
						           add(username);
						           add(mapFile);
						           add(new java.util.Date().toInstant());
					          }
			             });
			   return mapFile;
		  } else
			   return null;
	 }

	 @Override
	 public List<Map> getPurchasedMaps(String username) throws SQLException {
		  // having list of all the purchase cityId that the user bought
		  List<List<Object>> cityIdList = queryExecutor.selectColumnsByValue("purchaseDeatails", "cityId", username,
		            "cityId");
		  List<Integer> cityId = toIdList(cityIdList);

		  String tableUPDATE = "downloads";
		  // getting all the maps id
		  List<Map> maps = new ArrayList<>();
		  List<List<Object>> mapsIdList = new ArrayList<List<Object>>();

		  for (int i : cityId) {
			   mapsIdList.addAll(queryExecutor.selectColumnsByValue("citiesMaps", "cityId", i, "mapId"));
			   notifyManagerReportColumn(i, tableUPDATE);

		  }
		  List<Integer> mapsId = toIdList(mapsIdList);
		  for (int i : mapsId) {
			   maps.add(getMapDetails(i));
		  }

		  return maps;
	 }

//	 @Override
//	 public double getOneTimePurchasePrice(int cityId) throws SQLException {
//
//		  List<List<Object>> list = queryExecutor.selectColumnsByValue("citysesPrices", "cityId", cityId,
//		            "oneTimePurchase");
//		  if (list.isEmpty()) {
//			   return -1;
//		  }
//
//		  return (double) list.get(0).get(0);
//	 }

	 @Override
	 public String getSavedCreditCard(String username) throws SQLException {
		  List<List<Object>> list = queryExecutor.selectColumnsByValue("costumerPurchaseDeatils", "username", username,
		            "creditCard");
		  if (list.isEmpty()) {
			   return "";
		  }
		  String res = (String) list.get(0).get(0);
		  res = "XXXX-XXXX-XXXX-" + res.substring(res.length() - 4);
		  return res;
	 }

	 @Override
	 public boolean purchaseCity(int cityId, int timeInterval, PurchaseDetails purchaseDetails, String username)
	           throws SQLException {
		  // if seccess -> validate payment (not really can happen)

		  // update user purchaseDetails in his table , update report table

		  List<List<Object>> checkIfAlreadyExistUser = queryExecutor.selectColumnsByValue("purchaseDeatailsHistory",
		            "username", username, "purchaseDate");
		  if (checkIfAlreadyExistUser.isEmpty()) {
			   List<Object> cotumerPurchaseDetails = new ArrayList<Object>() {
					{
						 add(username);
						 add(purchaseDetails.getFirstname());
						 add(purchaseDetails.getLastname());
						 add(purchaseDetails.getCreditCard());
						 add(purchaseDetails.getCvv());
						 add(purchaseDetails.getCardExpireDate());
						 add(purchaseDetails.getCardOwnerIdString());
					}
			   };
			   queryExecutor.insertToTable("costumerPurchaseDetails", cotumerPurchaseDetails);
		  }

		  // update purchaseDeatailsHistory so can know all purchase history
		  if (timeInterval > 0) {
			   List<Object> pDetails = new ArrayList<Object>() {
					{

						 int days = 30 * timeInterval;
						 java.sql.Date startDate = new java.sql.Date(Calendar.getInstance().getTime().getTime());
						 java.sql.Date endDate = addDays(startDate, days);
						 add(username);
						 add(cityId);
						 add(startDate);
						 add(false);
						 add(timeInterval);
						 add(endDate);
					}
			   };
			   try

			   {
					String columnToUpdate = "downloads";
					queryExecutor.insertToTable("purchaseDeatailsHistory", pDetails);
					notifyManagerReportColumn(cityId, columnToUpdate);

			   } catch (SQLException e) {
					return false;
			   }
		  } else

		  {
			   // oneTimePurchase
			   List<Object> pDetails = new ArrayList<Object>() {
					{

						 int days = 30 * timeInterval;
						 java.sql.Date startDate = new java.sql.Date(Calendar.getInstance().getTime().getTime());
						 java.sql.Date endDate = addDays(startDate, days);
						 add(username);
						 add(cityId);
						 add(startDate);
						 add(true);
						 add(timeInterval);
						 add(endDate);
					}
			   };
			   try {
					String tableUPDATE = "oneTimePurchase";
					queryExecutor.insertToTable("purchaseDeatailsHistory", pDetails);

					notifyManagerReportColumn(cityId, tableUPDATE);
			   } catch (

			   SQLException e) {
					return false;
			   }

		  }

		  // if seccuss
		  return true;
	 }

	 @Override
	 public List<byte[]> purchaseCityOneTime(int cityId, PurchaseDetails purchaseDetails, String username)
	           throws SQLException {

		  int timeInterval = 0;

		  // validate details and insert to costumerpurchasedtails table
		  List<List<Object>> checkIfAlreadyExistUser = queryExecutor.selectColumnsByValue("purchaseDeatailsHistory",
		            "username", username, "purchaseDate");
		  if (checkIfAlreadyExistUser.isEmpty()) {
			   List<Object> cotumerPurchaseDetails = new ArrayList<Object>() {
					{
						 add(username);
						 add(purchaseDetails.getFirstname());
						 add(purchaseDetails.getLastname());
						 add(purchaseDetails.getCreditCard());
						 add(purchaseDetails.getCvv());
						 add(purchaseDetails.getCardExpireDate());
					}
			   };
			   try {
					queryExecutor.insertToTable("costumerPurchaseDetails", cotumerPurchaseDetails);
			   } catch (SQLException e) {
					// else give null
					return null;
			   }
		  }
		  // need to update purchaseDeatails table and mangerReports

		  // give list of mapsId that belong to that cityId
		  List<List<Object>> mapsIdList = queryExecutor.selectColumnsByValue("citiesMaps", "cityId", cityId, "mapId");
		  List<Integer> mapsid = toIdList(mapsIdList);

		  List<byte[]> files = new ArrayList<>();

		  for (int i : mapsid) {
			   files.add(getMapFile(i));
		  }

		  // update purchase history
		  List<Object> pDetails = new ArrayList<Object>() {
			   {
					int days = 30 * timeInterval;
					java.sql.Date startDate = new java.sql.Date(Calendar.getInstance().getTime().getTime());
					java.sql.Date endDate = addDays(startDate, days);
					add(username);
					add(cityId);
					add(startDate);
					add(true);
					add(0);
					add(endDate);
			   }
		  };
		  try {

			   String tableUPDATE = "oneTimePurchase";
			   queryExecutor.insertToTable("purchaseDeatailsHistory", pDetails);

			   notifyManagerReportColumn(cityId, tableUPDATE);
		  } catch (

		  SQLException e) {
			   return null;
		  }

		  return files;
	 }

	 @Override
	 public boolean notifyMapView(String username, int mapId) throws SQLException {
		  City city = getCityByMapId(mapId);
		  if (city != null && verifyPurchasedCity(username, city.getId())) {
			   String columnToUpdate = "viewsNum";
			   notifyManagerReportColumn(city.getId(), columnToUpdate);
			   return true;
		  } else
			   return false;
	 }

	 private boolean verifyPurchasedCity(String username, int cityId) throws SQLException {
		  List<List<Object>> rows = queryExecutor.selectColumnsByValues(
		            DatabaseMetaData.getTableName(Tables.purchaseHistory), new ArrayList<String>() {
			             {
					          add("username");
					          add("cityId");
			             }
		            }, new ArrayList<Object>() {
			             {
					          add(username);
					          add(cityId);
			             }
		            }, "*");
		  return !rows.isEmpty();
	 }

	 @Override
	 public List<PurchaseHistory> getPurchaseHistory(String username) throws SQLException {
		  // getting username purchase history
		  List<List<Object>> history = queryExecutor.selectColumnsByValue("purchaseDeatailsHistory", "username",
		            username, "*");

		  List<PurchaseHistory> purchases = new ArrayList<>();
		  // converting it to PurchaseHistory objects that contains - city id, start date
		  // , end date
		  for (int i = 0; i < history.size(); i++) {
			   try {
					PurchaseHistory purchaseHistory = new PurchaseHistory((Date) history.get(i).get(2),
					          (Date) history.get(i).get(5), getCityById((int) history.get(i).get(1)));
					purchases.add(purchaseHistory);
			   } catch (Exception e) {
			   }
		  }

		  return purchases;
	 }

	 @Override
	 public Report getCityReport(java.sql.Date startDate, java.sql.Date endDate, String cityName) throws SQLException {
		  int cityId = getCityIdByName(cityName);
//		  return createMangerReportOnOneCity(cityId, dateToSqlDate(startDate), dateToSqlDate(endDate));
		  return createMangerReportOnOneCity(cityId, startDate, endDate);
	 }

//	 private java.sql.Date dateToSqlDate(java.util.Date date) {
//		  date = Calendar.getInstance().getTime();
//		  DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
//		  String strDate = dateFormat.format(date);
//		  return Date.valueOf(strDate);
//	 }

	 @Override
	 public List<Report> getAllcitiesReport(java.sql.Date startDate, java.sql.Date endDate) throws SQLException {

		  String tableName = "mangerReports";
		  String columnCondition = "occurrenceDate";
		  List<List<Object>> cityIdList = queryExecutor.selectAllColumns("citiesMetaDetails", "cityId");
		  List<Report> allCitiesreports = new ArrayList<>();
		  for (int i = 0; i < cityIdList.size(); i++) {
			   List<List<Object>> list = queryExecutor.betweenDates(tableName, "*", startDate, columnCondition,
			             endDate);

			   if (!list.isEmpty()) {
//					Report report = createMangerReportOnOneCity((int) cityIdList.get(i).get(0),
//					          dateToSqlDate(startDate), dateToSqlDate(endDate));
					Report report = createMangerReportOnOneCity((int) cityIdList.get(i).get(0), startDate, endDate);
					if (emptyfields(report)) {
						 allCitiesreports.add(report);
					}
			   }
		  }

		  return allCitiesreports;
	 }

	 @Override
	 public List<Report> getUserReports(java.sql.Date startDate, java.sql.Date endDate, String username)
	           throws SQLException {
		  List<Report> reportsOnUser = new ArrayList<>();
//		  java.sql.Date sqlStartDate = dateToSqlDate(startDate);
//		  java.sql.Date sqlEndDate = dateToSqlDate(endDate);
		  java.sql.Date sqlStartDate = startDate;
		  java.sql.Date sqlEndDate = endDate;
		  List<List<Object>> list = queryExecutor.betweenDatesAndCondition("mangerReports", "*", startDate,
		            "occuranceDate", endDate, "username", username);

		  List<Integer> cityIdList = toIdList(list);

		  cityIdList.forEach((cityId) -> {
			   try {
					reportsOnUser.add(createManagerReportOnOneCity(cityId, username, sqlStartDate, sqlEndDate));
			   } catch (SQLException e) {
					e.printStackTrace();
			   }
		  });
		  return reportsOnUser;
	 }

	 @Override
	 public UserReport getUserReport(java.sql.Date startDate, java.sql.Date endDate, String username)
	           throws SQLException {
		  User user = getUserDetails(username);
		  List<PurchaseHistory> purchaseHistory = getPurchaseHistory(username);
		  UserType userType = getUserType(username);
		  return new UserReport(user, userType, purchaseHistory);
	 }

	 private UserType getUserType(String username) throws SQLException {
		  if (!queryExecutor.selectColumnsByValue(DatabaseMetaData.getTableName(Tables.customerUsers), "username",
		            username, "*").isEmpty())
			   return UserType.CUSTOMER;
		  else if (!queryExecutor
		            .selectColumnsByValue(DatabaseMetaData.getTableName(Tables.editorUsers), "username", username, "*")
		            .isEmpty())
			   return UserType.EDITOR;
		  else if (!queryExecutor.selectColumnsByValue(DatabaseMetaData.getTableName(Tables.contentManagerUsers),
		            "username", username, "*").isEmpty())
			   return UserType.CONTENT_MANAGER;
		  else if (!queryExecutor.selectColumnsByValue(DatabaseMetaData.getTableName(Tables.generalManagerUsers),
		            "username", username, "*").isEmpty())
			   return UserType.GENERAL_MANAGER;
		  else
			   return null;
	 }

	 private void updateMangerReports(int cityId, String tableUPDATE) throws SQLException {

		  queryExecutor.updateTableColumn("mangerReports", tableUPDATE, 1, "cityId", cityId);

	 }

	 private void notifyManagerReportColumn(int cityId, String tableToUpdate) throws SQLException {

		  String cityName = getCityName(cityId);

		  List<Object> objects = new ArrayList<Object>() {
			   {
					java.sql.Date date = new java.sql.Date(Calendar.getInstance().getTime().getTime());
					add(cityId);
					add(cityName);
					add(0);
					add(0);
					add(0);
					add(0);
					add(0);
					add(date);

			   }
		  };

		  queryExecutor.insertToTable("mangerReports", objects);
		  updateMangerReports(cityId, tableToUpdate);

	 }

	 private String getCityName(int cityId) throws SQLException {

		  String tableName = "citiesMetaDetails";
		  String columnsToSelect = "*";
		  String objectName = "cityId";
		  String cityName;

		  List<List<Object>> list = queryExecutor.selectColumnsByValue(tableName, objectName, cityId, columnsToSelect);

		  if (list.isEmpty()) {
			   return "";
		  } else {
			   cityName = (String) list.get(0).get(1);
			   return cityName;
		  }
	 }

	 private int getCityIdByName(String cityName) throws SQLException {

		  String tableName = "citiesMetaDetails";
		  String columnsToSelect = "cityId";
		  String objectName = "cityName";
		  int cityId = -1;

		  List<List<Object>> list = queryExecutor.selectColumnsByValue(tableName, objectName, cityName,
		            columnsToSelect);

		  if (!list.isEmpty()) {
			   cityId = (int) list.get(0).get(0);
		  }
		  return cityId;
	 }

	 private Date addDays(Date date, int days) {
		  Calendar c = Calendar.getInstance();
		  c.setTime(date);
		  c.add(Calendar.DATE, days);
		  return new Date(c.getTimeInMillis());
	 }

	 @Override
	 public List<Tour> getCityTours(int cityId) throws SQLException {
		  return getCityTours(cityId, Status.PUBLISH);
	 }

	 public List<Tour> getCityTours(int cityId, Status status) throws SQLException {
		  List<Integer> tourIds = toIdList(queryExecutor.selectColumnsByValue(
		            DatabaseMetaData.getTableName(Tables.citiesTours), "cityId", cityId, "tourId", status));
		  return getToursByIds(tourIds, status);
	 }

	 public City getCityByTourId(int tourId) throws SQLException {
		  City city = getCityByTourId(tourId, Status.PUBLISH);
		  if (city == null)
			   city = getCityByTourId(tourId, Status.ADD);
		  return city;
	 }

	 public City getCityByTourId(int tourId, Status status) throws SQLException {
		  List<List<Object>> lists = queryExecutor.selectColumnsByValue(
		            DatabaseMetaData.getTableName(Tables.citiesTours), "tourId", tourId, "cityId", status);
		  if (lists.isEmpty())
			   return null;
		  else
			   return getCityById((int) lists.get(0).get(0));
	 }

	 public City getCityBySite(int siteId) throws SQLException {
		  City city = getCityBySite(siteId, Status.PUBLISH);
		  return city;
	 }

	 public City getCityBySite(int siteId, Status status) throws SQLException {
		  List<List<Object>> lists = queryExecutor.selectColumnsByValue(
		            DatabaseMetaData.getTableName(Tables.citiesSitesIds), "siteId", siteId, "cityId", status);
		  if (lists.isEmpty())
			   return null;
		  else
			   return getCityById((int) lists.get(0).get(0));
	 }

	 @Override
	 public void changeCityPrices(int cityId, List<Double> prices) throws SQLException {
		  addCityWithPrices(getCityById(cityId), prices, Status.PRICE_UPDATE);
	 }

	 // public void addCityPrices(int cityId, List<Double> prices, Status status)
	 // throws SQLException {
	 // if (prices.size() == 7) {
	 // City city = getCityById(cityId);
	 // city.setPrices(prices);
	 // queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.citiesMetaDetails),
	 // objectParser.getCityFields(city), status);
	 // }
	 // }
	 public void addCityWithPrices(City publishedCity, List<Double> prices, Status status) throws SQLException {
		  if (prices.size() == 7) {
			   publishedCity.setPrices(prices);
			   queryExecutor.insertToTable(DatabaseMetaData.getTableName(Tables.citiesMetaDetails),
			             objectParser.getCityFields(publishedCity), status);
		  }
	 }

	 @Override
	 public List<PriceSubmission> getPriceSubmissions() throws SQLException {
		  List<PriceSubmission> priceSubmissions = new ArrayList<>();
		  List<City> cities = getCitiesByStatus(Status.PRICE_UPDATE);
		  for (City city : cities) {
			   City publishedCity = getCityById(city.getId());
			   priceSubmissions.add(new PriceSubmission(publishedCity.getId(), publishedCity.getName(),
			             publishedCity.getPrices(), city.getPrices()));
		  }
		  return priceSubmissions;
	 }

	 @Override
	 public void approveCityPrice(int cityId, List<Double> prices, boolean approve) throws SQLException {
		  deleteFromTables(cityId, Status.PRICE_UPDATE); // delete all price edits occurrences
		  if (approve) {
			   City publishedCity = getCityById(cityId);
			   deleteFromTables(cityId, Status.PUBLISH); // delete current city
			   addCityWithPrices(publishedCity, prices, Status.PUBLISH); // add the new city
		  }
	 }

	 private Report createMangerReportOnOneCity(int cityId, Date date1, Date date2) throws SQLException {
		  String cityName = getCityName(cityId);
		  Report report = new Report();
		  report.setCityId(cityId);
		  report.setCityName(cityName);
		  List<String> tableNames = new ArrayList<>();
		  tableNames.add("oneTimePurchase");
		  tableNames.add("subscribes");
		  tableNames.add("resubscribers");
		  tableNames.add("viewsNum");
		  tableNames.add("downloads");
		  int oneTimePurchase = 0, subscribes = 0, resubscribers = 0, viewsNum = 0, downloads = 0;
		  for (int i = 0; i < tableNames.size(); i++) {
			   List<List<Object>> list = queryExecutor.betweenDatesAnd2Conditions("mangerReports", "*", date1,
			             "occurrenceDate", date2, "cityId", tableNames.get(i), cityId, 1);
			   if (!list.isEmpty()) {
					if (tableNames.get(i).equals("oneTimePurchase")) {
						 oneTimePurchase = list.size();
					} else if (tableNames.get(i).equals("subscribes")) {
						 subscribes = list.size();
					} else if (tableNames.get(i).equals("resubscribers")) {
						 resubscribers = list.size();
					} else if (tableNames.get(i).equals("viewsNum")) {
						 viewsNum = list.size();
					} else if (tableNames.get(i).equals("downloads")) {
						 downloads = list.size();
					}
			   }
		  }
		  report.setOneTimePurchase(oneTimePurchase);
		  report.setSubscribes(subscribes);
		  report.setResubscribers(resubscribers);
		  report.setViewsNum(viewsNum);
		  report.setDownloads(downloads);
		  return report;

	 }

	 private Report createManagerReportOnOneCity(int cityId, String username, Date date1, Date date2)
	           throws SQLException {
		  String cityName = getCityName(cityId);
		  Report report = new Report();
		  report.setCityId(cityId);
		  report.setCityName(cityName);
		  List<String> tableNames = new ArrayList<>();
		  tableNames.add("oneTimePurchase");
		  tableNames.add("subscribes");
		  tableNames.add("resubscribers");
		  tableNames.add("viewsNum");
		  tableNames.add("downloads");
		  int oneTimePurchase = 0, subscribes = 0, resubscribers = 0, viewsNum = 0, downloads = 0;
		  for (int i = 0; i < tableNames.size(); i++) {
			   List<List<Object>> list = queryExecutor.betweenDatesAnd3Conditions("mangerReports", "*", date1,
			             "occurrenceDate", date2, "cityId", tableNames.get(i), "username", cityId, 1, username);
			   if (!list.isEmpty()) {
					if (tableNames.get(i).equals("oneTimePurchase")) {
						 oneTimePurchase = list.size();
					} else if (tableNames.get(i).equals("subscribes")) {
						 subscribes = list.size();
					} else if (tableNames.get(i).equals("resubscribers")) {
						 resubscribers = list.size();
					} else if (tableNames.get(i).equals("viewsNum")) {
						 viewsNum = list.size();
					} else if (tableNames.get(i).equals("downloads")) {
						 downloads = list.size();
					}
			   }
		  }
		  report.setOneTimePurchase(oneTimePurchase);
		  report.setSubscribes(subscribes);
		  report.setResubscribers(resubscribers);
		  report.setViewsNum(viewsNum);
		  report.setDownloads(downloads);
		  return report;

	 }

	 private boolean emptyfields(Report report) {

		  if (report.getDownloads() == 0 && report.getOneTimePurchase() == 0 && report.getResubscribers() == 0
		            && report.getSubscribes() == 0 && report.getViewsNum() == 0) {
			   return false;
		  }
		  return true;
	 }

}
