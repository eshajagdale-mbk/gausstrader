package net.toddsarratt.gaussTrader.orders;

import net.toddsarratt.gaussTrader.PriceBasedAction;
import net.toddsarratt.gaussTrader.TransactionId;
import net.toddsarratt.gaussTrader.securities.Option;
import net.toddsarratt.gaussTrader.securities.Security;
import net.toddsarratt.gaussTrader.securities.SecurityType;
import net.toddsarratt.gaussTrader.securities.Stock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Class to record each order
 * Fields :
 * orderId : Use gaussTrader.generateNewId() to populate
 * open : boolean, open or closed order
 * ticker : security being traded
 * limitPrice : Limit orders only
 * Action : "BUY" or "SELL"
 * totalQuantity : probably 1 contract, could be 100 shares :: REPLACED WITH ACTION
 * secType : "CALL", "PUT", "STOCK" :: REPLACED WITH ACTION
 * tif (Time in Force) : "GTC" (Good 'Til Cancelled) vs "GFD" (Good For Day = day order)
 * epochOpened : milliseconds since epoch when order was opened
 * instantClosed : milliseconds since epoch when order was closed
 * closeReason : "FILLED", "EXPIRED", or "CANCELLED"
 * fillPrice : price at which order was filled and closed
 */

abstract class Order {
	private final static Logger LOGGER = LoggerFactory.getLogger(Option.class);
	TransactionId orderId;
	boolean open;
	String ticker;
	BigDecimal limitPrice;
	PriceBasedAction action;
	BigDecimal claimAgainstCash;
	String tif;
	Instant instantOpened;
	Instant instantClosed;
	String closeReason;
	BigDecimal fillPrice;

	Order() {
	}

	static Order of(Security security, BigDecimal limitPrice, PriceBasedAction action, String tif) {
		LOGGER.debug("Entering Order factory method of(Security {}, BigDecimal {}, PriceBasedAction {}, String {})",
				security, limitPrice.toPlainString(), action, tif);
		LOGGER.debug("Security is type {}", security.getSecType());
		if ((action.getSecurityType().equals(SecurityType.CALL))
				|| (action.getSecurityType().equals(SecurityType.PUT))) {
			LOGGER.debug("Security is an option");
			return new OptionOrder((Option) security, limitPrice, action, tif);
		}
		return new StockOrder((Stock) security, limitPrice, action, tif);
	}

	TransactionId getOrderId() {
		return orderId;
	}

	void setOrderId(TransactionId orderId) {
		this.orderId = orderId;
	}

	boolean isOpen() {
		return open;
	}

	void setOpen(boolean open) {
		this.open = open;
	}

	public String getTicker() {
		return ticker;
	}

	void setTicker(String ticker) {
		this.ticker = ticker;
	}

	BigDecimal getLimitPrice() {
		return limitPrice;
	}

	void setLimitPrice(BigDecimal limitPrice) {
		this.limitPrice = limitPrice;
	}

	PriceBasedAction getAction() {
		return action;
	}

	void setAction(PriceBasedAction action) {
		this.action = action;
	}

	String getTif() {
		return tif;
	}

	void setTif(String tif) {
		this.tif = tif;
	}

	void fill(BigDecimal fillPrice) {
		LOGGER.debug("Entering fill(${})", fillPrice.toPlainString());
		this.closeReason = "FILLED";
		this.open = false;
		this.fillPrice = fillPrice;
		this.instantClosed = Instant.now();
		LOGGER.info("Order {} {} @ ${} epoch {}", orderId, closeReason, this.fillPrice, instantClosed);
	}

	void closeExpired() {
		close("EXPIRED");
	}

	public void closeCancelled() {
		close("CANCELLED");
	}

	private void close(String closeReason) {
		LOGGER.debug("Entering close()");
		this.closeReason = closeReason;
		this.open = false;
		this.fillPrice = BigDecimal.ZERO;
		this.instantClosed = Instant.now();
		LOGGER.info("Order {} {} @ ${} epoch {}", this.orderId, closeReason, fillPrice, instantClosed);
	}

	Instant getInstantOpened() {
		return instantOpened;
	}

	void setInstantOpened(Instant instantOpened) {
		this.instantOpened = instantOpened;
	}

	Instant getInstantClosed() {
		return instantClosed;
	}

	void setInstantClosed(Instant instantClosed) {
		this.instantClosed = instantClosed;
	}

	String getCloseReason() {
		return closeReason;
	}

	public void setCloseReason(String closeReason) {
		this.closeReason = closeReason;
	}

	BigDecimal getFillPrice() {
		return fillPrice;
	}

	public void setFillPrice(BigDecimal fillPrice) {
		this.fillPrice = fillPrice;
	}

	BigDecimal getClaimAgainstCash() {
		return claimAgainstCash;
	}

	void setClaimAgainstCash(BigDecimal claimAgainstCash) {
		this.claimAgainstCash = claimAgainstCash;
	}

	BigDecimal calculateCostBasis() {
		LOGGER.debug("Entering Order.calculateClaimAgainstCash()");
		BigDecimal costBasis = limitPrice.multiply(
				new BigDecimal(
						action.getNumberToTransact()
								* (action.getSecurityType().equals(SecurityType.STOCK) ? 1.0 : 100.0)
								* (action.getBuyOrSell().equals("BUY") ? 1.0 : -1.0)));
		LOGGER.debug("costBasis = ${}", costBasis);
		return costBasis;
	}

	abstract BigDecimal calculateClaimAgainstCash();

	boolean canBeFilled(BigDecimal lastTick) {
		return (lastTick.compareTo(BigDecimal.ZERO) > 0) &&
				(action.getBuyOrSell().equals("BUY") ?
						(lastTick.compareTo(limitPrice) <= 0) : (lastTick.compareTo(limitPrice) >= 0));
	}

	@Override
	public String toString() {
		return (orderId + " " + action + " " + ticker + " " + " @ $" + limitPrice);
	}
}
