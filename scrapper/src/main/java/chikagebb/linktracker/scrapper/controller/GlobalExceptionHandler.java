package chikagebb.linktracker.scrapper.controller;

import chikagebb.linktracker.scrapper.model.ApiErrorResponse;
import java.util.Arrays;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException e) {
        var error = new ApiErrorResponse()
                .description(e.getReason())
                .code(String.valueOf(e.getStatusCode().value()))
                .exceptionName(e.getClass().getSimpleName())
                .exceptionMessage(e.getMessage())
                .stacktrace(Arrays.stream(e.getStackTrace())
                        .map(StackTraceElement::toString)
                        .toList());

        return ResponseEntity.status(e.getStatusCode()).body(error);
    }
}
