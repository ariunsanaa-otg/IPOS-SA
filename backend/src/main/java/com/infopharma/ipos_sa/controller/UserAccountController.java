package com.infopharma.ipos_sa.controller;

import com.infopharma.ipos_sa.dto.CreateUserAccountRequest;
import com.infopharma.ipos_sa.entity.UserAccount;
import com.infopharma.ipos_sa.mapper.Mapper;
import com.infopharma.ipos_sa.mapper.impl.UserAccountMapper;
import com.infopharma.ipos_sa.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserAccountController {

    private Mapper<UserAccount, CreateUserAccountRequest> userAccountMapper;

    private final UserService userService;

    public UserAccountController(Mapper<UserAccount, CreateUserAccountRequest> userAccountMapper, UserService userService) {
        this.userService = userService;
        this.userAccountMapper = userAccountMapper;
    }

    @PostMapping("/api/accounts")
    public ResponseEntity<CreateUserAccountRequest> createAccount(@RequestBody CreateUserAccountRequest account) {


        UserAccount userAccount = userAccountMapper.mapFrom(account);


        UserAccount newAccount = userService.createAccount(userAccount);


        CreateUserAccountRequest newAccountRequest = userAccountMapper.mapTo(newAccount);

        return new ResponseEntity<>(newAccountRequest, HttpStatus.CREATED);

    }
}
