package com.campusmart.service;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusmart.common.BizException;
import com.campusmart.common.ErrorCode;
import com.campusmart.mapper.UserMapper;
import com.campusmart.pojo.dto.LoginDTO;
import com.campusmart.pojo.dto.RegisterDTO;
import com.campusmart.pojo.entity.User;
import com.campusmart.pojo.vo.LoginVO;
import com.campusmart.pojo.vo.UserVO;
import com.campusmart.utils.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    public UserService(UserMapper userMapper,JwtUtil jwtUtil){
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterDTO dto) {
        // 1. 查手机号是否已存在 → 存在就抛 10001
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        if (count > 0){
            throw new BizException(ErrorCode.PHONE_EXISTS,"该手机号已注册");
        }

        // 2. dto 转 User（密码先存明文）
        User user = new User();
        user.setPhone(dto.getPhone());
        user.setPassword(dto.getPassword());

        // 3. nickname 为空时取手机号后 4 位
        user.setNickname(StringUtils.hasText(dto.getNickname())
                ? dto.getNickname()
                : defaultNickname(dto.getPhone()));
        user.setStatus(1);

        // 4. userMapper.insert(user)
        userMapper.insert(user);
        // 5. return user.getId()
        return user.getId();
    }

    private String defaultNickname(String phone){
        return "用户"+ phone.substring(phone.length() - 4);
    }

    public LoginVO login(LoginDTO dto) {
        // 1. 按手机号查用户（selectOne）
        //    提示：手机号有唯一索引，最多查出一条
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));

        // 2. 用户不存在 或 密码不匹配 → 抛 10002「手机号或密码错误」
        //    ⚠️ 两个情况必须合并成一个判断，不要分开抛不同的错
        if (user == null || !dto.getPassword().equals(user.getPassword())){
            throw new BizException(ErrorCode.LOGIN_FAILED,"手机号或密码错误");
        }

        // 3. 账号被禁用（status == 0）→ 抛 10004「账号已被禁用」
        if (user.getStatus() == 0){
            throw new BizException(ErrorCode.USER_DISABLED,"账号已被禁用");
        }

        // 4. jwtUtil.create(user.getId(), "USER") 生成 token
        String token = jwtUtil.create(user.getId(), "USER");
        // 5. 组装 LoginVO 返回
        return new LoginVO(token,user.getId(),user.getNickname());
    }

    public UserVO getInfo(Long userId) {
        User user = userMapper.selectById(userId);

        if (user == null){
            throw new BizException(ErrorCode.USER_NOT_FOUND,"用户不存在");
        }

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user,userVO);
        return userVO;
    }
}
