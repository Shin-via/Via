package com.via.shinvia.mydata.controller;

import com.via.shinvia.mydata.service.MyDataAuthService;
import com.via.shinvia.mydata.service.MyDataConnectionService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/mydata")
public class MyDataConnectionController {

    private final MyDataAuthService myDataAuthService;
    private final MyDataConnectionService myDataConnectionService;

    public MyDataConnectionController(MyDataAuthService myDataAuthService, MyDataConnectionService myDataConnectionService) {
        this.myDataAuthService = myDataAuthService;
        this.myDataConnectionService = myDataConnectionService;
    }

    @GetMapping("/connection")
    public String connectionPage() {
        return "mydata/connection";
    }

    @GetMapping("/callback")
    public String callback(@RequestParam("state") String state,
                           @RequestParam("code") String code) {
        Long connectionId = Long.valueOf(state);

        try{
            myDataAuthService.issueTokens(state, code);
            myDataConnectionService.completeConnection(connectionId);

            return "redirect:/mydata/result";
        }catch(Exception e) {
            myDataConnectionService.failConnection(connectionId);
            return "redirect:/mydata/connection?error";
        }
    }
}
