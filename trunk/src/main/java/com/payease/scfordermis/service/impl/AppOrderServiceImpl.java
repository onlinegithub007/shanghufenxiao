package com.payease.scfordermis.service.impl;

import com.google.common.collect.Lists;
import com.payease.scfordermis.bo.ResultBo;
import com.payease.scfordermis.bo.responseBo.PageResponseCommBean;
import com.payease.scfordermis.bo.responseBo.app.customer.RespAppCustomerOrderBean;
import com.payease.scfordermis.bo.responseBo.app.customer.RespAppCustomerOrderDetailBean;
import com.payease.scfordermis.bo.responseBo.app.customer.RespAppPayDetailBean;
import com.payease.scfordermis.bo.responseBo.app.customer.RespAppPayHistoryBean;
import com.payease.scfordermis.bo.responseBo.app.driver.RespAppGoodBean;
import com.payease.scfordermis.bo.responseBo.app.driver.RespAppOrderBean;
import com.payease.scfordermis.common.constant.OrderEntity;
import com.payease.scfordermis.common.constant.RedisConstant;
import com.payease.scfordermis.dao.*;
import com.payease.scfordermis.entity.*;
import com.payease.scfordermis.exception.CommonRuntimeException;
import com.payease.scfordermis.service.IAppOrderService;
import com.payease.scfordermis.utils.DateUtil;
import com.payease.scfordermis.utils.PageUtil;
import com.payease.scfordermis.utils.RandomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.payease.scfordermis.bean.Status.PICKCODE;

/**
 * Created by zhangzhili on 2018/1/19.
 */
@Service
public class AppOrderServiceImpl implements IAppOrderService {

    private static final Logger log = LoggerFactory.getLogger(AppOrderServiceImpl.class);
    @Autowired
    OrderDao orderDao;
    @Autowired
    TransportOrderDao transportOrderDao;
    @Autowired
    TransportOrderDetailDao transportOrderDetailDao;
    @Autowired
    UnitInfoDao unitInfoDao;
    @Autowired
    ProductInfoDao productInfoDao;
    @Autowired
    ProductFormatDao productFormatDao;
    @Autowired
    OrderDetailDao orderDetailDao;
    @Autowired
    PaymentHistoryDao paymentHistoryDao;
    @Autowired
    ConsumerInfoDao consumerInfoDao;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Resource(name = "stringRedisTemplate")
    ValueOperations<String, String> valOps;
    @Autowired
    private NoteDao noteDao;
    @Autowired
    private ConsumerMsgDao consumerMsgDao;



    @Override
    public PageResponseCommBean<RespAppCustomerOrderBean> findAllOrderNotComplete(Long companyId,
                                                                                  int status,
                                                                                  Pageable
                                                                                          pageable) {
        Page<TOrderEntity> orderEntities;
        if (status == 1) {
            orderEntities =
                orderDao.findByFOrderStatus(OrderEntity.OrderPayStatus.Wait.getValue(),
                    OrderEntity.OrderPayStatus.Pary.getValue(), pageable);
        } else if (status == 0) {
            orderEntities = orderDao.findAll(pageable);
        } else {
            log.error("STATUS参数错误：status:{}", String.valueOf(status));
            throw new RuntimeException("STATUS参数错误");
        }

        PageResponseCommBean result = PageUtil.createPageResponseCommBean(orderEntities);
        List<RespAppCustomerOrderBean> lists = Lists.newArrayList();
        for (TOrderEntity order : orderEntities) {
            RespAppCustomerOrderBean bean = new RespAppCustomerOrderBean();
            bean.setCreatetime(DateUtil.formatNewDatetoString(order.getfCreatetime()));
            bean.setOrderAmount(order.getfOrderAmountPay().toString());
            bean.setOrderId(order.getId());
            bean.setOrderNum(order.getfOrderNum());
            bean.setPayStatus(order.getfPayStatus());
            bean.setProductNum(order.getfProductNum());
            bean.setStatus(order.getfOrderStatus());
            lists.add(bean);
        }
        result.setContent(lists);
        return result;
    }

