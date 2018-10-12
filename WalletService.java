package com.mideadc.wallet.service;

import com.mideadc.commons.domain.JsonObjectPage;
import com.mideadc.commons.domain.exception.BusinessRuntimeException;
import com.mideadc.commons.domain.utils.DateUtil;
import com.mideadc.commons.domain.utils.Snowflake;
import com.mideadc.component.llwallet.accp.AccpApi;
import com.mideadc.component.llwallet.accp.bean.*;
import com.mideadc.wallet.dao.entity.AccountDetail;
import com.mideadc.wallet.dao.entity.BindCard;
import com.mideadc.wallet.dao.entity.Refund;
import com.mideadc.wallet.dao.entity.WalletAccount;
import com.mideadc.wallet.dao.mapper.AccountDetailMapper;
import com.mideadc.wallet.dao.mapper.BindCardMapper;
import com.mideadc.wallet.dao.mapper.WalletAccountMapper;
import com.mideadc.wallet.enums.BindCardStatusEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author 大白
 * @since 0.1.0
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class WalletService {

    private static final Logger LOG = LoggerFactory.getLogger(WalletService.class);
    //交易成功码
    private static final String SUCCESS_CODE = "0000";

    @Resource
    private WalletAccountMapper walletAccountMapper;
    @Resource
    private BindCardMapper bindCardMapper;
    @Resource
    private AccountDetailMapper accountDetailMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    private Snowflake snowflake = Snowflake.getInstanceSnowflake();


    /**
     * 3.16. 账户信息查询
     * @param req
     * @return
     */
    public JsonObjectPage<QueryAcctinfoResp> queryAcctinfo(QueryAcctinfoReq req) {
        QueryAcctinfoResp resp = AccpApi.queryAcctinfo(req);
        if (!resp.getRet_code().equals(SUCCESS_CODE)) {
            throw new BusinessRuntimeException(resp.getRet_msg(), resp.getRet_code());
        }
        return JsonObjectPage.createJsonObjectPage(resp);
    }
        /**
         * 开户接口服务
         *
         * @param req
         * @return
         */
    public JsonObjectPage<OpenAcctApplyResp> openAcctApply(OpenAcctApplyReq req) {
        //TODO set notify_url
        String orderNum=DateUtil.getOrderNum();
        req.setTxn_seqno(orderNum).setUser_id(orderNum).setTxn_time(req.getTimestamp());
        OpenAcctApplyResp resp = AccpApi.openAcctApply(req);
        if (!resp.getRet_code().equals(SUCCESS_CODE)) {
            throw new BusinessRuntimeException(resp.getRet_msg(), resp.getRet_code());
        }
        redisTemplate.opsForValue().set(getRedisKey("openAcctApply_req", req.getUser_id()), req);
        redisTemplate.opsForValue().set(getRedisKey("openAcctApply_resp", req.getUser_id()), resp);
        return JsonObjectPage.createJsonObjectPage(resp);
    }

    /**
     * 开户验证服务
     *
     * @param req
     * @return
     */
    public JsonObjectPage<OpenacctVerifyResp> openAcctVerify(OpenacctVerifyReq req) {
        String reqKey = getRedisKey("openAcctApply_req", req.getUser_id());
        OpenAcctApplyReq preRequest = (OpenAcctApplyReq) redisTemplate.opsForValue().get(reqKey);
        if (preRequest == null) {
            throw new BusinessRuntimeException("请先调用绑卡申请接口");
        }
        String respKey = getRedisKey("openAcctApply_resp", req.getUser_id());
        OpenAcctApplyResp postResp = (OpenAcctApplyResp) redisTemplate.opsForValue().get(respKey);
        req.setToken(postResp.getToken()).setTxn_seqno(postResp.getTxn_seqno());
        OpenacctVerifyResp verifyResp = AccpApi.openAcctVerify(req);
        if (!verifyResp.getRet_code().equals(SUCCESS_CODE)) {
            throw new BusinessRuntimeException(verifyResp.getRet_msg(), verifyResp.getRet_code());
        }
        Date date = new Date();
        WalletAccount walletAccount = new WalletAccount();
        walletAccount.setWalletAccountId(snowflake.nextId());
        walletAccount.setCreateTime(date);
        walletAccount.setIdCardNo(preRequest.getBasicInfo().getId_no());
        walletAccount.setName(preRequest.getBasicInfo().getUser_name());
        walletAccount.setTradeNo(req.getTxn_seqno());
        walletAccount.setAccountId(Long.parseLong(req.getUser_id()));
        //TODO set walletAccount appId  status balance frozen
        walletAccountMapper.insert(walletAccount);
        redisTemplate.delete(reqKey);
        redisTemplate.delete(respKey);
        return JsonObjectPage.createJsonObjectPage(verifyResp);
    }

    /**
     * 解绑银行卡
     *
     * @param req
     * @return
     */
    public JsonObjectPage<UnlinkedacctResp> unLinkedAcct(UnlinkedacctReq req) {
        UnlinkedacctResp resp = AccpApi.unlinkedacct(req);
        if (!resp.getRet_code().equals(SUCCESS_CODE)) {
            throw new BusinessRuntimeException(resp.getRet_msg());
        }
        BindCard bindCard = new BindCard();
        bindCard.setStatus((short) BindCardStatusEnum.UNBINDED.getIndex());
        Example example = new Example(BindCard.class);
        example.createCriteria().andEqualTo("bankCardNo", req.getLinked_acctno());
        bindCardMapper.updateByExampleSelective(bindCard, example);
        return JsonObjectPage.createJsonObjectPage(resp);
    }

    /**
     * 绑卡申请
     *
     * @param req
     * @return
     */
    public JsonObjectPage<BindcardApplyResp> bindcardApply(BindcardApplyReq req) {
        BindcardApplyResp resp = AccpApi.bindcardApply(req);
        if (!resp.getRet_code().equals(SUCCESS_CODE)) {
            throw new BusinessRuntimeException(resp.getRet_msg());
        }
        redisTemplate.opsForValue().set(getRedisKey("bindcardApply_req", req.getUser_id()), req);
        redisTemplate.opsForValue().set(getRedisKey("bindcardApply_resp", req.getUser_id()), resp);
        return JsonObjectPage.createJsonObjectPage(resp);
    }

    /**
     * 绑卡验证
     *
     * @param req
     * @return
     */
    public JsonObjectPage<BindcardVerifyResp> bindcardVerify(BindcardVerifyReq req) {
        String reqKey = getRedisKey("bindcardApply_req", req.getUser_id());
        BindcardApplyReq preRequest = (BindcardApplyReq) redisTemplate.opsForValue().get(reqKey);
        if (preRequest == null) {
            throw new BusinessRuntimeException("请先调用绑卡申请接口");
        }
        String respKey = getRedisKey("bindcardApply_resp", req.getUser_id());
        BindcardApplyResp postResp = (BindcardApplyResp) redisTemplate.opsForValue().get(respKey);
        req.setToken(postResp.getToken()).setTxn_seqno(postResp.getTxn_seqno());
        BindcardVerifyResp resp = AccpApi.bindcardVerify(req);
        if (!resp.getRet_code().equals(SUCCESS_CODE)) {
            throw new BusinessRuntimeException(resp.getRet_msg());
        }
        Date date = new Date();
        BindCard bindCard = new BindCard();
        bindCard.setStatus((short) BindCardStatusEnum.BINDED.getIndex());
        bindCard.setBindCardId(snowflake.nextId());
        bindCard.setBankCardNo(preRequest.getLinked_acctno());
        bindCard.setCreateTime(date);
        bindCard.setWalletAccountId(Long.parseLong(preRequest.getUser_id()));
        bindCard.setTradeNo(resp.getTxn_seqno());
        bindCard.setAgreenNo(resp.getLinked_agrtno());
        bindCardMapper.insert(bindCard);
        redisTemplate.delete(reqKey);
        redisTemplate.delete(respKey);
        return JsonObjectPage.createJsonObjectPage(resp);
    }

    /**
     * 找回密码-验证
     */
    public JsonObjectPage<FindPasswordVerifyResp> findPasswordVerify(FindPasswordVerifyReq req) {
        String key = getRedisKey("findPasswordApply_resp", req.getUser_id());
        FindPasswordApplyResp postResp = (FindPasswordApplyResp) redisTemplate.opsForValue().get(key);
        if (postResp == null) {
            throw new BusinessRuntimeException("请先调用找回密码申请接口");
        }
        req.setToken(postResp.getToken());
        FindPasswordVerifyResp resp = AccpApi.findPasswordVerify(req);
        if (!resp.getRet_code().equals(SUCCESS_CODE)) {
            throw new BusinessRuntimeException(resp.getRet_msg());
        }
        redisTemplate.delete(key);
        return JsonObjectPage.createJsonObjectPage(resp);
    }


    /**
     * 找回密码-申请
     */
    public JsonObjectPage<FindPasswordApplyResp> findPasswordApply(FindPasswordApplyReq req) {
        FindPasswordApplyResp resp = AccpApi.findPasswordApply(req);
        if (!resp.getRet_code().equals(SUCCESS_CODE)) {
            throw new BusinessRuntimeException(resp.getRet_msg());
        }
        redisTemplate.opsForValue().set(getRedisKey("findPasswordApply_resp", req.getUser_id()), resp);
        return JsonObjectPage.createJsonObjectPage(resp);
    }


    /**
     * 获取随机因子
     *
     * @param req
     * @return
     */
    public JsonObjectPage<GetRandomResp> getRandom(GetRandomReq req) {
        GetRandomResp resp = AccpApi.getRandom(req);
        if (!resp.getRet_code().equals(SUCCESS_CODE)) {
            throw new BusinessRuntimeException(resp.getRet_msg());
        }
        return JsonObjectPage.createJsonObjectPage(resp);
    }

    /**
     * 重置密码
     *
     * @param req
     * @return
     */
    public JsonObjectPage<ChangePasswordResp> changePassword(ChangePasswordReq req) {
        ChangePasswordResp resp = AccpApi.changePassword(req);
        if (!resp.getRet_code().equals(SUCCESS_CODE)) {
            throw new BusinessRuntimeException(resp.getRet_msg());
        }

        return JsonObjectPage.createJsonObjectPage(resp);
    }

    /**
     * 支付统一创单
     *
     * @param req
     * @return
     */
    public JsonObjectPage<TradeCreateResp> createTrade(TradeCreateReq req) {
        TradeCreateResp resp = AccpApi.createTrade(req);
        if (!resp.getRet_code().equals(SUCCESS_CODE)) {
            throw new BusinessRuntimeException(resp.getRet_msg());
        }
        redisTemplate.opsForValue().set(getRedisKey("createTrade_resp", req.getUser_id()), resp, 30, TimeUnit.MINUTES);
        return JsonObjectPage.createJsonObjectPage(resp);
    }

    /**
     * 银行卡快捷支付
     *
     * @param req
     * @return
     */
    public JsonObjectPage<BankcardPayResp> bankcardPay(BankcardPayReq req) {
        TradeCreateResp postResp = (TradeCreateResp) redisTemplate.opsForValue().get(getRedisKey("createTrade_resp", req.getPayerInfo().getUser_id()));
        if (postResp == null) {
            throw new BusinessRuntimeException("请先调用支付统一创单接口");
        }
        req.setTxn_seqno(postResp.getTxn_seqno());
        BankcardPayResp resp = AccpApi.bankcardPay(req);
        if (!resp.getRet_code().equals(SUCCESS_CODE)) {
            throw new BusinessRuntimeException(resp.getRet_msg());
        }
        redisTemplate.delete(getRedisKey("createTrade_resp", req.getPayerInfo().getUser_id()));
        redisTemplate.opsForValue().set(getRedisKey("forGetToken_resp", req.getPayerInfo().getUser_id()), resp, 30, TimeUnit.MINUTES);
        return JsonObjectPage.createJsonObjectPage(resp);
    }

    /**
     * 余额支付
     *
     * @param req
     * @return
     */
    public JsonObjectPage<BalancePayResp> balancePay(BalancePayReq req) {
        TradeCreateResp postResp = (TradeCreateResp) redisTemplate.opsForValue().get(getRedisKey("createTrade_resp", req.getPayerInfo().getUser_id()));
        if (postResp == null) {
            throw new BusinessRuntimeException("请先调用支付统一创单接口");
        }
        req.setTxn_seqno(postResp.getTxn_seqno());
        BalancePayResp resp = AccpApi.balancePay(req);
        if (!resp.getRet_code().equals(SUCCESS_CODE)) {
            throw new BusinessRuntimeException(resp.getRet_msg());
        }
        redisTemplate.delete(getRedisKey("createTrade_resp", req.getPayerInfo().getUser_id()));
        redisTemplate.opsForValue().set(getRedisKey("forGetToken_resp", req.getPayerInfo().getUser_id()), resp, 30, TimeUnit.MINUTES);
        return JsonObjectPage.createJsonObjectPage(resp);
    }

    public JsonObjectPage<RefundResp> refund(RefundReq req){
        RefundResp resp = AccpApi.refund(req);
        if (!resp.getRet_code().equals(SUCCESS_CODE)) {
            throw new BusinessRuntimeException(resp.getRet_msg());
        }
        Refund refund=new Refund();
        refund.setRefundId(snowflake.nextId());
        refund.setAccountId(Long.parseLong(req.getUser_id()));
        refund.setCreateTime(new Date());
        refund.setOriginalTradeNo(req.getOriginalOrderInfo().getTxn_seqno());
        refund.setTradeNo(req.getRefundOrderInfo().getRefund_seqno());
        refund.setTotalAmount(req.getOriginalOrderInfo().getTotal_amount());
        return null;
    }


    /**
     * 交易中短信验证服务
     *
     * @param req
     * @return
     */
    public JsonObjectPage<TradeSecondSmsValidResp> tradeSecondSmsValid(TradeSecondSmsValidReq req) {
        Object postResp = redisTemplate.opsForValue().get(getRedisKey("forGetToken_resp", req.getPayer_id()));
        req.setToken(getToken(postResp));
        TradeSecondSmsValidResp resp = AccpApi.tradeSecondSmsValid(req);
        if (!resp.getRet_code().equals(SUCCESS_CODE)) {
            throw new BusinessRuntimeException(resp.getRet_msg());
        }
        redisTemplate.delete(getRedisKey("forGetToken_resp",req.getPayer_id()));
        WalletAccount walletAccount=new WalletAccount();
        walletAccount.setAccountId(Long.parseLong(resp.getUser_id()));
        List<WalletAccount> walletAccounts = walletAccountMapper.select(walletAccount);
        if(walletAccounts!=null&&walletAccounts.size()>0){
            AccountDetail accountDetail=new AccountDetail();
            accountDetail.setAccountDetailId(snowflake.nextId());
            accountDetail.setWalletAccountId(walletAccounts.get(0).getWalletAccountId());
            accountDetail.setAmount(resp.getTotal_amount());
            accountDetail.setCreateTime(new Date());
            //TODO set type remark
            accountDetailMapper.insert(accountDetail);
        }
        return JsonObjectPage.createJsonObjectPage(resp);
    }

    /**
     * @param bizKey
     * @param userId
     * @return
     */
    private String getRedisKey(String bizKey, String userId) {
        return new StringBuffer().append(this.getClass().getName()).append("_").append(bizKey).append("_").append(userId).toString();
    }

    /**
     *
     * @param postResp
     * @return
     */
    private String getToken(Object postResp) {
        if(postResp==null){
            throw new BusinessRuntimeException("请先调用支付接口");
        }
        if(postResp instanceof BankcardPayResp){
            return ((BankcardPayResp)postResp).getToken();
        }
        if(postResp instanceof BalancePayResp){
            return ((BalancePayResp)postResp).getToken();
        }
        return null;
    }
}
