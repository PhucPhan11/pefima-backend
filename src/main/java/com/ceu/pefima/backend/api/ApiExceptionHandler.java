package com.ceu.pefima.backend.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ceu.pefima.backend.market.MarketPriceUnavailableException;
import com.ceu.pefima.backend.source.PefimaSourceException;
import com.ceu.pefima.backend.source.UnknownSnapshotException;

/**
 * Maps source and market-price failures to RFC 9457 problem responses. Their
 * exception messages never contain financial values, so they are safe to return.
 */
@RestControllerAdvice(assignableTypes = { PefimaDataController.class, MarketController.class })
class ApiExceptionHandler {

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ProblemDetail unknownSnapshot(UnknownSnapshotException ex) {
        return problem(HttpStatus.NOT_FOUND, "Unknown snapshot", ex.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    ProblemDetail sourceNotConfigured(SourceNotConfiguredException ex) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Data source not configured", ex.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    ProblemDetail sourceFailure(PefimaSourceException ex) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Data source error", ex.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    ProblemDetail marketPriceUnavailable(MarketPriceUnavailableException ex) {
        return problem(HttpStatus.BAD_GATEWAY, "Market price unavailable", ex.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }

}
