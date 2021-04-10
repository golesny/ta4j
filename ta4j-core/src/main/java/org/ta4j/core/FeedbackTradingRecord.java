package org.ta4j.core;

import org.ta4j.core.Trade.TradeType;
import org.ta4j.core.cost.CostModel;
import org.ta4j.core.num.Num;

/**
 * Extension to the {@link BaseTradingRecord} to avoid polluting all classes.
 *
 */
public class FeedbackTradingRecord extends BaseTradingRecord {
	private static final long serialVersionUID = 1L;
	private Num priceForTrade;

	
    /**
     * Constructor.
     *
     * @param entryTradeType       the {@link TradeType trade type} of entries in
     *                             the trading session
     * @param transactionCostModel the cost model for transactions of the asset
     * @param holdingCostModel     the cost model for holding asset (e.g. borrowing)
     */
    public FeedbackTradingRecord(TradeType entryTradeType, CostModel transactionCostModel, CostModel holdingCostModel) {
        super(entryTradeType, transactionCostModel, holdingCostModel);
    }


    /**
     * This shall be used to overwrite by {@link Rule}s.
     * 
     * @param price the price to enter or exit the trade
     */
	public void setPriceForTrade(Num price) {
		if (price == null) {
			throw new IllegalArgumentException("price may not be null");
		}
		this.priceForTrade = price;
	}
	
	/**
	 * 
	 * @return the price to enter or exit the trade
	 */
	public Num getPriceForTrade() {
		return priceForTrade;
	}
}