    @Override
    public RespAppCustomerOrderDetailBean getOrderDetail(Long companyId, Long id) {
        TOrderEntity order = this.getOrder(companyId, id);
        TTransportOrderEntity transportOrderEntity =
            transportOrderDao.findOne(order.getfTransportOrderId());
        RespAppCustomerOrderDetailBean bean = new RespAppCustomerOrderDetailBean();
        bean.setOrderId(order.getId());
        bean.setOrderNum(order.getfOrderNum());
        bean.setCreatetime(DateUtil.formatNewDatetoString(order.getfCreatetime()));
        bean.setStatus(order.getfOrderStatus());
        bean.setProductNum(order.getfProductNum());
        bean.setSpecial(order.getfSpecial());
        bean.setAmount(order.getfAmount().toString());
        bean.setOrderAmount(order.getfOrderAmountPay().toString());
        bean.setPaidAmount(order.getfPaidMoney().toString());
        bean.setPayStatus(order.getfPayStatus());
        bean.setCardNum(transportOrderEntity.getfCarNum());
        bean.setDriverName(transportOrderEntity.getfDriverName());
        bean.setDriverPhone(transportOrderEntity.getfDriverPhone());

        //查询运单详情
        List<TOrderDetailEntity> orderDetails = orderDetailDao.findByFOrderId(order.getId());

        //组装商品详情
        bean.setGoodBeanList(this.getListOrderGoods(orderDetails));
        return bean;
    }

    @Override
    public RespAppPayDetailBean getPayDetails(Long companyId, Long id) {

        TOrderEntity order = this.getOrder(companyId, id);
        RespAppPayDetailBean respAppPayDetailBean = new RespAppPayDetailBean();
        respAppPayDetailBean.setOrderId(order.getId());
        respAppPayDetailBean.setOrderAmount(order.getfOrderAmountPay().toString());
        respAppPayDetailBean.setPaidAmount(order.getfPaidMoney().toString());
        respAppPayDetailBean.setPayStatus(order.getfPayStatus());

        List<TPaymentHistoryEntity> paymentList = paymentHistoryDao.findByFOrderId(order.getId());


        List<RespAppPayHistoryBean> respAppPayHistoryBeans = new ArrayList<>();
        for (TPaymentHistoryEntity payment : paymentList) {
            RespAppPayHistoryBean appOrderBean = new RespAppPayHistoryBean();
            appOrderBean.setPayAmount(payment.getfAmount().toString());
            appOrderBean.setPayMethod(payment.getfPaymethod());
            appOrderBean.setPayNum(payment.getfPayNum());
            appOrderBean.setPaytime(DateUtil.formatNewDatetoString(payment.getfCreatetime()));
            appOrderBean.setPayRemark(StringUtils.isEmpty(payment.getfRemark()) ?
                "无" :
                payment.getfRemark());
            respAppPayHistoryBeans.add(appOrderBean);
        }
        respAppPayDetailBean.setHistoryBeanList(respAppPayHistoryBeans);
        return respAppPayDetailBean;
    }


    @Override
    public RespAppPayDetailBean getOrderMount(Long companyId, Long id) {
        TOrderEntity order = this.getOrder(companyId, id);
        RespAppPayDetailBean respAppPayDetailBean = new RespAppPayDetailBean();
        respAppPayDetailBean.setOrderId(order.getId());
        respAppPayDetailBean.setOrderAmount(order.getfOrderAmountPay().toString());
        respAppPayDetailBean.setPaidAmount(order.getfPaidMoney().toString());
        respAppPayDetailBean.setPayStatus(order.getfPayStatus());
        return respAppPayDetailBean;
    }



    @Override
    public RespAppPayDetailBean makeAgreement(String customerName, Long companyId, Long id) {
        TOrderEntity order = this.getOrder(companyId, id);
        RespAppPayDetailBean respAppPayDetailBean = new RespAppPayDetailBean();
        respAppPayDetailBean.setOrderId(order.getId());
        respAppPayDetailBean.setOrderAmount(order.getfOrderAmountPay().toString());
        respAppPayDetailBean.setPaidAmount(order.getfPaidMoney().toString());
        respAppPayDetailBean.setPayStatus(order.getfPayStatus());
        respAppPayDetailBean.setCustomer(customerName);
        respAppPayDetailBean.setOrderNum(order.getfOrderNum());
        return respAppPayDetailBean;
    }



