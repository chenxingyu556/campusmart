package com.campusmart.controller;

import com.campusmart.common.Result;
import com.campusmart.pojo.dto.AddressSaveDTO;
import com.campusmart.pojo.entity.Address;
import com.campusmart.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/address")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService){

        this.addressService = addressService;
    }

    @GetMapping("/list")
    public Result<List<Address>> list(){
        return Result.ok(addressService.list());
    }

    @PostMapping
    public Result<Long> save(@RequestBody @Valid AddressSaveDTO dto){
        return Result.ok(addressService.save(dto));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,@RequestBody @Valid AddressSaveDTO dto){
        addressService.update(id,dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id){
        addressService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id){
        addressService.setDefault(id);
        return Result.ok();
    }
}
