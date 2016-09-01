package net.toddsarratt.GaussTrader;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Collected constants for GaussTrader. All members of this class are static final. Google Guava library used
 * (inconsistently, at this time) to ensure collections (only a subset) are immutable.
 *
 * @author Todd Sarratt todd.sarratt@gmail.com
 * @since v0.2
 */
class Constants {
	private static final Logger LOGGER = LoggerFactory.getLogger(Constants.class);
	private static final String DEFAULT_CONFIG_FILENAME = "default.properties";
	private static final String CONFIG_FILENAME = "appProps.properties";
	private static final Properties appProps = null;
	static final BigDecimal BIGDECIMAL_MINUS_ONE = new BigDecimal(-1);
	static final BigDecimal BIGDECIMAL_ONE_HUNDRED = new BigDecimal(100);

	static {
		Properties defaultProps = new Properties();
		try (FileInputStream defaultsIn = new FileInputStream(DEFAULT_CONFIG_FILENAME);
		     FileInputStream appsIn = new FileInputStream(CONFIG_FILENAME)) {
			// create and load default properties
			defaultProps.load(defaultsIn);
			// create application properties with default
			new Properties(defaultProps);
			// now load properties
			// from last invocation
			appProps.load(appsIn);
		} catch (NullPointerException | IOException e) {
			LOGGER.error("Problem loading appProps.properties", e);
			System.exit(1);
		}
	}

	private static final String DOW_JONES_30 = "MMM, NKE, AXP, T, GS, BA, CAT, CVX, CSCO, KO, DD," +
			"XOM, GE, V, HD, INTC, IBM, JNJ, JPM, MCD, MRK, MSFT, PFE, PG, TRV, UNH, UTX, VZ, WMT, DIS";
	static final String PORTFOLIO_NAME = appProps.getProperty("PORTFOLIO_NAME");
	static final List<String> TICKERS = Arrays.asList(appProps.getProperty("TICKERS", DOW_JONES_30)
			.replaceAll("\\s", "")
			.split(","));
	static final String DB_IP = appProps.getProperty("DB_IP", "localhost");
	static final String DB_NAME = appProps.getProperty("DB_NAME", "postgres");
	static final String DB_USER = appProps.getProperty("DB_USER", "postgres");
	static final String DB_PASSWORD = appProps.getProperty("DB_PASSWORD", "b3llcurv38");
	static final int MARKET_QUERY_RETRIES = Integer.valueOf(appProps.getProperty("DB_PASSWORD", "5"));
	static final BigDecimal STOCK_PCT_OF_PORTFOLIO = new BigDecimal(appProps.getProperty("STOCK_PCT_OF_PORTFOLIO", "10.0"));
	static final BigDecimal STARTING_CASH = new BigDecimal(appProps.getProperty("STARTING_CASH", "1_000_000.00"));
	static final int BOLL_BAND_PERIOD = Integer.valueOf(appProps.getProperty("BOLL_BAND_PERIOD", "20"));
	static final BigDecimal BOLLINGER_SD1 = new BigDecimal(appProps.getProperty("BOLLINGER_SD1", "2.0"));
	static final BigDecimal BOLLINGER_SD2 = new BigDecimal(appProps.getProperty("BOLLINGER_SD2", "2.5"));
	static final BigDecimal BOLLINGER_SD3 = new BigDecimal(appProps.getProperty("BOLLINGER_SD3", "3.0"));
	static final boolean DELAYED_QUOTES = Boolean.valueOf(appProps.getProperty("DELAYED_QUOTES", "true"));
	static final int DELAY_MS = (Integer.valueOf(appProps.getProperty("DELAY", "60"))) * 1000;
	private static final List<Integer> JULIAN_HOLIDAYS_2016 = Arrays.stream(
			appProps.getProperty("JULIAN_HOLIDAYS_2016").split(","))
			.map(Integer::valueOf)
			.collect(Collectors.toList());
	private static final List<Integer> JULIAN_HOLIDAYS_2017 = Arrays.stream(
			appProps.getProperty("JULIAN_HOLIDAYS_2017").split(","))
			.map(Integer::valueOf)
			.collect(Collectors.toList());
	static final ImmutableMap<Integer, List<Integer>> HOLIDAY_MAP =
			ImmutableMap.<Integer, List<Integer>>builder()
					.put(2016, JULIAN_HOLIDAYS_2016)
					.put(2017, JULIAN_HOLIDAYS_2017)
					.build();
	private static final List<Integer> JULIAN_1PM_CLOSE_2016 = Arrays.stream(
			appProps.getProperty("JULIAN_1PM_CLOSE_2016").split(","))
			.map(Integer::valueOf)
			.collect(Collectors.toList());
	private static final List<Integer> JULIAN_1PM_CLOSE_2017 = Arrays.stream(
			appProps.getProperty("JULIAN_1PM_CLOSE_2017").split(","))
			.map(Integer::valueOf)
			.collect(Collectors.toList());
	static final ImmutableMap<Integer, List<Integer>> EARLY_CLOSE_MAP =
			ImmutableMap.<Integer, List<Integer>>builder()
					.put(2016, JULIAN_1PM_CLOSE_2016)
					.put(2017, JULIAN_1PM_CLOSE_2017)
					.build();
	static final ZoneId MARKET_ZONE = ZoneId.of(appProps.getProperty("MARKET_ZONE", "America/New_York"));
}
