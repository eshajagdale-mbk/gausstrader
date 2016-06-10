package net.toddsarratt.GaussTrader;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.MutableDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;

/* 	HistoricalPrice(String closeEpoch, double adjClose)  */

public abstract class DBHistoricalPrices {
   private static DataSource dataSource = GaussTrader.getDataSource();
   private static Connection dbConnection;
   private static final Logger LOGGER = LoggerFactory.getLogger(DBHistoricalPrices.class);

    static boolean marketWasOpen(MutableDateTime histDateTime) {
      return !(TradingSession.isMarketHoliday(histDateTime) ||
         (histDateTime.getDayOfWeek() == DateTimeConstants.SATURDAY) ||
         (histDateTime.getDayOfWeek() == DateTimeConstants.SUNDAY));
   }

   static LinkedHashMap<Long, Double> getDBPrices(String ticker, DateTime earliestCloseDate) {
      LOGGER.debug("Entering DBHistoricalPrices.getDBPrices(String {}, DateTime {} ({})",
         ticker, earliestCloseDate.getMillis(), earliestCloseDate.toString());
      HistoricalPrice histPrice = null;
      LinkedHashMap<Long, Double> queriedPrices = new LinkedHashMap<>();
      int pricesNeeded = GaussTrader.bollBandPeriod;
      int dbPricesFound = 0;
      ResultSet historicalPriceResultSet = null;
      PreparedStatement sqlStatement = null;
      long closeEpoch = 0l;
      double adjClose = 0.0;
      DateTime dateTimeForLogging;
      try {
         LOGGER.debug("Getting connection to {}", GaussTrader.DB_NAME);
         dbConnection = dataSource.getConnection();
         sqlStatement = dbConnection.prepareStatement("SELECT * FROM prices WHERE ticker = ? AND close_epoch >= ?");
         sqlStatement.setString(1, ticker);
         sqlStatement.setLong(2, earliestCloseDate.getMillis());
         LOGGER.debug("SELECT * FROM prices WHERE ticker = {} AND close_epoch >= {}", ticker, earliestCloseDate.getMillis());
         historicalPriceResultSet = sqlStatement.executeQuery();
         while (historicalPriceResultSet.next() && (dbPricesFound < pricesNeeded)) {
            closeEpoch = historicalPriceResultSet.getLong("close_epoch");
            adjClose = historicalPriceResultSet.getDouble("adj_close");
            dateTimeForLogging = new DateTime(closeEpoch, DateTimeZone.forID("America/New_York"));
            LOGGER.debug("Adding {} ({}), {}", closeEpoch, dateTimeForLogging.toString(), adjClose);
            queriedPrices.put(closeEpoch, adjClose);
            dbPricesFound++;
         }
         dbConnection.close();
      } catch (SQLException sqle) {
         LOGGER.info("Unable to get connection to {}", GaussTrader.DB_NAME);
         LOGGER.debug("Caught (SQLException sqle)", sqle);
      } finally {
         LOGGER.debug("Found {} prices in db", dbPricesFound);
         LOGGER.debug("Returning from database LinkedHashMap of size {}", queriedPrices.size());
         if (dbPricesFound != queriedPrices.size()) {
            LOGGER.warn("Mismatch between prices found {} and number returned {}, possible duplication?", dbPricesFound, queriedPrices.size());
         }
         return queriedPrices;
      }
   }

   public static void addStockPrice(String ticker, long dateEpoch, double adjClose) {
      LOGGER.debug("Entering DBHistoricalPrices.addStockPrice(String {}, long {}, double {})", ticker, dateEpoch, adjClose);
      PreparedStatement sqlStatement = null;
      int insertedRowCount;
      try {
         LOGGER.debug("Getting connection to {}", GaussTrader.DB_NAME);
         LOGGER.debug("Inserting historical stock price data for ticker {} into the database.", ticker);
         dbConnection = dataSource.getConnection();
         sqlStatement = dbConnection.prepareStatement("INSERT INTO prices (ticker, adj_close, close_epoch) VALUES (?, ?, ?)");
         sqlStatement.setString(1, ticker);
         sqlStatement.setDouble(2, adjClose);
         sqlStatement.setLong(3, dateEpoch);
         LOGGER.debug("Executing INSERT INTO prices (ticker, adj_close, close_epoch) VALUES ({}, {}, {})", ticker, adjClose, dateEpoch);
         if ((insertedRowCount = sqlStatement.executeUpdate()) != 1) {
            LOGGER.warn("Inserted {} rows. Should have inserted 1 row.", insertedRowCount);
         }
         dbConnection.close();
      } catch (SQLException sqle) {
         LOGGER.info("Unable to get connection to {}", GaussTrader.DB_NAME);
         LOGGER.debug("Caught (SQLException sqle)", sqle);
      }
   }
   public static boolean tickerPriceInDb(String ticker) {
      LOGGER.debug("Entering DBHistoricalPrices.tickerPriceInDb()");
      try {
         dbConnection = dataSource.getConnection();
         PreparedStatement summarySqlStatement = dbConnection.prepareStatement("SELECT DISTINCT ticker FROM prices WHERE ticker = ?");
         summarySqlStatement.setString(1, ticker);
         LOGGER.debug("Executing SELECT DISTINCT ticker FROM prices WHERE ticker = {}", ticker);
         ResultSet tickerInDbResultSet = summarySqlStatement.executeQuery();
         dbConnection.close();
         return (tickerInDbResultSet.next());
      } catch (SQLException sqle) {
         LOGGER.info("SQLException attempting to find historical price for {}", ticker);
         LOGGER.debug("Exception", sqle);
      }
      return false;
   }
}