    @Override
    public RespAppOrderBean getPickCode(String customerName, Long companyId, Long id) {

        TOrderEntity order = this.getOrder(companyId, id);
        TTransportOrderEntity transportOrderEntity =
            transportOrderDao.findOne(order.getfTransportOrderId());
        RespAppOrderBean respAppOrderBean = new RespAppOrderBean();
        respAppOrderBean.setOrderId(order.getId());
        respAppOrderBean.setCustomerName(customerName);
        respAppOrderBean.setOrderNum(order.getfOrderNum());
        respAppOrderBean.setProductCategoryNum(order.getfProductCategoryNum());
        respAppOrderBean.setProductNum(order.getfProductNum());
        respAppOrderBean.setStatus(OrderEntity.OrderStatus.orderStatusEnum.get(String.valueOf
            (order.getfOrderStatus())).getViewPage());
        respAppOrderBean.setPickUpCode(this.createPickCode(companyId, order.getfTransportOrderId
            (), id, order.getfOrderNum(), transportOrderEntity.getfTransportNum()));
        respAppOrderBean.setCardNum(transportOrderEntity.getfCarNum());
        respAppOrderBean.setDriverName(transportOrderEntity.getfDriverName());
        respAppOrderBean.setDriverPhone(transportOrderEntity.getfDriverPhone());

        List<TOrderDetailEntity> orderDetails = orderDetailDao.findByFOrderId(order.getId());

        respAppOrderBean.setGoodBeanList(this.getListOrderGoods(orderDetails));
        return respAppOrderBean;
    }



    @Override
    public List<RespAppOrderBean> driverOrders(Long companyId, Long orderStatus, long tranOrderId) {

        List<TOrderEntity> orders = null;

        if (orderStatus != null && orderStatus.longValue() == 1) {
            orders =
                orderDao.findByFOrderStatusAndFTransportOrderIdOrderByIdAsc(Integer.parseInt
                    (OrderEntity.OrderStatus.ForPick.getValue()), tranOrderId);
        } else if (orderStatus == null || orderStatus.longValue() == 0) {
            orders =
                orderDao
                    .findByFOrderStatusGreaterThanEqualAndFTransportOrderIdOrderByFOrderStatusAsc
                        (Integer.parseInt(OrderEntity.OrderStatus.ForPick.getValue()), tranOrderId);
        } else {
            throw new RuntimeException("参数不合法orderStatus：{" + orderStatus + "}");
        }
        List<RespAppOrderBean> beanList = Lists.newArrayList();


        List<TConsumerInfoEntity> customList = consumerInfoDao.findByFCompanyId(companyId);
        Map<Long, String> customMap =
            customList.stream().collect(Collectors.toMap(TConsumerInfoEntity::getfId,
                TConsumerInfoEntity::getfName));
        for (TOrderEntity order : orders) {
            RespAppOrderBean respAppOrderBean = new RespAppOrderBean();
            respAppOrderBean.setOrderId(order.getId());
            respAppOrderBean.setCustomerName(customMap.get(order.getfConsumerId()));
            respAppOrderBean.setOrderNum(order.getfOrderNum());
            respAppOrderBean.setProductCategoryNum(order.getfProductCategoryNum());
            respAppOrderBean.setProductNum(order.getfProductNum());
            respAppOrderBean.setStatus(OrderEntity.OrderStatus.orderStatusEnum.get(String.valueOf
                (order.getfOrderStatus())).getViewPage());
            beanList.add(respAppOrderBean);
        }
        return beanList;
    }

    @Override
    public RespAppOrderBean getDriverOrderDetail(Long tranOrderId, long orderId) {


        TOrderEntity orderEntity = this.getOrderTargetAppDriver(tranOrderId, orderId);
        TConsumerInfoEntity tConsumerInfoEntity =
            consumerInfoDao.findOne(orderEntity.getfConsumerId());
        RespAppOrderBean respAppOrderBean = new RespAppOrderBean();


        respAppOrderBean.setOrderId(orderEntity.getId());
        respAppOrderBean.setCustomerName(tConsumerInfoEntity.getfName());
        respAppOrderBean.setOrderNum(orderEntity.getfOrderNum());
        respAppOrderBean.setProductCategoryNum(orderEntity.getfProductCategoryNum());
        respAppOrderBean.setProductNum(orderEntity.getfProductNum());
        respAppOrderBean.setStatus(OrderEntity.OrderStatus.orderStatusEnum.get(String.valueOf
            (orderEntity.getfOrderStatus())).getViewPage());


        respAppOrderBean.setGoodBeanList(this.getListOrderGoods(orderDetailDao.findByFOrderId
            (orderEntity.getId())));

        return respAppOrderBean;
    }

