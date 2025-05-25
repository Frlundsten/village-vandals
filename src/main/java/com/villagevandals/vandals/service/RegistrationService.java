package com.villagevandals.vandals.service;

import com.villagevandals.vandals.model.domain.User;
import com.villagevandals.vandals.model.domain.Village;
import com.villagevandals.vandals.service.user.UserService;
import com.villagevandals.vandals.service.village.VillageService;
import org.springframework.stereotype.Service;

@Service
public class RegistrationService {

    UserService userService;
    VillageService villageService;

    public RegistrationService(UserService userService, VillageService villageService) {
        this.userService = userService;
        this.villageService = villageService;
    }

    public void newUser(String username, String rawPassword ) {
           User user = userService.newUser(username, rawPassword);
           Village village = villageService.starterVillage(user);

           user.villages().add(village);

           userService.saveNewUser(user);
    }
}
