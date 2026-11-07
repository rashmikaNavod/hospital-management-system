package org.example.authservice.handler;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.example.authservice.exception.DuplicateUserException;
import org.example.authservice.exception.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateUser(
            DuplicateUserException ex, HttpServletRequest request){
        log.warn("Duplicate user: {}", ex.getMessage());

        return ProblemResponseFactory.create(
                HttpStatus.CONFLICT,
                "Duplicate User",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUserNotFound(
            UserNotFoundException ex, HttpServletRequest request){
        log.warn("User not found: {}", ex.getMessage());

        return ProblemResponseFactory.create(
                HttpStatus.NOT_FOUND,
                "User Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );
    }


}
