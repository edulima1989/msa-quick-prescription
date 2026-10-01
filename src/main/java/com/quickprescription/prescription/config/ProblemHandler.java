package com.quickprescription.prescription.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Todos los errores salen en application/problem+json con title y status.
 * Los detail son textos fijos: nunca se usa el mensaje de excepciones de Spring o Jackson,
 * porque incluye valores del cuerpo (paciente, edad, fecha).
 */
@RestControllerAdvice
public class ProblemHandler extends ResponseEntityExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(ProblemHandler.class);
  private static final Map<Integer, String> TITLES = Map.of(
      400, "Solicitud inválida", 404, "Recurso no encontrado", 405, "Método no permitido",
      406, "No aceptable", 415, "Tipo de contenido no soportado", 422, "Entidad no procesable",
      500, "Error interno");
  static final String INTERNAL_DETAIL = "Ocurrió un error inesperado. Intente nuevamente más tarde.";

  @ExceptionHandler(ApiException.class)
  ResponseEntity<Object> api(ApiException e, WebRequest r) {
    return respond(e, HttpStatusCode.valueOf(e.getStatus()), e.getMessage(), null, r);
  }
  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<Object> constraint(ConstraintViolationException e, WebRequest r) {
    List<ErrorItem> errores = e.getConstraintViolations().stream()
        .map(v -> new ErrorItem(lastNode(v), v.getMessage())).toList();
    return respond(e, HttpStatus.BAD_REQUEST, "La solicitud no es válida", errores, r);
  }
  @ExceptionHandler(Exception.class)
  ResponseEntity<Object> unexpected(Exception e, WebRequest r) {
    // Solo clase y ubicación: el mensaje o las causas pueden contener datos de la receta.
    StackTraceElement[] st = e.getStackTrace();
    log.error("Error inesperado en {} {}: {} en {}", method(r), path(r), e.getClass().getName(),
        st.length > 0 ? st[0] : "?");
    return respond(e, HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_DETAIL, null, r);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException e,
      HttpHeaders h, HttpStatusCode s, WebRequest r) {
    String detail = e.getCause() == null ? "El cuerpo de la solicitud es obligatorio."
        : "El cuerpo no es un JSON válido o algún campo tiene un tipo o formato inválido.";
    return respond(e, s, detail, null, r);
  }
  @Override
  protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException e, HttpHeaders h,
      HttpStatusCode s, WebRequest r) {
    String name = e.getPropertyName() != null ? e.getPropertyName() : "desconocido";
    return respond(e, s, "El parámetro '" + name + "' tiene un tipo inválido.", null, r);
  }
  @Override
  protected ResponseEntity<Object> handleMissingServletRequestParameter(
      MissingServletRequestParameterException e, HttpHeaders h, HttpStatusCode s, WebRequest r) {
    return respond(e, s, "Falta el parámetro '" + e.getParameterName() + "'.", null, r);
  }
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
      HttpHeaders h, HttpStatusCode s, WebRequest r) {
    List<ErrorItem> errores = e.getBindingResult().getFieldErrors().stream()
        .map(x -> new ErrorItem(x.getField(), x.getDefaultMessage())).toList();
    return respond(e, s, "La solicitud no es válida", errores, r);
  }
  @Override
  protected ResponseEntity<Object> handleHandlerMethodValidationException(
      HandlerMethodValidationException e, HttpHeaders h, HttpStatusCode s, WebRequest r) {
    List<ErrorItem> errores = e.getParameterValidationResults().stream()
        .flatMap(p -> p.getResolvableErrors().stream().map(x -> new ErrorItem(
            x instanceof FieldError f ? f.getField() : p.getMethodParameter().getParameterName(),
            x.getDefaultMessage()))).toList();
    return respond(e, HttpStatus.BAD_REQUEST, "La solicitud no es válida", errores, r);
  }
  @Override
  protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException e,
      HttpHeaders h, HttpStatusCode s, WebRequest r) {
    return respond(e, s, "El Content-Type debe ser " + types(e.getSupportedMediaTypes()) + ".", null, h, r);
  }
  @Override
  protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException e,
      HttpHeaders h, HttpStatusCode s, WebRequest r) {
    return respond(e, s, "El recurso solo puede entregarse como " + types(e.getSupportedMediaTypes()) + ".",
        null, r);
  }

  /** Punto de salida común (también para lo que resuelve la clase base): title, instance y Content-Type. */
  @Override
  protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers,
      HttpStatusCode status, WebRequest r) {
    ProblemDetail p = body instanceof ProblemDetail pd ? pd : ProblemDetail.forStatus(status);
    p.setTitle(TITLES.getOrDefault(status.value(), p.getTitle()));
    p.setInstance(URI.create(path(r)));
    HttpHeaders out = new HttpHeaders();
    out.putAll(headers);
    out.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
    return new ResponseEntity<>(p, out, status);
  }

  private ResponseEntity<Object> respond(Exception e, HttpStatusCode status, String detail,
      List<ErrorItem> errores, WebRequest r) {
    return respond(e, status, detail, errores, new HttpHeaders(), r);
  }
  private ResponseEntity<Object> respond(Exception e, HttpStatusCode status, String detail,
      List<ErrorItem> errores, HttpHeaders headers, WebRequest r) {
    ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detail);
    if (errores != null) {
      p.setType(URI.create("https://httpstatuses.com/" + status.value()));
      p.setProperty("errores", errores);
    }
    return handleExceptionInternal(e, p, headers, status, r);
  }
  private static String lastNode(ConstraintViolation<?> v) {
    String path = v.getPropertyPath().toString();
    return path.substring(path.lastIndexOf('.') + 1);
  }
  private static String types(List<MediaType> types) {
    return types.isEmpty() ? "un formato soportado"
        : String.join(" o ", types.stream().map(MediaType::toString).toList());
  }
  private static String path(WebRequest r) {
    return r instanceof ServletWebRequest s ? s.getRequest().getRequestURI() : "/";
  }
  private static String method(WebRequest r) {
    return r instanceof ServletWebRequest s ? s.getRequest().getMethod() : "?";
  }
  record ErrorItem(String campo, String mensaje) {}
}
