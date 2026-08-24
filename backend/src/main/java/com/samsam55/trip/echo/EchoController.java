package com.samsam55.trip.echo;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
public class EchoController {

    @PostMapping("/api/v1/echo")
    public EchoResponse echo(@RequestBody EchoRequest request) {
        return new EchoResponse(request.message());
    }
}
