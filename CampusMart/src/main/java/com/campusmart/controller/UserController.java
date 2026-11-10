package com.campusmart.controller;

import com.campusmart.common.Result;
import com.campusmart.common.UserContext;
import com.campusmart.pojo.dto.LoginDTO;
import com.campusmart.pojo.dto.RegisterDTO;
import com.campusmart.pojo.vo.LoginVO;
import com.campusmart.pojo.vo.UserVO;
import com.campusmart.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequestMapping("/api/user")
@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<Long> register(@RequestBody @Valid RegisterDTO dto){
        log.info("注册用户：phone={}",dto.getPhone());
        Long userId = userService.register(dto);
        return Result.ok(userId);
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO dto){
        log.info("登录用户:phone={}",dto.getPhone());
        return Result.ok(userService.login(dto));
    }

    @GetMapping("/info")
    public Result<UserVO> getInfo(){
        Long userId = UserContext.get();
        return Result.ok(userService.getInfo(userId));
    }
}
