package me.gregorsomething.database.processor;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.lang.model.element.Element;

@RequiredArgsConstructor
@Getter
public class ProcessingValidationException extends RuntimeException {
    private final String message;
    private final transient Element element;
}
