package com.reposteria.productos.exception;

import com.reposteria.productos.dto.response.ErrorResponse;
import com.reposteria.productos.dto.response.ErrorResponse.CampoError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

/**
 * Manejo centralizado de errores (sección 8.8, RNF-19, RNF-P01).
 * Convierte cada excepción en una respuesta con el mismo formato y un mensaje comprensible.
 * Nunca devuelve trazas, SQL, rutas internas ni tokens; los detalles técnicos solo van al log.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------- Errores del negocio ----------

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException e, HttpServletRequest peticion) {
        return responder(HttpStatus.NOT_FOUND, e.getMessage(), null, null, peticion);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse> reglaNegocio(ReglaNegocioException e, HttpServletRequest peticion) {
        return responder(HttpStatus.CONFLICT, e.getMessage(), e.getCodigo(), null, peticion);
    }

    @ExceptionHandler(DatosInvalidosException.class)
    public ResponseEntity<ErrorResponse> datosInvalidos(DatosInvalidosException e, HttpServletRequest peticion) {
        return responder(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), null,
                List.of(new CampoError(e.getCampo(), e.getMessage())), peticion);
    }

    @ExceptionHandler(ArchivoDemasiadoGrandeException.class)
    public ResponseEntity<ErrorResponse> archivoGrande(ArchivoDemasiadoGrandeException e, HttpServletRequest peticion) {
        return responder(HttpStatus.PAYLOAD_TOO_LARGE, e.getMessage(), null, null, peticion);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> subidaGrande(MaxUploadSizeExceededException e, HttpServletRequest peticion) {
        return responder(HttpStatus.PAYLOAD_TOO_LARGE, "La imagen pesa más de 5 MB. Usa una más liviana.", null, null, peticion);
    }

    @ExceptionHandler(AlmacenamientoException.class)
    public ResponseEntity<ErrorResponse> almacenamiento(AlmacenamientoException e, HttpServletRequest peticion) {
        log.error("Falla al guardar o borrar una imagen", e);
        return responder(HttpStatus.BAD_GATEWAY, e.getMessage(), null, null, peticion);
    }

    // ---------- Errores de formato de la petición (400) ----------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> cuerpoInvalido(MethodArgumentNotValidException e, HttpServletRequest peticion) {
        List<CampoError> campos = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new CampoError(error.getField(), mensajeDe(error)))
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "Revisa los campos marcados.", null, campos, peticion);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> parametrosInvalidos(HandlerMethodValidationException e,
                                                             HttpServletRequest peticion) {
        List<CampoError> campos = e.getAllErrors().stream()
                .map(error -> new CampoError("consulta", error.getDefaultMessage()))
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "Revisa los parámetros de la consulta.", null, campos, peticion);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> cuerpoIlegible(HttpMessageNotReadableException e, HttpServletRequest peticion) {
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido.", null, null, peticion);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> tipoIncorrecto(MethodArgumentTypeMismatchException e, HttpServletRequest peticion) {
        return responder(HttpStatus.BAD_REQUEST, "El valor de «" + e.getName() + "» no tiene el formato esperado.",
                null, null, peticion);
    }

    @ExceptionHandler({MissingServletRequestPartException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> faltaDato(Exception e, HttpServletRequest peticion) {
        return responder(HttpStatus.BAD_REQUEST, "Falta un dato obligatorio en la petición.", null, null, peticion);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> tipoNoSoportado(HttpMediaTypeNotSupportedException e,
                                                         HttpServletRequest peticion) {
        return responder(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "El tipo de contenido enviado no es compatible.",
                null, null, peticion);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> metodoNoPermitido(HttpRequestMethodNotSupportedException e,
                                                           HttpServletRequest peticion) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, "Esta dirección no admite el método " + e.getMethod() + ".",
                null, null, peticion);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> rutaInexistente(NoResourceFoundException e, HttpServletRequest peticion) {
        return responder(HttpStatus.NOT_FOUND, "Esa dirección no existe.", null, null, peticion);
    }

    // ---------- Seguridad ----------

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> credenciales(AuthenticationException e, HttpServletRequest peticion) {
        return responder(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos.", null, null, peticion);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> sinPermisos(AccessDeniedException e, HttpServletRequest peticion) {
        return responder(HttpStatus.FORBIDDEN, "Tu usuario no tiene permiso para esta operación.", null, null, peticion);
    }

    // ---------- Base de datos ----------

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> concurrencia(OptimisticLockingFailureException e, HttpServletRequest peticion) {
        return responder(HttpStatus.CONFLICT,
                "Otra persona modificó este producto al mismo tiempo. Recarga y vuelve a intentarlo.",
                "MODIFICACION_CONCURRENTE", null, peticion);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integridad(DataIntegrityViolationException e, HttpServletRequest peticion) {
        log.warn("Violación de integridad de datos: {}", e.getMostSpecificCause().getClass().getSimpleName());
        return responder(HttpStatus.CONFLICT, "Los datos no cumplen las reglas de integridad del catálogo.",
                "INTEGRIDAD", null, peticion);
    }

    @ExceptionHandler({DataAccessResourceFailureException.class, CannotCreateTransactionException.class})
    public ResponseEntity<ErrorResponse> baseDeDatosCaida(Exception e, HttpServletRequest peticion) {
        log.error("No hay conexión con la base de datos", e);
        return responder(HttpStatus.SERVICE_UNAVAILABLE,
                "El catálogo no está disponible en este momento. Inténtalo de nuevo en unos minutos.", null, null, peticion);
    }

    // ---------- Cualquier otro error ----------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> inesperado(Exception e, HttpServletRequest peticion) {
        log.error("Error inesperado en {}", peticion.getRequestURI(), e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado. Inténtalo de nuevo.", null, null, peticion);
    }

    private static String mensajeDe(FieldError error) {
        return error.getDefaultMessage() != null ? error.getDefaultMessage() : "Valor no válido.";
    }

    private static ResponseEntity<ErrorResponse> responder(HttpStatus estado, String mensaje, String codigo,
                                                           List<CampoError> campos, HttpServletRequest peticion) {
        ErrorResponse cuerpo = new ErrorResponse(estado.value(), estado.getReasonPhrase(), mensaje, codigo, campos,
                peticion.getRequestURI(), Instant.now());
        return ResponseEntity.status(estado).body(cuerpo);
    }
}
