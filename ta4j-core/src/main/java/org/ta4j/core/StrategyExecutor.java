package org.ta4j.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.Trade.TradeType;
import org.ta4j.core.cost.CostModel;
import org.ta4j.core.cost.ZeroCostModel;
import org.ta4j.core.num.Num;

public class StrategyExecutor {

    /** The logger */
    private final static Logger log = LoggerFactory.getLogger(StrategyExecutor.class);

    /** The managed bar series */
    private BarSeries barSeries;

    /** The trading cost models */
    private CostModel transactionCostModel;
    private CostModel holdingCostModel;

	private int unstablePeriod;
    
    /**
     * Constructor.
     * Uses {@link ZeroCostModel}
     */
    public StrategyExecutor() {
        this(null, new ZeroCostModel(), new ZeroCostModel());
    }
    
    /**
     * Constructor.
     * 
     * @param barSeries            the bar series to be managed
     * @param transactionCostModel the cost model for transactions of the asset
     * @param holdingCostModel     the cost model for holding asset (e.g. borrowing)
     */
    public StrategyExecutor(BarSeries barSeries, CostModel transactionCostModel, CostModel holdingCostModel) {
        this.barSeries = barSeries;
        this.transactionCostModel = transactionCostModel;
        this.holdingCostModel = holdingCostModel;
    }
    
    /**
     * @param barSeries the bar series to be managed
     */
    public void setBarSeries(BarSeries barSeries) {
        this.barSeries = barSeries;
    }

    /**
     * @return the managed bar series
     */
    public BarSeries getBarSeries() {
        return barSeries;
    }
    
    /**
     * @param index a bar index
     * @return true if this strategy is unstable at the provided index, false
     *         otherwise (stable)
     */
    boolean isUnstableAt(int index) {
    	return index < unstablePeriod;
    }
    
    /**
     * @param unstablePeriod number of bars that will be strip off for this strategy
     */
    void setUnstablePeriod(int unstablePeriod) {
    	this.unstablePeriod = unstablePeriod;
    }

    /**
     * @return unstablePeriod number of bars that will be strip off for this
     *         strategy
     */
    int getUnstablePeriod() {
    	return unstablePeriod;
    }
    
    /**
     * Runs the provided strategy over the managed series (from startIndex to
     * finishIndex).
     *
     * @param entryRule   the entry rule
     * @param exitRule    the exit rule
     * @param tradeType   the {@link TradeType} used to open the trades
     * @param amount      the amount used to open/close the trades
     * @param startIndex  the start index for the run (included)
     * @param finishIndex the finish index for the run (included)
     * @return the trading record coming from the run
     */
    public TradingRecord run(Rule entryRule, Rule exitRule, TradeType tradeType, Num amount, int startIndex, int finishIndex) {

        int runBeginIndex = Math.max(startIndex, barSeries.getBeginIndex());
        int runEndIndex = Math.min(finishIndex, barSeries.getEndIndex());

        log.trace("Running strategy (indexes: {} -> {}): entry {} exit {} (starting with {})", runBeginIndex, runEndIndex, entryRule, exitRule, tradeType);
        
        FeedbackTradingRecord tradingRecord = new FeedbackTradingRecord(tradeType, transactionCostModel, holdingCostModel);
        for (int index = runBeginIndex; index <= runEndIndex; index++) {
        	if (!isUnstableAt(index)) {
        		// set the default price is the close price --> can be overwritten by rules
        		tradingRecord.setPriceForTrade(getBarSeries().getBar(index).getClosePrice());
        		// first close all what is satisfied
        		checkExitTrades(exitRule, tradingRecord, amount, index);
        		// after that check th
        		checkEntryRule(entryRule, tradingRecord, amount, index);
        	}
        }
        
        if (!tradingRecord.isClosed()) {
            // If the last position is still opened, we search out of the run end index.
            // May works if the end index for this run was inferior to the actual number of
            // bars
            int seriesMaxSize = Math.max(barSeries.getEndIndex() + 1, barSeries.getBarData().size());
            for (int i = runEndIndex + 1; i < seriesMaxSize && !tradingRecord.isClosed(); i++) {
                // For each bar after the end index of this run...
                // --> Trying to close the last position
                checkExitTrades(exitRule, tradingRecord, amount, i);
            }
        }
        
        return tradingRecord;
    }

	private void checkEntryRule(Rule entryRule, FeedbackTradingRecord tradingRecord, Num amount, int index) {
		log.trace(">>> #checkEntryRule({}): {}", index, entryRule);
		if (!tradingRecord.getCurrentPosition().isNew()) {
			return;
		}
		if (entryRule.isSatisfied(index, tradingRecord)) {
			tradingRecord.operate(index, tradingRecord.getPriceForTrade(), amount);
		}
	}

	private void checkExitTrades(Rule exitRule, FeedbackTradingRecord tradingRecord, Num amount, int index) {
		log.trace(">>> #checkExitTrades({}): {}", index, exitRule);
		if (!tradingRecord.getCurrentPosition().isOpened()) {
			return;
		}
		if (exitRule.isSatisfied(index, tradingRecord)) {
			tradingRecord.operate(index, tradingRecord.getPriceForTrade(), amount);
		}
	}
}
