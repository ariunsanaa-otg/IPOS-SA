package com.infopharma.ipos_sa.service.impl;

import com.infopharma.ipos_sa.dto.CreateUserAccountRequest;
import com.infopharma.ipos_sa.entity.UserAccount;
import com.infopharma.ipos_sa.repository.UserAccountRepository;
import com.infopharma.ipos_sa.service.UserService;
import org.springframework.stereotype.Service;

import java.util.Optional;

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

    @Override
    public UserAccount updateAccount(UserAccount account){
        return userAccountRepository.save(account);
    }

    @Override
    public void deleteAccount(Long id){
        userAccountRepository.deleteById(String.valueOf(id));
    }

    @Override
    public Optional<UserAccount> findOne(Long id){
        return userAccountRepository.findById(String.valueOf(id));
    }


}
