package org.example.authservice.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import java.net.URI;
import java.time.Instant;

//final class — මේ class එක extend කරන්න බැහැ.
public final class ProblemResponseFactory {

    //මේක නොලිව්වොත් Java එකෙන් default constructor එකක් ලබාදෙනවා.
    // ඔයාගේ class එක public නිසා වෙන තැනක call krnn puluwn
    private ProblemResponseFactory(){}

    public static ResponseEntity<ProblemDetail> create(
            HttpStatus status,
            String title,
            String detail,
            String instance){

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setInstance(URI.create(instance));
        problem.setProperty("timestamp", Instant.now().toString());

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);

    }

}
