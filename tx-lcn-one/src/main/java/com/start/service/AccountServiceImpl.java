package com.start.service;

import com.codingapi.txlcn.tc.annotation.LcnTransaction;
import com.netflix.discovery.converters.Auto;
import com.start.api.TxLcnTwoApi;
import com.start.entity.request.AccountTransferRequest;
import com.start.entity.root.Account;
import com.start.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AccountServiceImpl {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TxLcnTwoApi txLcnTwoApi;

    public Account getAccountById(int id){
        Account account = accountRepository.findById(id).orElseGet(Account::new);
        return account;
    }

    @LcnTransaction
    @Transactional
    public void transfer(AccountTransferRequest requestVo){
        Optional<Account> sourceAccountOp = accountRepository.findFirstByIdAndAmountIsGreaterThanEqual(requestVo.getSourceAccountId(), requestVo.getAmount());
        if (!sourceAccountOp.isPresent()){
            throw new RuntimeException("转账人账号不存在,或金额不足");
        }
        Optional<Account> targetAccountOp = accountRepository.findById(requestVo.getTargetAccountId());
        if (!targetAccountOp.isPresent()){
            throw new RuntimeException("收款人账号不存在");
        }

        // 从转账人账户扣款
        Account sourceAccount = sourceAccountOp.get();
        sourceAccount.setAmount(sourceAccount.getAmount()-requestVo.getAmount());
        accountRepository.save(sourceAccount);

        // 向目标账户打款
        Account targetAccount = targetAccountOp.get();
        targetAccount.setAmount(targetAccount.getAmount()+requestVo.getAmount());
        accountRepository.save(targetAccount);

        // 添加转账记录
        txLcnTwoApi.add(requestVo.getSourceAccountId(),requestVo.getAmount(),"支出");
        txLcnTwoApi.add(requestVo.getTargetAccountId(),requestVo.getAmount(),"收入");
        int i = 0/0;
    }

}
