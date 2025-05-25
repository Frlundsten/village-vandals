package com.villagevandals.vandals.controller.village;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/village")
public class VillageController {

    @GetMapping
    public String village() {
        return "Your village";
    }

}
