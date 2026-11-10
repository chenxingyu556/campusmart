package com.campusmart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusmart.pojo.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    // OrderMapper
    @Update("UPDATE t_order SET status = 20, pay_time = NOW() " +
            "WHERE id = #{id} AND user_id = #{userId} AND status = 10")
    int paySuccess(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE t_order SET status = 50, close_time = NOW() WHERE id = #{id} AND user_id = #{userId} AND status = 10")
    int cancel(@Param("id") Long id, @Param("userId") Long userId);

    // 捞一批过期订单的 id
    @Select("SELECT id FROM t_order " +
            "WHERE status = 10 AND expire_time < NOW() " +
            "ORDER BY id LIMIT 200")
    List<Long> selectExpiredOrderIds();

    // 关掉其中一条
    @Update("UPDATE t_order SET status = 60, close_time = NOW() " +
            "WHERE id = #{id} AND status = 10 AND expire_time < NOW()")
    int closeExpired(@Param("id") Long id);

    @Update("UPDATE t_order SET status = 60, close_time = NOW() " +
            "WHERE id = #{id} AND status = 10")
    int closeById(@Param("id") Long id);

}
