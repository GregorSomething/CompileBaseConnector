package me.gregorsomething.database.processor.types;

import lombok.RequiredArgsConstructor;
import me.gregorsomething.database.annotations.Repository;
import me.gregorsomething.database.processor.RepositoryProcessor;

import javax.lang.model.element.*;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public class TypeDefResolver {

    private final RepositoryProcessor processor;

    public void setup(Repository repo) {
        for (TypeMirror type : this.readClasses(repo)) {
            this.searchFromExtension(type);
        }
    }

    private void searchFromExtension(TypeMirror classType) {
        Element element = this.processor.getTypeUtils().asElement(classType);
        if (!element.getModifiers().contains(Modifier.PUBLIC)) {
            this.processor.error("Type extension for repository must be public!", element);
        }
        for (Element enclosedElement : element.getEnclosedElements()) {
            if (enclosedElement.getKind().equals(ElementKind.METHOD)) {
                // Last check checks that this is method.
                this.processMethod((ExecutableElement) enclosedElement, element);
            }
        }
    }

    private void processMethod(ExecutableElement element, Element parent) {
        if (!this.isCorrectSignatureForTypeDef(element, parent))
            return;
    }

    private boolean isCorrectSignatureForTypeDef(ExecutableElement element, Element parent) {
        // Check that element is public static
        if (parent.getKind().equals(ElementKind.INTERFACE)) {
            if (!element.getModifiers().contains(Modifier.DEFAULT)) {
                return false;
            }
        } else if (!element.getModifiers().contains(Modifier.PUBLIC)
                || !element.getModifiers().contains(Modifier.STATIC)
                || element.getModifiers().contains(Modifier.ABSTRACT)) {
            return false;
        }
        // Check parameter signature
        List<? extends VariableElement> params = element.getParameters();
        if (params.size() == 2
                && this.processor.isOfType(params.get(0).asType(), ResultSet.class)
                && params.get(1).asType().getKind().equals(TypeKind.INT)) {
            // Returns true if no exception or if only one and it is SQLException
            return element.getThrownTypes().isEmpty() ||
                    element.getThrownTypes().size() == 1
                            && this.processor.isBaseTypeOf(element.getThrownTypes().getFirst(), SQLException.class);
        }
        this.processor.warning("Invalid signature for type def to a repository", element);
        return false;
    }

    private List<? extends TypeMirror> readClasses(Repository repo) {
        try {
            // https://stackoverflow.com/questions/7687829/java-6-annotation-processing-getting-a-class-from-an-annotation
            // This should almost always cause exception,
            // as this might not be compiled,
            // but just in case this is handed here as well
            return Arrays.stream(repo.additionalTypes())
                    .map(c -> this.processor.getElementUtils()
                            .getTypeElement(c.getTypeName())
                            .asType())
                    .toList();
        } catch (MirroredTypesException ex) {
            return ex.getTypeMirrors();
        }
    }
}
