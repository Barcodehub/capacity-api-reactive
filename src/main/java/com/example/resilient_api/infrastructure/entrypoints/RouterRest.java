package com.example.resilient_api.infrastructure.entrypoints;

import com.example.resilient_api.infrastructure.entrypoints.handler.BootcampHandlerImpl;
import com.example.resilient_api.infrastructure.entrypoints.handler.CapacityHandlerImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterRest {
    @Bean
    public RouterFunction<ServerResponse> routerFunction(CapacityHandlerImpl capacityHandler,
                                                          BootcampHandlerImpl bootcampHandler) {
        return route(POST("/capacity"), capacityHandler::createCapacity)
            .andRoute(POST("/capacity/check-exists"), capacityHandler::checkCapacitiesExist)
            .andRoute(GET("/capacity"), capacityHandler::listCapacities)
            .andRoute(POST("/capacity/with-technologies"), capacityHandler::getCapacitiesWithTechnologies)
            .andRoute(POST("/capacity/delete-by-ids"), capacityHandler::deleteCapacitiesByIds)
            .andRoute(POST("/capacity/bootcamp"), bootcampHandler::createBootcamp)
            .andRoute(GET("/capacity/bootcamp"), bootcampHandler::listBootcamps)
            .andRoute(DELETE("/capacity/bootcamp/{id}"), bootcampHandler::deleteBootcamp);
    }

}
