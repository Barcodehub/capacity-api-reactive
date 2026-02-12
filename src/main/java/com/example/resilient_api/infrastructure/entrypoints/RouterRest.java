package com.example.resilient_api.infrastructure.entrypoints;

import com.example.resilient_api.infrastructure.entrypoints.handler.BootcampHandlerImpl;
import com.example.resilient_api.infrastructure.entrypoints.handler.CapacityHandlerImpl;
import com.example.resilient_api.infrastructure.entrypoints.handler.EnrollmentHandlerImpl;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterRest {

    @Bean
    @RouterOperations({
        // Capacidades
        @RouterOperation(path = "/capacity", method = RequestMethod.POST, beanClass = CapacityHandlerImpl.class, beanMethod = "createCapacity"),
        @RouterOperation(path = "/capacity/check-exists", method = RequestMethod.POST, beanClass = CapacityHandlerImpl.class, beanMethod = "checkCapacitiesExist"),
        @RouterOperation(path = "/capacity", method = RequestMethod.GET, beanClass = CapacityHandlerImpl.class, beanMethod = "listCapacities"),
        @RouterOperation(path = "/capacity/with-technologies", method = RequestMethod.POST, beanClass = CapacityHandlerImpl.class, beanMethod = "getCapacitiesWithTechnologies"),
        @RouterOperation(path = "/capacity/delete-by-ids", method = RequestMethod.POST, beanClass = CapacityHandlerImpl.class, beanMethod = "deleteCapacitiesByIds"),
        // Bootcamps
        @RouterOperation(path = "/capacity/bootcamp", method = RequestMethod.POST, beanClass = BootcampHandlerImpl.class, beanMethod = "createBootcamp"),
        @RouterOperation(path = "/capacity/bootcamp", method = RequestMethod.GET, beanClass = BootcampHandlerImpl.class, beanMethod = "listBootcamps"),
        @RouterOperation(path = "/capacity/bootcamp/{id}", method = RequestMethod.DELETE, beanClass = BootcampHandlerImpl.class, beanMethod = "deleteBootcamp"),
        // Inscripciones
        @RouterOperation(path = "/capacity/bootcamp/enroll", method = RequestMethod.POST, beanClass = EnrollmentHandlerImpl.class, beanMethod = "enrollUser"),
        @RouterOperation(path = "/capacity/bootcamp/{bootcampId}/unenroll", method = RequestMethod.DELETE, beanClass = EnrollmentHandlerImpl.class, beanMethod = "unenrollUser"),
        @RouterOperation(path = "/capacity/bootcamp/my-bootcamps", method = RequestMethod.GET, beanClass = EnrollmentHandlerImpl.class, beanMethod = "getUserBootcamps")
    })
    public RouterFunction<ServerResponse> routerFunction(CapacityHandlerImpl capacityHandler,
                                                          BootcampHandlerImpl bootcampHandler,
                                                          EnrollmentHandlerImpl enrollmentHandler) {
        return route(POST("/capacity"), capacityHandler::createCapacity)
            .andRoute(POST("/capacity/check-exists"), capacityHandler::checkCapacitiesExist)
            .andRoute(GET("/capacity"), capacityHandler::listCapacities)
            .andRoute(POST("/capacity/with-technologies"), capacityHandler::getCapacitiesWithTechnologies)
            .andRoute(POST("/capacity/delete-by-ids"), capacityHandler::deleteCapacitiesByIds)
            .andRoute(POST("/capacity/bootcamp"), bootcampHandler::createBootcamp)
            .andRoute(GET("/capacity/bootcamp"), bootcampHandler::listBootcamps)
            .andRoute(DELETE("/capacity/bootcamp/{id}"), bootcampHandler::deleteBootcamp)
            .andRoute(POST("/capacity/bootcamp/enroll"), enrollmentHandler::enrollUser)
            .andRoute(DELETE("/capacity/bootcamp/{bootcampId}/unenroll"), enrollmentHandler::unenrollUser)
            .andRoute(GET("/capacity/bootcamp/my-bootcamps"), enrollmentHandler::getUserBootcamps);
    }

}
