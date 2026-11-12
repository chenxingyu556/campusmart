package com.campusmart.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campusmart.common.BizException;
import com.campusmart.common.ErrorCode;
import com.campusmart.common.UserContext;
import com.campusmart.mapper.AddressMapper;
import com.campusmart.pojo.dto.AddressSaveDTO;
import com.campusmart.pojo.entity.Address;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

@Service
public class AddressService {

    private final AddressMapper addressMapper;

    public AddressService(AddressMapper addressMapper){
        this.addressMapper = addressMapper;
    }

    // 当前用户所有地址，默认的排前面
    public List<Address> list(){
        Long userId = UserContext.get();

        return addressMapper.selectList(new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId)
                .orderByDesc(Address::getIsDefault)
                .orderByDesc(Address::getCreatedAt));
    }

    // 新增，返回 id
    @Transactional(rollbackFor = Exception.class)
    public Long save(AddressSaveDTO dto){
        Long userId = UserContext.get();

        // 如果这条要设为默认，先把该用户的其他地址全部取消默认
        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            addressMapper.update(null, new LambdaUpdateWrapper<Address>()
                    .eq(Address::getUserId, userId)
                    .set(Address::getIsDefault, false));
        }

        Address address = new Address();
        address.setUserId(userId);
        address.setReceiverName(dto.getReceiverName());
        address.setReceiverPhone(dto.getReceiverPhone());
        address.setCity(dto.getCity());
        address.setProvince(dto.getProvince());
        address.setDistrict(dto.getDistrict());
        address.setDetail(dto.getDetail());
        address.setIsDefault(Boolean.TRUE.equals(dto.getIsDefault()));
        addressMapper.insert(address);

        return address.getId();

    }

    // 修改
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AddressSaveDTO dto){

        Long userId = UserContext.get();

        Address address = addressMapper.selectOne(new LambdaQueryWrapper<Address>()
                .eq(Address::getId,id).eq(Address::getUserId,userId));

        if (address == null){
            throw new BizException(ErrorCode.NOT_FOUND,"地址不存在");
        }

        address.setReceiverName(dto.getReceiverName());
        address.setReceiverPhone(dto.getReceiverPhone());
        address.setProvince(dto.getProvince());
        address.setCity(dto.getCity());
        address.setDistrict(dto.getDistrict());
        address.setDetail(dto.getDetail());

        int rows = addressMapper.updateById(address);
        if (rows == 0) {
            throw new BizException(ErrorCode.NOT_FOUND, "地址不存在或已被删除");
        }
    }

    //删除
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id){
        Long userId = UserContext.get();

        Address target = addressMapper.selectOne(new LambdaQueryWrapper<Address>()
                .eq(Address::getId, id).eq(Address::getUserId, userId));

        if (target == null){
            throw new BizException(ErrorCode.NOT_FOUND,"地址不存在或已被删除");
        }

        addressMapper.deleteById(target.getId());
        if (Boolean.TRUE.equals(target.getIsDefault())){
            Address latest = addressMapper.selectOne(new LambdaQueryWrapper<Address>()
                    .eq(Address::getUserId, userId)
                    .orderByDesc(Address::getCreatedAt).last("LIMIT 1"));

            if (latest != null){
                latest.setIsDefault(true);
                addressMapper.updateById(latest);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id){
        Long userId = UserContext.get();

        addressMapper.update(null,new LambdaUpdateWrapper<Address>()
                .eq(Address::getUserId,userId)
                .set(Address::getIsDefault,false));

        int rows = addressMapper.update(null, new LambdaUpdateWrapper<Address>()
                .eq(Address::getUserId, userId)
                .eq(Address::getId, id)
                .set(Address::getIsDefault, true));

        if (rows == 0){
            throw new BizException(ErrorCode.NOT_FOUND,"地址不存在");
        }
    }

}
