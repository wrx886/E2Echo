package com.github.wrx886.e2echo.server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.wrx886.e2echo.server.result.Result;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Ping")
@RestController
@RequestMapping("/server/ping")
public class PingController {

    @GetMapping
    @Operation(summary = "Ping")
    public Result<Void> ping() {
        return Result.ok();
    }

}
