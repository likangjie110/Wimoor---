package com.wimoor.ozon.finance.service;

import java.time.LocalDate;
import java.util.List;

import com.wimoor.common.user.UserInfo;
import com.wimoor.ozon.finance.pojo.dto.OzonFinanceImportCommand;
import com.wimoor.ozon.finance.pojo.dto.OzonFinanceTransactionQuery;
import com.wimoor.ozon.finance.pojo.entity.OzonFinTransaction;
import com.wimoor.ozon.finance.pojo.vo.OzonFinanceImportResult;
import com.wimoor.ozon.finance.pojo.vo.OzonFinanceTaskView;

public interface IOzonFinanceService {

    OzonFinanceImportResult importReport(UserInfo user, OzonFinanceImportCommand command);

    List<OzonFinanceTaskView> listTasks(UserInfo user, String authId);

    List<OzonFinTransaction> listTransactions(UserInfo user, OzonFinanceTransactionQuery query);

    String getRawContent(UserInfo user, String authId, String taskId);

    // API Sync Methods
    OzonFinanceImportResult syncTransactionsFromApi(UserInfo user, String authId, LocalDate startDate, LocalDate endDate);

    OzonFinanceImportResult syncRealizationsFromApi(UserInfo user, String authId, LocalDate startDate, LocalDate endDate);

    OzonFinanceImportResult fetchReportFromApi(UserInfo user, String authId, String reportType);
}
