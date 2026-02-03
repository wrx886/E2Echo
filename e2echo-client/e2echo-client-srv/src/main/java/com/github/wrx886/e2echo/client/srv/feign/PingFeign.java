package com.github.wrx886.e2echo.client.srv.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import com.github.wrx886.e2echo.client.srv.result.Result;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Ping")
@FeignClient(name = "e2echo-server-ping", url = "$Dynamic$", path = "/server/ping")
public interface PingFeign {

    @GetMapping
    @Operation(summary = "Ping")
    public Result<Void> ping();

}
