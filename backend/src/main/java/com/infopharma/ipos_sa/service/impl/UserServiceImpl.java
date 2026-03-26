package com.infopharma.ipos_sa.service.impl;

import com.infopharma.ipos_sa.dto.CreateUserAccountRequest;
import com.infopharma.ipos_sa.entity.UserAccount;
import com.infopharma.ipos_sa.repository.UserAccountRepository;
import com.infopharma.ipos_sa.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private UserAccountRepository userAccountRepository;

    public UserServiceImpl(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UserAccount createAccount(UserAccount account){
        return userAccountRepository.save(account);
    }

//    @Override
//    public UserAccount assignRole(String userId, String role){
//
//    }
//
//    @Override
//    public UserAccount updateRole(String userId, String role){
//
//    }
}
