/*
 * This Source Code Form is subject to the terms of the Mozilla
 * Public License, v. 2.0. If a copy of the MPL was not distributed
 * with this file, You can obtain one at
 *
 *  https://mozilla.org/MPL/2.0/.
 */
package org.openmf.psp.mojaloop.routebuilder.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.openmf.psp.component.FspRestClient;
import org.openmf.psp.dto.FspMoneyData;
import org.openmf.psp.dto.fsp.QuoteFspRequestDTO;
import org.openmf.psp.dto.fsp.QuoteFspResponseDTO;
import org.openmf.psp.mojaloop.cache.TransactionContextHolder;
import org.openmf.psp.mojaloop.constant.ExchangeHeader;
import org.openmf.psp.mojaloop.internal.TransactionCacheContext;
import org.openmf.psp.mojaloop.internal.TransactionRoleContext;
import org.openmf.psp.type.TransactionRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Processor to send POST /quotes request to FSP.
 */
@Component("quotesFspProcessor")
public class QuotesFspProcessor implements Processor {

    private FspRestClient fspRestClient;

    private TransactionContextHolder transactionContextHolder;

    @Autowired
    public QuotesFspProcessor(FspRestClient fspRestClient, TransactionContextHolder transactionContextHolder) {
        this.fspRestClient = fspRestClient;
        this.transactionContextHolder = transactionContextHolder;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        String transactionId = exchange.getProperty(ExchangeHeader.TRANSACTION_ID.getKey(), String.class);
        TransactionCacheContext transactionContext = transactionContextHolder.getTransactionContext(transactionId);

        TransactionRole quotesRole = exchange.getProperty(ExchangeHeader.QUOTES_ROLE.getKey(), TransactionRole.class);
        if (quotesRole == null)
            quotesRole = exchange.getProperty(ExchangeHeader.CURRENT_ROLE.getKey(), TransactionRole.class);

        TransactionRoleContext roleContext = transactionContext.getRoleContext(quotesRole);
        String accountId = roleContext.getPartyContext().getAccountId();

        String quoteId = transactionContext.getOrCreateQuoteId();

        FspMoneyData amount = new FspMoneyData(transactionContext.getTransferAmount(), transactionContext.getCurrency());
        QuoteFspRequestDTO request = new QuoteFspRequestDTO(transactionId, transactionContext.getTransactionRequestId(),
                quoteId, accountId, amount, transactionContext.getAmountType(), quotesRole, transactionContext.getTransactionType());

        QuoteFspResponseDTO quotesResponseDTO = fspRestClient.callQuotes(request, roleContext.getFspId());

        transactionContextHolder.updateFspQuote(transactionId, quotesRole, quotesResponseDTO);
    }
}
