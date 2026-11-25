package com.myfinance.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");
        return (uri != null && uri.startsWith("/api/")) ||
               (accept != null && accept.contains("application/json"));
    }

    @ExceptionHandler(BusinessValidationException.class)
    public Object handleBusinessValidation(BusinessValidationException ex,
                                           HttpServletRequest request,
                                           RedirectAttributes redirectAttributes) {
        log.warn("Validation failure at {}: {} -> {}", request.getRequestURI(), ex.getMessage(), ex.getFieldErrors());
        if (isApiRequest(request)) {
            return ResponseEntity.badRequest().body(new ApiErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    ex.getErrorCode(),
                    ex.getMessage(),
                    request.getRequestURI(),
                    ex.getFieldErrors()
            ));
        }
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        redirectAttributes.addFlashAttribute("fieldErrors", ex.getFieldErrors());
        return "redirect:/dashboard";
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Object handleBindingAndValidation(BindException ex,
                                             HttpServletRequest request,
                                             RedirectAttributes redirectAttributes) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError err : ex.getBindingResult().getFieldErrors()) {
            String msg = err.getDefaultMessage();
            if (err.isBindingFailure()) {
                msg = "Invalid format for field '" + err.getField() + "'.";
            }
            errors.putIfAbsent(err.getField(), msg);
        }
        log.warn("Form/Payload binding failure at {}: {}", request.getRequestURI(), errors);

        if (isApiRequest(request)) {
            return ResponseEntity.badRequest().body(new ApiErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    ErrorCode.VALIDATION_FAILED,
                    "Request payload validation failed",
                    request.getRequestURI(),
                    errors
            ));
        }
        redirectAttributes.addFlashAttribute("errorMessage", "Please check your input values and try again.");
        redirectAttributes.addFlashAttribute("fieldErrors", errors);
        return "redirect:/dashboard";
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Object handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                     HttpServletRequest request,
                                     RedirectAttributes redirectAttributes) {
        String msg = "Invalid parameter value for '" + ex.getName() + "'.";
        if (isApiRequest(request)) {
            return ResponseEntity.badRequest().body(new ApiErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    ErrorCode.VALIDATION_FAILED,
                    msg,
                    request.getRequestURI(),
                    Map.of(ex.getName(), msg)
            ));
        }
        redirectAttributes.addFlashAttribute("errorMessage", msg);
        return "redirect:/dashboard";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Object handleDataIntegrity(DataIntegrityViolationException ex,
                                      HttpServletRequest request,
                                      RedirectAttributes redirectAttributes) {
        log.error("Database constraint error at {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        String msg = "Unable to save record due to invalid or duplicate data.";
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErrorResponse(
                    HttpStatus.CONFLICT.value(),
                    ErrorCode.DUPLICATE_RESOURCE,
                    msg,
                    request.getRequestURI(),
                    null
            ));
        }
        redirectAttributes.addFlashAttribute("errorMessage", msg);
        return "redirect:/dashboard";
    }

    @ExceptionHandler(FileStorageException.class)
    public Object handleFileStorage(FileStorageException ex,
                                    HttpServletRequest request,
                                    RedirectAttributes redirectAttributes) {
        log.warn("File upload rejected at {}: {}", request.getRequestURI(), ex.getMessage());
        if (isApiRequest(request)) {
            return ResponseEntity.badRequest().body(new ApiErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    ErrorCode.INVALID_FILE,
                    ex.getMessage(),
                    request.getRequestURI(),
                    Map.of("attachment", ex.getMessage())
            ));
        }
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/dashboard";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Object handleMaxSize(MaxUploadSizeExceededException ex,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        String msg = "Uploaded image exceeds the maximum allowed size of 5MB.";
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(new ApiErrorResponse(
                    HttpStatus.PAYLOAD_TOO_LARGE.value(),
                    ErrorCode.INVALID_FILE,
                    msg,
                    request.getRequestURI(),
                    Map.of("attachment", msg)
            ));
        }
        redirectAttributes.addFlashAttribute("errorMessage", msg);
        return "redirect:/dashboard";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleMissingStaticResource(NoResourceFoundException ex) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleNotFound(ResourceNotFoundException ex,
                                  HttpServletRequest request,
                                  RedirectAttributes redirectAttributes) {
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiErrorResponse(
                    HttpStatus.NOT_FOUND.value(),
                    ErrorCode.RESOURCE_NOT_FOUND,
                    ex.getMessage(),
                    request.getRequestURI(),
                    null
            ));
        }
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/dashboard";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiErrorResponse(
                    HttpStatus.FORBIDDEN.value(),
                    ErrorCode.ACCESS_DENIED,
                    "Access denied to this resource.",
                    request.getRequestURI(),
                    null
            ));
        }
        return "redirect:/login?denied";
    }

    @ExceptionHandler(Exception.class)
    public Object handleGenericException(Exception ex,
                                         HttpServletRequest request,
                                         RedirectAttributes redirectAttributes) {
        log.error("Unhandled exception at {} [{}]: {}", request.getRequestURI(), ex.getClass().getSimpleName(), ex.getMessage(), ex);
        String detail = ex.getMessage() != null ? ex.getMessage() : "Please verify your database migrations and try again.";
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    ErrorCode.INTERNAL_ERROR,
                    "Internal error: " + detail,
                    request.getRequestURI(),
                    null
            ));
        }
        redirectAttributes.addFlashAttribute("errorMessage", "Error: " + detail);
        return "redirect:/dashboard";
    }
}
