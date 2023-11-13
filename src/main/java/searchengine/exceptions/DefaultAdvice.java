package searchengine.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import searchengine.dto.response.Response;

@ControllerAdvice
@Slf4j
public class DefaultAdvice {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response> handleException(Exception e) {
        log.error(e.getMessage());
        Response response = new Response(false, "Указанная страница не найдена");
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}