    @Override
    public RespAppOrderBean getScanPickCode(Long companyId, String tranOrderNum, Long
        tranOrderId, String pickCode) {
        Long orderId = this.scanPickCodeToOrderId(companyId, tranOrderNum, pickCode);
        return this.getDriverOrderDetail(tranOrderId, orderId);
    }

    @Override
    @Transactional
    public String pickGoods(Long tranOrderId, Long orderId) {
        int result =
            orderDao.updateOrderStatus(orderId.longValue(), Integer.parseInt(OrderEntity
                .OrderStatus.Completed.getValue()), tranOrderId.longValue());
        if (result != 1) {
            log.error("提货失败Long tranOrderId:{}, Long orderId:{}", tranOrderId.longValue(),
                orderId.longValue());
            throw new RuntimeException("result 更新异常");
        }
        TOrderEntity orderEntity = orderDao.findOne(orderId);
        TTransportOrderEntity transportOrderEntity = transportOrderDao.findOne(tranOrderId);
        //todo  添加日志====添加消息
        TNoteEntity tNoteEntity = new TNoteEntity();
        tNoteEntity.setfCompanyId(orderEntity.getfCompanyId());//公司id
        tNoteEntity.setfCreatetime(new Date());//时间
        tNoteEntity.setfOperate(orderEntity.getfConsumerId());//用户id
        tNoteEntity.setfPartyType("merchants");//商户操作
        tNoteEntity.setfTransportOrderId(orderId);//订单号
        tNoteEntity.setfType("order");//日志类型（订单操作日志）
        tNoteEntity.setfOperator(transportOrderEntity.getfDriverName());//操作者名称
        tNoteEntity.setfNote("提货完成");
        tNoteEntity.setfOperatingType("deliveryCompletion");
        noteDao.save(tNoteEntity);


        //封装客户消息对象（公共部分）
        TConsumerMsgEntity tConsumerMsgEntity = new TConsumerMsgEntity();

        tConsumerMsgEntity.setfConsumerId(orderEntity.getfConsumerId());//客户id
        tConsumerMsgEntity.setfCompanyId(orderEntity.getfCompanyId());//公司id
        tConsumerMsgEntity.setfMsgType("order");//消息类型 order:订单消息
        tConsumerMsgEntity.setfLinkId(orderId);//外链id 对应订单表主键id
        tConsumerMsgEntity.setfMsgStatus("unread");//消息状态 read：已读 unread:未读
        tConsumerMsgEntity.setfCreateTime(new Date());//创建时间
        tConsumerMsgEntity.setfMsgDesc("您的订单已提货，订单号" + orderEntity.getfOrderNum());
        tConsumerMsgEntity.setfMsgTitle("订单提货");
        tConsumerMsgEntity.setfRemark(OrderEntity.OrderStatus.Completed.getValue());
        consumerMsgDao.save(tConsumerMsgEntity);


        return this.getOrderTargetAppDriver(tranOrderId, orderId).getfOrderNum();
    }


    /**
     * 根据二维码返回订单号
     *
     * @param pickCode 提货码
     * @return
     */
    private long scanPickCodeToOrderId(Long companyId, String tranOrderNum, String pickCode) {
        if (stringRedisTemplate.hasKey(this.prefixRedisKey(companyId) + tranOrderNum + pickCode)) {
            return Long.parseLong(valOps.get(
                this.prefixRedisKey(companyId) + tranOrderNum + pickCode));
        } else {
            //二维码失效
            throw new CommonRuntimeException(PICKCODE);
        }
    }



    /**
     * 生产提货码
     *
     * @param companyId    公司id
     * @param tranId       运单id
     * @param orderId      订单id
     * @param orderNum     订单号
     * @param tranOrderNum 运单号
     * @return
     */
    private String createPickCode(Long companyId, Long tranId, Long orderId, String orderNum,
                                  String tranOrderNum) {
        System.out.println("进入方法体");
        //todo  生产提货码并存入redis设置有效期
        String pick = RandomUtil.RandomNumber(8);
        if (stringRedisTemplate.hasKey(this.prefixRedisKey(companyId) + orderNum)) {
            return valOps.get(this.prefixRedisKey(companyId) + orderNum);
        }

        valOps.set(this.prefixRedisKey(companyId) + orderNum, pick, 5, TimeUnit.MINUTES);
        //二维码的key
        valOps.set(
            this.prefixRedisKey(companyId) + tranOrderNum + pick,
            "" + orderId, 5, TimeUnit.MINUTES);

        return pick;
    }


