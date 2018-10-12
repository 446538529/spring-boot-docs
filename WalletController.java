package com.mideadc.wallet.rest;

import com.mideadc.commons.domain.JsonObjectPage;
import com.mideadc.component.llwallet.accp.bean.*;
import com.mideadc.wallet.api.IWalletApi;
import com.mideadc.wallet.service.WalletService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author 大白
 * @since 0.1.0
 */
@RestController
@RequestMapping("/wallet/center")
public class WalletController implements IWalletApi {

    @Resource
    private WalletService walletService;

    /**
     * 3.4.1. 开户申请
     */
    @Override
    @PostMapping("/openAcctApply")
    public JsonObjectPage<OpenAcctApplyResp> openAcctApply(@RequestBody OpenAcctApplyReq req) {
        return walletService.openAcctApply(req);
    }

    /**
     * 3.4.2. 开户认证
     *
     * @param req
     * @return
     */
    @Override
    @PostMapping("/openAcctVerify")
    public JsonObjectPage<OpenacctVerifyResp> openAcctVerify(@RequestBody OpenacctVerifyReq req) {
        return walletService.openAcctVerify(req);
    }

    /**
     * 3.2.1支付统一创单
     *
     * @param req
     * @return
     */
    @Override
    @PostMapping("/createTrade")
    public JsonObjectPage<TradeCreateResp> createTrade(@RequestBody TradeCreateReq req) {
        return walletService.createTrade(req);
    }

    /**
     * 3.2.3银行卡快捷支付
     *
     * @param req
     * @return
     */
    @Override
    @PostMapping("/bankcardPay")
    public JsonObjectPage<BankcardPayResp> bankcardPay(@RequestBody BankcardPayReq req) {
        return walletService.bankcardPay(req);
    }

    /**
     * 3.2.2商户系统用户余额支付
     *
     * @param req
     * @return
     */
    @Override
    @PostMapping("/balancePay")
    public JsonObjectPage<BalancePayResp> balancePay(@RequestBody BalancePayReq req) {
        return walletService.balancePay(req);
    }

    /**
     * 3.17 交易流水结果查询
     *
     * @param req
     * @return
     */
    @Override
    @PostMapping("/queryTnx")
    public JsonObjectPage<QueryTnxResp> queryTnx(@RequestBody QueryTnxReq req) {
        return null;
    }

    /**
     * 3.16. 账户信息查询
     *
     * @param req
     * @return
     */
    @Override
    @PostMapping("/queryAcctinfo")
    public JsonObjectPage<QueryAcctinfoResp> queryAcctinfo(@RequestBody QueryAcctinfoReq req) {
        return null;
    }

    /**
     * 3.15. 用户信息查询
     *
     * @param req
     * @return
     */
    @Override
    @PostMapping("/queryUserinfo")
    public JsonObjectPage<QueryUserinfoResp> queryUserinfo(@RequestBody QueryUserinfoReq req) {
        return null;
    }

    /**
     * 3.13.获取随机因子
     */
    @Override
    @PostMapping("/getRandom")
    public JsonObjectPage<GetRandomResp> getRandom(@RequestBody GetRandomReq req) {
        return walletService.getRandom(req);
    }

    /**
     * 3.12.2 找回密码-验证
     *
     * @param req
     * @return
     */
    @Override
    @PostMapping("/findPasswordVerify")
    public JsonObjectPage<FindPasswordVerifyResp> findPasswordVerify(@RequestBody FindPasswordVerifyReq req) {
        return walletService.findPasswordVerify(req);
    }

    /**
     * 3.12.2 找回密码-申请
     *
     * @param req
     * @return
     */
    @Override
    @PostMapping("/findPasswordApply")
    public JsonObjectPage<FindPasswordApplyResp> findPasswordApply(@RequestBody FindPasswordApplyReq req) {
        return walletService.findPasswordApply(req);
    }

    /**
     * 3.11.重置密码
     */
    @Override
    @PostMapping("/changePassword")
    public JsonObjectPage<ChangePasswordResp> changePassword(@RequestBody ChangePasswordReq req) {
        return walletService.changePassword(req);
    }

    /**
     * 3.6.2. 个人用户新增绑卡-绑卡验证
     *
     * @param req
     * @return
     */
    @Override
    @PostMapping("/bindcardVerify")
    public JsonObjectPage<BindcardVerifyResp> bindcardVerify(@RequestBody BindcardVerifyReq req) {
        return walletService.bindcardVerify(req);
    }

    /**
     * 3.6.2. 个人用户新增绑卡-绑卡申请
     *
     * @param req
     * @return
     */
    @Override
    @PostMapping("/bindcardApply")
    public JsonObjectPage<BindcardApplyResp> bindcardApply(@RequestBody BindcardApplyReq req) {
        return walletService.bindcardApply(req);
    }

    /**
     * 3.8.1.个人用户解绑银行卡
     */
    @Override
    @PostMapping("/unLinkedAcct")
    public JsonObjectPage<UnlinkedacctResp> unlinkedacct(@RequestBody UnlinkedacctReq req) {
        return walletService.unLinkedAcct(req);
    }

    /**
     * 3.6.交易二次短信验证
     */
    @Override
    @PostMapping("tradeSecondSmsValid")
    public JsonObjectPage<TradeSecondSmsValidResp> tradeSecondSmsValid(@RequestBody TradeSecondSmsValidReq req) {
        return walletService.tradeSecondSmsValid(req);
    }

    /**
     * 3.5.1. 担保交易确认
     *
     * @param req
     * @return
     */
    @Override
    @PostMapping("/securedConfirm")
    public JsonObjectPage<SecuredConfirmResp> securedConfirm(@RequestBody SecuredConfirmReq req) {
        return null;
    }

    /**
     * 3.4.1. 代发申请
     *
     * @param req
     * @return
     */
    @Override
    @PostMapping("/transfer")
    public JsonObjectPage<TransferResp> transfer(@RequestBody TransferReq req) {
        return null;
    }

    /**
     * 3.3.1.提现申请
     *
     * @param req
     * @return
     */
    @Override
    @PostMapping("/withdrawal")
    public JsonObjectPage<WithdrawalResp> withdrawal(@RequestBody WithdrawalReq req) {
        return null;
    }

    /**
     * 3.3.3. 提现结果查询
     *
     * @param req
     * @return
     */
    @Override
    @PostMapping("/queryWithdrawal")
    public JsonObjectPage<QueryWithdrawalResp> queryWithdrawal(@RequestBody QueryWithdrawalReq req) {
        return null;
    }

    /**
     * 3.7.1.退款
     *
     * @param req
     * @return
     */
    @Override
    @PostMapping("/refund")
    public JsonObjectPage<RefundResp> refund(@RequestBody RefundReq req) {
        return null;
    }


}
