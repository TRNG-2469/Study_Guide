package com.example.books.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * STARTER CODE — provided as a scaffold.
 *
 * TODO: Complete this class.
 *  1. Add the correct class-level annotation for global REST exception handling.
 *  2. Add @ExceptionHandler methods for:
 *       - BookNotFoundException       → 404 Not Found
 *       - DuplicateIsbnException      → 409 Conflict
 *       - MethodArgumentNotValidException → 400 Bad Request
 *       - Exception (catch-all)       → 500 Internal Server Error
 *  3. Each method must return ResponseEntity<ErrorResponse>.
 *  4. The catch-all must NOT return ex.getMessage() — use a generic safe message.
 */
// TODO: Add class-level annotation
public class GlobalExceptionHandler {

    // TODO: Handle BookNotFoundException
    @ExceptionHandler(BookNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(BookNotFoundException ex) {
        // TODO: implement
        return null;
    }

    // TODO: Handle DuplicateIsbnException

    // TODO: Handle MethodArgumentNotValidException
    //       Hint: use ex.getBindingResult().getFieldErrors() to get field-level messages

    // TODO: Handle Exception (catch-all)
    //       IMPORTANT: Do NOT return ex.getMessage() to the client
}
