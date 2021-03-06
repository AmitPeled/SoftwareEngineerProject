package database.metadata;

import java.util.HashMap;

import database.objectParse.Status;

public class DatabaseMetaData {
	 private static final String            host        = "remotemysql.com";
	 private static final String            DBName      = "X6SgPM1fb2";
	 private static final String            username    = "X6SgPM1fb2";
	 @SuppressWarnings("serial")
	 private static HashMap<Tables, String> tablesNames = new HashMap<Tables, String>() {
															 {
																  put(Tables.customerUsers, "customerUsers");
																  put(Tables.editorUsers, "editorUsers");
																  put(Tables.contentManagerUsers,
																            "contentManagerUsers");
																  put(Tables.generalManagerUsers,
																            "generalManagerUsers");

																  put(Tables.mapsMetaDetails, "mapsMetaDetails");
																  put(Tables.mapsFiles, "mapsFiles");
																  put(Tables.mapsSites, "mapsSites");
																  put(Tables.citiesMetaDetails, "citiesMetaDetails");
																  put(Tables.citiesMapsIds, "citiesMaps");
																  put(Tables.citiesSitesIds, "citiesSites");
																  put(Tables.sites, "sites");
																  put(Tables.tourSitesIdsAndDurance, "toursSites");
																  put(Tables.citiesTours, "citiesTours");
																  put(Tables.toursMetaDetails, "toursMetaDetails");
																  put(Tables.mapsTours, "mapsTours");
																  put(Tables.purchaseHistory,
																            "purchaseDeatailsHistory");
																  put(Tables.mapsDownloadHistory,
																            "mapsDownloadHistory");
																  put(Tables.customerPurchaseDetails,
																            "costumerPurchaseDetails");
															 }
														};

	 public static enum Tables {
		  customerUsers, editorUsers, contentManagerUsers, generalManagerUsers, mapsMetaDetails, mapsFiles, mapsSites,
		  citiesMetaDetails, citiesMapsIds, citiesSitesIds, sites, toursMetaDetails, tourSitesIdsAndDurance, mapsTours,
		  citiesTours, purchaseHistory, mapsDownloadHistory, customerPurchaseDetails
	 }

	 public static String getHostName() {
		  return host;
	 }

	 public static String getDbName() {
		  return DBName;
	 }

	 public static String getDbUsername() {
		  return username;
	 }

	 public static String getTableName(Tables table) {
		  return tablesNames.get(table);
	 }

	 public static int getStatus(Status status) {
		  switch (status) {
			   case PUBLISH:
					return 0;
			   case ADD:
					return 1;
			   case UPDATE:
					return 2;
			   case DELETE:
					return 3;
			   case PRICE_UPDATE:
					return 4;
			   default:
					System.err.println("bad status value");
					return -1;
		  }
	 }

	 public static int getTableColumnsSize(Tables table) {
		  switch (table) {
			   case citiesMetaDetails:
					return 10;
			   default:
					return 0;
		  }

	 }
}
