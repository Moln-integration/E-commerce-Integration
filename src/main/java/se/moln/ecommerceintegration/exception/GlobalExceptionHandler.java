package se.moln.ecommerceintegration.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<?> handleValidation(Exception ex) {
        BindingResult br = (ex instanceof MethodArgumentNotValidException manv)
                ? manv.getBindingResult()
                : ((BindException) ex).getBindingResult();

        List<Map<String, Object>> details = br.getFieldErrors().stream()
                .map(this::toProblem)
                .collect(Collectors.toList());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "validation_failed");
        body.put("message", "Validation failed");
        body.put("details", details);

        return ResponseEntity.badRequest().body(body);
    }

    private Map<String, Object> toProblem(FieldError fe) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("field", fe.getField());
        m.put("code", fe.getCode());
        m.put("message", fe.getDefaultMessage());
        // maska rejectedValue för lösenord
        m.put("rejectedValue", "password".equals(fe.getField()) ? null : fe.getRejectedValue());
        return m;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleNotReadable(HttpMessageNotReadableException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "bad_request");
        body.put("message", "Malformed JSON request");
        body.put("details", List.of());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("field", ex.getName());
        detail.put("code", "TypeMismatch");
        detail.put("message", "Expected type: " + (ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"));
        detail.put("rejectedValue", ex.getValue());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "validation_failed");
        body.put("message", "Validation failed");
        body.put("details", List.of(detail));
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraintViolation(ConstraintViolationException ex) {
        List<Map<String, Object>> details = ex.getConstraintViolations().stream()
                .map(this::toProblem)
                .collect(Collectors.toList());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "validation_failed");
        body.put("message", "Validation failed");
        body.put("details", details);
        return ResponseEntity.badRequest().body(body);
    }

    private Map<String, Object> toProblem(ConstraintViolation<?> v) {
        Map<String, Object> m = new LinkedHashMap<>();
        // propertyPath kan vara t.ex. "register.arg0.email" – ta hela strängen som "field"
        m.put("field", String.valueOf(v.getPropertyPath()));
        m.put("code", v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName());
        m.put("message", v.getMessage());
        m.put("rejectedValue", safeRejected(v.getInvalidValue())); // kan vara null
        return m;
    }

    private Object safeRejected(Object value) {
        if (value == null) return null;
        String s = value.toString();
        // maska om det liknar känsligt innehåll och/eller är väldigt långt
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatus(ResponseStatusException ex) {
        HttpStatusCode sc = ex.getStatusCode();
        String error = toErrorKey(sc);
        String message = ex.getReason() != null ? ex.getReason() : defaultReason(sc);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("message", message);
        body.put("details", List.of());
        return ResponseEntity.status(sc).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "forbidden");
        body.put("message", "Access is denied");
        body.put("details", List.of());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<?> handleNotFound(NoSuchElementException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "not_found");
        body.put("message", ex.getMessage() != null ? ex.getMessage() : "Resource not found");
        body.put("details", List.of());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalState(IllegalStateException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "conflict");
        body.put("message", ex.getMessage());
        body.put("details", List.of());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneric(Exception ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "internal_server_error");
        body.put("message", "An unexpected error occurred");
        body.put("details", List.of());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    /* --------- helpers --------- */
    private String toErrorKey(HttpStatusCode sc) {
        if (sc instanceof HttpStatus hs) {
            return hs.name().toLowerCase(); // e.g. UNAUTHORIZED -> "unauthorized"
        }
        return "error";
    }

    private String defaultReason(HttpStatusCode sc) {
        if (sc instanceof HttpStatus hs) return hs.getReasonPhrase();
        return "Error";
    }
}
