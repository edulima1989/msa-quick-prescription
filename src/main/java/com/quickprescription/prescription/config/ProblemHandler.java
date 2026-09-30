package com.quickprescription.prescription.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestControllerAdvice
public class ProblemHandler {
  @ExceptionHandler(ApiException.class)
  ResponseEntity<ProblemDetail> api(ApiException e, HttpServletRequest r) { return problem(e.getStatus(),e.getMessage(),r); }
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException e, HttpServletRequest r) {
    ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), "La solicitud no es válida");
    p.setType(URI.create("https://httpstatuses.com/400")); p.setProperty("errores",
        e.getBindingResult().getFieldErrors().stream().map(x -> new ErrorItem(x.getField(),x.getDefaultMessage())).toList());
    p.setInstance(URI.create(r.getRequestURI())); return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_PROBLEM_JSON).body(p);
  }
  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<ProblemDetail> constraint(ConstraintViolationException e, HttpServletRequest r) { return problem(400,e.getMessage(),r); }
  private ResponseEntity<ProblemDetail> problem(int status, String detail, HttpServletRequest r) {
    ProblemDetail p=ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status),detail); p.setInstance(URI.create(r.getRequestURI()));
    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(p);
  }
  record ErrorItem(String campo, String mensaje) {}
}
