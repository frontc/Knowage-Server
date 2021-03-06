/*
 * Knowage, Open Source Business Intelligence suite
 * Copyright (C) 2016 Engineering Ingegneria Informatica S.p.A.
 *
 * Knowage is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Knowage is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.eng.spagobi.api.v2;

import it.eng.spago.error.EMFUserError;
import it.eng.spago.security.IEngUserProfile;
import it.eng.spagobi.api.AbstractSpagoBIResource;
import it.eng.spagobi.commons.constants.SpagoBIConstants;
import it.eng.spagobi.commons.dao.DAOFactory;
import it.eng.spagobi.commons.utilities.messages.MessageBuilder;
import it.eng.spagobi.services.rest.annotations.ManageAuthorization;
import it.eng.spagobi.services.rest.annotations.UserConstraint;
import it.eng.spagobi.services.serialization.JsonConverter;
import it.eng.spagobi.tools.datasource.bo.DataSource;
import it.eng.spagobi.tools.datasource.bo.DataSourceModel;
import it.eng.spagobi.tools.datasource.bo.IDataSource;
import it.eng.spagobi.tools.datasource.dao.IDataSourceDAO;
import it.eng.spagobi.utilities.exceptions.SpagoBIRestServiceException;
import it.eng.spagobi.utilities.exceptions.SpagoBIServiceException;


import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.json.JSONException;
import org.json.JSONObject;

import com.mongodb.DB;
import com.mongodb.MongoClient;

import it.eng.spago.error.EMFUserError;
import it.eng.spago.security.IEngUserProfile;
import it.eng.spagobi.api.AbstractSpagoBIResource;
import it.eng.spagobi.commons.constants.SpagoBIConstants;
import it.eng.spagobi.commons.dao.DAOFactory;
import it.eng.spagobi.services.rest.annotations.UserConstraint;
import it.eng.spagobi.services.serialization.JsonConverter;
import it.eng.spagobi.tools.datasource.bo.DataSource;
import it.eng.spagobi.tools.datasource.bo.DataSourceModel;
import it.eng.spagobi.tools.datasource.bo.IDataSource;
import it.eng.spagobi.tools.datasource.dao.IDataSourceDAO;
import it.eng.spagobi.utilities.exceptions.SpagoBIRestServiceException;
import it.eng.spagobi.utilities.exceptions.SpagoBIServiceException;

@Path("/2.0/datasources")
public class DataSourceResource extends AbstractSpagoBIResource {

	public static final String SERVICE_NAME = "2.0/datasources/";

	static protected Logger logger = Logger.getLogger(DataSourceResource.class);

	IDataSourceDAO dataSourceDAO;
	DataSource dataSource;
	List<DataSource> dataSourceList;

	@SuppressWarnings("unchecked")
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@UserConstraint(functionalities = { SpagoBIConstants.DATASOURCE_READ })
	public List<DataSource> getAllDataSources() {

		logger.debug("IN");

		try {

			dataSourceDAO = DAOFactory.getDataSourceDAO();
			dataSourceDAO.setUserProfile(getUserProfile());
			dataSourceList = dataSourceDAO.loadAllDataSources();

			return dataSourceList;

		} catch (Exception exception) {

			logger.error("Error while getting the list of DS", exception);
			throw new SpagoBIRestServiceException("Error while getting the list of DS", buildLocaleFromSession(), exception);

		} finally {

			logger.debug("OUT");

		}
	}

	@GET
	@Path("/{dsId}")
	@Produces(MediaType.APPLICATION_JSON)
	@UserConstraint(functionalities = { SpagoBIConstants.DATASOURCE_READ })
	public String getDataSourceById(@PathParam("dsId") Integer dsId) {

		logger.debug("IN");

		try {

			dataSourceDAO = DAOFactory.getDataSourceDAO();
			dataSourceDAO.setUserProfile(getUserProfile());
			dataSource = dataSourceDAO.loadDataSourceByID(dsId);

			return JsonConverter.objectToJson(dataSource, null);

		} catch (Exception e) {

			logger.error("Error while loading a single data source", e);
			throw new SpagoBIRestServiceException("Error while loading a single data source", buildLocaleFromSession(), e);

		} finally {

			logger.debug("OUT");

		}
	}

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@UserConstraint(functionalities = { SpagoBIConstants.DATASOURCE_MANAGEMENT })
	public String postDataSource(DataSource dataSource) {

		logger.debug("IN");

		try {
			logger.debug(dataSource.toString());
			logger.debug(dataSource);
			dataSourceDAO = DAOFactory.getDataSourceDAO();
			dataSourceDAO.setUserProfile(getUserProfile());
			dataSourceDAO.insertDataSource(dataSource, getUserProfile().getOrganization());

			DataSource newLabel = (DataSource) dataSourceDAO.loadDataSourceByLabel(dataSource.getLabel());
			int newId = newLabel.getDsId();

			return Integer.toString(newId);

		} catch (Exception exception) {

			logger.error("Error while posting DS", exception);
			throw new SpagoBIRestServiceException("Error while posting DS", buildLocaleFromSession(), exception);

		} finally {

			logger.debug("OUT");

		}
	}

	@PUT
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@UserConstraint(functionalities = { SpagoBIConstants.DATASOURCE_MANAGEMENT })
	public List<DataSource> putDataSource(DataSourceModel dataSource) {

		logger.debug("IN");

		try {

			dataSourceDAO = DAOFactory.getDataSourceDAO();

			dataSourceDAO.setUserProfile(getUserProfile());
			IDataSource oldDataSource = dataSourceDAO.loadDataSourceWriteDefault();

			if (oldDataSource != null && dataSource.getWriteDefault() && oldDataSource.getDsId() != dataSource.getDsId()) {
				// unset the cache
				// SpagoBICacheManager.removeCache();
				oldDataSource.setWriteDefault(false);
				dataSourceDAO.modifyDataSource(oldDataSource);
			}
			dataSourceDAO.modifyDataSource(dataSource);
			return DAOFactory.getDataSourceDAO().loadAllDataSources();

		} catch (Exception e) {

			logger.error("Error while updating data source", e);
			throw new SpagoBIRestServiceException("Error while updating data source", buildLocaleFromSession(), e);

		} finally {

			logger.debug("OUT");

		}
	}

	@DELETE
	@Path("/{dsId}")
	@UserConstraint(functionalities = { SpagoBIConstants.DATASOURCE_MANAGEMENT })
	public List<DataSource> deleteDataSourceById(@PathParam("dsId") Integer dsId) throws EMFUserError {

		logger.debug("IN");

		// if the ds is associated with any BIEngine or BIObjects, creates an error
		Map<String, List<String>> objNames = DAOFactory.getDataSourceDAO().returnEntitiesAssociated(dsId);

		if (objNames.size() > 0) {
			logger.warn("datasource is in use, build message");

			String[] dependsBy = new String[] { "sbi.datasource.usedby.biobject", "sbi.datasource.usedby.metamodel", "sbi.datasource.usedby.dataset",
					"sbi.datasource.usedby.lov" };

			String message = "";
			MessageBuilder msgBuild = new MessageBuilder();
			Locale locale = buildLocaleFromSession();

			for (int j = 0; j < dependsBy.length; j++) {
				String key = dependsBy[j];
				String translatedKey = msgBuild.getMessage(key, locale);
				if (objNames.get(key) != null) {
					int i = 0;
					for (Iterator iterator = objNames.get(key).iterator(); iterator.hasNext();) {
						String objName = (String) iterator.next();
						if (i == 0) {
							message += translatedKey + " ( " + objName;
						} else {
							message += ", " + objName;
						}
						if(i == objNames.get(key).size()-1){
							message += ") ";
						}
						i++;
					}
					
					
					message += "\n";
				}
			}

			throw new SpagoBIRestServiceException(msgBuild.getMessage("sbi.datasource.usedby", locale) + message, locale, new Exception());
		}

		try {
			DataSource dataSource = new DataSource();
			dataSource.setDsId(dsId);
			dataSourceDAO = DAOFactory.getDataSourceDAO();
			dataSourceDAO.setUserProfile(getUserProfile());
			dataSourceDAO.eraseDataSource(dataSource);

			return DAOFactory.getDataSourceDAO().loadAllDataSources();

		} catch (Exception e) {

			logger.error("Error while deleting data source", e);
			throw new SpagoBIRestServiceException("Error while deleting data source", buildLocaleFromSession(), e);

		} finally {

			logger.debug("OUT");

		}
	}

	@DELETE
	@Path("/")
	@UserConstraint(functionalities = { SpagoBIConstants.DATASOURCE_MANAGEMENT })
	public List<DataSource> deleteMultiple(@QueryParam("id") List<Integer> ids) {

		logger.debug("IN");

		try {

			dataSourceDAO = DAOFactory.getDataSourceDAO();
			dataSourceDAO.setUserProfile(getUserProfile());

			for (int i = 0; i < ids.size(); i++) {
				DataSource ds = new DataSource();
				ds.setDsId(ids.get(i));
				dataSourceDAO.eraseDataSource(ds);
			}

			return dataSourceDAO.loadAllDataSources();

		} catch (Exception e) {

			logger.error("Error while deleting multiple data sources", e);
			throw new SpagoBIRestServiceException("Error while deleting multiple data sources", buildLocaleFromSession(), e);

		} finally {

			logger.debug("OUT");

		}
	}

	@GET
	@Path("/structure/{dsId}")
	@Produces(MediaType.APPLICATION_JSON)
	@UserConstraint(functionalities = { SpagoBIConstants.DATASOURCE_READ })
	public String getDataSourceStruct(@PathParam("dsId") Integer dsId) {

		logger.debug("IN");
		JSONObject tableContent = new JSONObject();
		try {

			dataSourceDAO = DAOFactory.getDataSourceDAO();
			dataSourceDAO.setUserProfile(getUserProfile());
			dataSource = dataSourceDAO.loadDataSourceByID(dsId);
			Connection conn = dataSource.getConnection();

			tableContent = getTableMetadata(conn);

		} catch (Exception e) {

			logger.error("Error while getting structure of data source by id", e);
			throw new SpagoBIRestServiceException("Error while getting structure of data source by id", buildLocaleFromSession(), e);

		} finally {

			logger.debug("OUT");

		}

		return tableContent.toString();
	}

	private static ConcurrentMap<String, JSONObject> metadataCache = new ConcurrentHashMap<>();

	private JSONObject getTableMetadata(Connection conn) throws HibernateException, JSONException, SQLException {
		String metadataCacheKey = null;
		JSONObject tableContent = new JSONObject();
		ResultSet rs = null;
		try {
			DatabaseMetaData meta = conn.getMetaData();
			String userName = meta.getUserName();
			String url = meta.getURL();
			metadataCacheKey = url + "|" + userName;
			if (metadataCache.get(metadataCacheKey) != null) {
				return metadataCache.get(metadataCacheKey);
			}

				if (conn.getMetaData().getDatabaseProductName().toLowerCase().contains("oracle")) {
					// String q =
					// "SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE FROM ALL_TAB_COLUMNS WHERE OWNER = '"
					// + userName + "'";
					String q = "SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE FROM USER_TAB_COLUMNS";
					Statement stmt = conn.createStatement();
					rs = stmt.executeQuery(q);
					while (rs.next()) {
						if (!tableContent.has(rs.getString(1))) {
							tableContent.put(rs.getString(1), new JSONObject());
						}
						tableContent.getJSONObject(rs.getString(1)).put(rs.getString(2), rs.getString(3));
					}
				} else {
					final String[] TYPES = { "TABLE", "VIEW" };
					final String tableNamePattern = "%";
					final String catalog = null;
					rs = meta.getTables(catalog, null, tableNamePattern, TYPES);
					while (rs.next()) {
						String tableName = rs.getString(3);

						JSONObject column = new JSONObject();
						ResultSet tabCol = meta.getColumns(rs.getString(1), rs.getString(2), tableName, "%");
						while (tabCol.next()) {
							column.put(tabCol.getString(4), "null");
						}
						tabCol.close();
						tableContent.put(tableName, column);
					}
				}
			} finally {
				if (rs != null) {
					rs.close();
				}
				if (!conn.isClosed()) {
					conn.close();
				}
			}
		metadataCache.put(metadataCacheKey, tableContent);
		return tableContent;
	}

	@POST
	@Path("/test")
	@Consumes(MediaType.APPLICATION_JSON)
	@UserConstraint(functionalities = { SpagoBIConstants.DATASOURCE_MANAGEMENT })
	public String testDataSource(DataSource dataSource) throws Exception {

		logger.debug("IN");

		String url = dataSource.getUrlConnection();
		String user = dataSource.getUser();
		String pwd = dataSource.getPwd();
		String driver = dataSource.getDriver();
		String schemaAttr = dataSource.getSchemaAttribute();
		String jndi = dataSource.getJndi();

		IEngUserProfile profile = getUserProfile();

		String schema = (String) profile.getUserAttribute(schemaAttr);
		logger.debug("schema:" + schema);
		Connection connection = null;

		if (jndi != null && jndi.length() > 0) {
			String jndiName = schema == null ? jndi : jndi + schema;

			try {
				logger.debug("Lookup JNDI name:" + jndiName);
				Context ctx = new InitialContext();
				javax.sql.DataSource ds = (javax.sql.DataSource) ctx.lookup(jndiName);
				connection = ds.getConnection();
			} catch (AuthenticationException e) {
				logger.error("Error while attempting to reacquire the authentication information on provided JNDI name", e);
				throw new SpagoBIServiceException(SERVICE_NAME, e);
			} catch (NamingException e) {
				logger.error("Error with provided JNDI name. Can't find the database with that name.", e);
				throw new SpagoBIServiceException(SERVICE_NAME, e);
			} catch (Exception e) {
				logger.error("Error with provided JNDI name.", e);
				throw new SpagoBIServiceException(SERVICE_NAME, e);
			}

		} else {

			if (driver.toLowerCase().contains("mongo")) {
				logger.debug("Checking the connection for MONGODB");
				MongoClient mongoClient = null;
				try {
					int databaseNameStart = url.lastIndexOf("/");
					if (databaseNameStart < 0) {
						logger.error("Error connecting to the mongoDB. No database selected");
					}
					String databaseUrl = url.substring(0, databaseNameStart);
					String databaseName = url.substring(databaseNameStart + 1);

					mongoClient = new MongoClient(databaseUrl);
					DB database = mongoClient.getDB(databaseName);
					database.getCollectionNames();

					logger.debug("Connection OK");
					return new JSONObject().toString();
				} catch (Exception e) {
					logger.error("Error connecting to the mongoDB", e);
				} finally {
					if (mongoClient != null) {
						mongoClient.close();
					}
				}
			} else {

				try {
					Class.forName(driver);
				} catch (ClassNotFoundException e) {
					logger.error("Driver not found", e);
					throw new SpagoBIRestServiceException("Driver not found: " + driver, buildLocaleFromSession(), e);
				}

				connection = DriverManager.getConnection(url, user, pwd);

			}

		}

		return new JSONObject().toString();

	}
}
