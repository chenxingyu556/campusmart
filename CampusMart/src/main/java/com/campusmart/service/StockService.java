package com.campusmart.service;

import com.campusmart.common.BizException;
import com.campusmart.common.ErrorCode;
import com.campusmart.common.StockBizType;
import com.campusmart.mapper.SkuMapper;
import com.campusmart.mapper.StockLogMapper;
import com.campusmart.pojo.entity.StockLog;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {

    private final StockLogMapper stockLogMapper;
    private final SkuMapper skuMapper;

    // 构造函数注入...
    public StockService(StockLogMapper stockLogMapper,SkuMapper skuMapper){
        this.stockLogMapper = stockLogMapper;
        this.skuMapper = skuMapper;
    }
    /** 下单扣减 */
    @Transactional(rollbackFor = Exception.class)
    public void deduct(Long skuId, Integer num, String bizNo) {

        // ① 插一条流水，change_num 是负数
        StockLog log = new StockLog();
        log.setSkuId(skuId);
        log.setBizType(StockBizType.DEDUCT);
        log.setBizNo(bizNo);
        log.setChangeNum(-num);
        stockLogMapper.insert(log);

        // ② 扣库存：skuMapper.deductStock(skuId, num)
        int row = skuMapper.deductStock(skuId, num);

        // ③ rows == 0 → 抛 STOCK_NOT_ENOUGH（事务回滚，①的流水一起撤掉）
        if (row == 0){
            throw new BizException(ErrorCode.STOCK_NOT_ENOUGH,"库存不足");
        }
    }

    /** 归还，天然幂等 */
    @Transactional(rollbackFor = Exception.class)
    public void restore(Long skuId, Integer num, String bizNo) {
        StockLog log = new StockLog();
        log.setSkuId(skuId);
        log.setBizType(StockBizType.RESTORE);
        log.setBizNo(bizNo);
        log.setChangeNum(num);

        try {
            stockLogMapper.insert(log);
        } catch (DuplicateKeyException e) {
            // ← 这一步是关键：撞了唯一键说明什么？想清楚再写
            return;
        }

        skuMapper.restoreStock(skuId, num);
    }
}