    /**
     * 生产redis前缀
     *
     * @param companyId 公司id
     * @return 返回redis前缀
     */
    private String prefixRedisKey(Long companyId) {
        return RedisConstant.REDIS_SYSTEM + companyId;
    }



    /**
     * 拼装商品详情参数
     *
     * @param orderDetails
     * @return
     */
    private List<RespAppGoodBean> getListOrderGoods(List<TOrderDetailEntity> orderDetails) {
        //初始化返回参数
        List<RespAppGoodBean> goosList = Lists.newArrayList();

        //查询组装数据
        TProductInfoEntity tProductInfoEntity = null;
        TProductFormatEntity tProductFormatEntity = null;
        TUnitInfoEntity tUnitInfoEntity = null;
        TTransportOrderDetailEntity tTransportOrderDetailEntity = null;
        for (TOrderDetailEntity orderDetail : orderDetails) {
            tTransportOrderDetailEntity =
                transportOrderDetailDao.findOne(orderDetail.getfTranOrderDetailId());
            tProductFormatEntity =
                productFormatDao.findOne(tTransportOrderDetailEntity.gettProductFormatId());
            tUnitInfoEntity = unitInfoDao.findOne(tTransportOrderDetailEntity.gettUnitInfoId());
            tProductInfoEntity =
                productInfoDao.findOne(tTransportOrderDetailEntity.getfProductId());
            RespAppGoodBean goodBean = new RespAppGoodBean();
            goodBean.setGoodPrice(orderDetail.gettProductPrice().toString());
            goodBean.setFormateName(tProductFormatEntity.getfName());
            goodBean.setGoodNum(orderDetail.getfProductNum());
            goodBean.setUnitName(tUnitInfoEntity.getfName());
            goodBean.setGoodName(tProductInfoEntity.getfName());
            goodBean.setImgUrl(tProductFormatEntity.getfPic());
            goosList.add(goodBean);
        }
        return goosList;
    }



    /**
     * 获取订单
     *
     * @param companyId
     * @param id
     * @return
     */
    private TOrderEntity getOrder(Long companyId, Long id) {
        TOrderEntity order = orderDao.findOne(id);
        if (order.getfCompanyId() != companyId.longValue()) {
            log.error("非法操作find order getfCompanyId:{},paramscompanyId:{}", order.getfCompanyId()
                , companyId);
            throw new RuntimeException("非法操作");
        }
        return order;
    }



    /**
     * 获取订单
     *
     * @param tranOrderId
     * @param id
     * @return
     */
    private TOrderEntity getOrderTargetAppDriver(Long tranOrderId, Long id) {
        TOrderEntity order = orderDao.findOne(id);
        if (order.getfTransportOrderId() != tranOrderId.longValue()) {
            log.error("非法操作find order gettranOrderId:{},paramtranOrderId:{}", order
                .getfTransportOrderId(), tranOrderId);
            throw new RuntimeException("非法操作");
        }
        return order;
    }
    /**
     * 获取订单公开
     *
     * @param companyId
     * @param id
     * @return
     */
    @Override
    public TOrderEntity getOrderPublic(Long companyId, Long id) {
        TOrderEntity order = orderDao.findOne(id);
        if (order.getfCompanyId() != companyId.longValue()) {
            log.error("非法操作find order getfCompanyId:{},paramscompanyId:{}", order.getfCompanyId()
                    , companyId);
            throw new RuntimeException("非法操作");
        }
        return order;
    }
    /**
     * 修改订单
     *
     * @param orderId
     * @param companyId
     * @param payMoney
     * @return
     */
    @Override
    @Transactional
    public int updateOrder(long orderId,long companyId,BigDecimal payMoney){
        int result = orderDao.updateOrderPaidMoney(orderId,companyId,payMoney);
        if (result != 1) {
            log.error("模拟支付Long orderId:{}, Long companyId:{},Long payMoney{}", orderId,
                    companyId,payMoney);
            throw new RuntimeException("result 更新异常");
        }
        return result;
    }
    /**
     * 添加支付流水
     *
     * @param paymentHistoryEntity
     * @return
     */
    @Override
    @Transactional
    public int savePaymentHistory(TPaymentHistoryEntity paymentHistoryEntity){
        int result = 0;
        paymentHistoryDao.save(paymentHistoryEntity);
        result = 1;
        return result;
    }
}
