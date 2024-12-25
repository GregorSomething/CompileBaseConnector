package me.gregorsomething.database.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;

import lombok.SneakyThrows;
import me.gregorsomething.database.Database;
import me.gregorsomething.database.Transaction;
import me.gregorsomething.database.Transactional;
import me.gregorsomething.database.annotations.Query;
import me.gregorsomething.database.annotations.Repository;
import me.gregorsomething.database.annotations.Statement;
import me.gregorsomething.database.processor.helpers.SqlHelper;
import me.gregorsomething.database.processor.types.TypeDefResolver;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes(
        "me.gregorsomething.database.annotations.Repository")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class RepositoryProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        for (TypeElement annotation : annotations) {
            for (Element element : roundEnvironment.getElementsAnnotatedWith(annotation)) {
                try {
                    if (!this.processRepository(element))
                        return false;
                } catch (IOException e) {
                    return error(e.getMessage(), element);
                }
            }
        }
        return true;
    }

    private boolean processRepository(Element element) throws IOException {
        if (!element.getKind().isInterface())
            return error("Repository annotation can be used only on interface!", element);
        Repository repoAnnotation = element.getAnnotation(Repository.class);

        TypeSpec.Builder builder = TypeSpec.classBuilder(element.getSimpleName().toString() + "Imp")
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(element.asType());
        TypeDefResolver extraTypes = new TypeDefResolver(this);
        extraTypes.setup(repoAnnotation);

        this.createConstructor(builder, repoAnnotation);

        try {

            final List<MethodSpec> accessMethods = this.createMethods(element, extraTypes);
            builder.addMethods(accessMethods);

            this.addTransactionSupportIfNeeded(element, builder);

        } catch (ProcessingValidationException e) {
            return error(e.getMessage(), e.getElement());
        }

        JavaFile javaFile = JavaFile.builder(element.getEnclosingElement().toString(), builder.build())
                .build();
        javaFile.writeTo(this.processingEnv.getFiler());
        return true;
    }

    private List<MethodSpec> createMethods(Element element, TypeDefResolver extraTypes) {
        SqlHelper helper = new SqlHelper(this);
        StatementSubProcessor subProcessorStatement = new StatementSubProcessor(this);
        QuerySubProcessor subProcessorQuery = new QuerySubProcessor(this, helper);

        final List<ExecutableElement> statementMethods = this.getMethodsWithAnnotation(element, Statement.class);
        final List<ExecutableElement> queryMethods = this.getMethodsWithAnnotation(element, Query.class);
        final List<MethodSpec> specs = statementMethods.stream()
                .map(subProcessorStatement::createStatementMethod)
                .collect(Collectors.toList());
        specs.addAll(queryMethods.stream()
                .map(subProcessorQuery::createQueryMethod).toList());
        return specs;
    }

    private void createConstructor(TypeSpec.Builder builder, Repository repoAnnotation) {
        builder.addField(Database.class, "database", Modifier.PRIVATE, Modifier.FINAL);

        MethodSpec.Builder code = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Database.class, "database")
                .addStatement("this.database = database")
                .addAnnotation(SneakyThrows.class);

        for (String statement : repoAnnotation.value()) {
            code.addStatement("this.database.execute($S)", statement);
        }

        builder.addMethod(code.build());
    }

    private void addTransactionSupportIfNeeded(Element element, TypeSpec.Builder builder) {
        if (element instanceof TypeElement typeElement) {
            TypeMirror transactionType = typeElement.getInterfaces().stream().filter(i -> this.isBaseTypeOf(i, Transactional.class)).findFirst().orElse(null);
            if (transactionType == null)
                return;
            this.implementTransactional(typeElement, transactionType, builder);
        } else
            throw new ProcessingValidationException("Expected TypeElement, got something else.", element);
    }

    private void implementTransactional(TypeElement repository, TypeMirror transactionType, TypeSpec.Builder builder) {
        builder.addSuperinterface(transactionType)
                .addMethod(this.implementGetNewTransaction())
                .addMethod(this.implementTransactionalRepoCreate(repository));
    }

    private MethodSpec implementGetNewTransaction() {
        return MethodSpec.methodBuilder("getNewTransaction")
                .returns(Transaction.class)
                .addAnnotation(Override.class)
                .addAnnotation(SneakyThrows.class)
                .addModifiers(Modifier.PUBLIC)
                .addStatement("return new $T(this.database.getConnection())", Transaction.class).build();
    }

    private MethodSpec implementTransactionalRepoCreate(TypeElement repository) {
        return MethodSpec.methodBuilder("asTransactional")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(TypeName.get(repository.asType()))
                .addParameter(Transaction.class, "transaction")
                .addStatement("return new $TImp(transaction.getTransactionalDatabase())", TypeName.get(repository.asType()))
                .build();
    }

    public List<ExecutableElement> getMethodsWithAnnotation(Element element, Class<? extends Annotation> annotationClass) {
        return element.getEnclosedElements().stream()
                .filter(e -> e.getKind().equals(ElementKind.METHOD))
                .filter(e -> e.getAnnotation(annotationClass) != null)
                .map(e -> (ExecutableElement) e)
                .toList();
    }

    public boolean error(String message, @Nullable Element element) {
        this.processingEnv.getMessager().printError(message, element);
        return false;
    }

    public void warning(String message, @Nullable Element element) {
        this.processingEnv.getMessager().printWarning(message, element);
    }

    public void message(String message, @Nullable Element element) {
        this.processingEnv.getMessager().printNote(message);
    }

    public boolean isOfType(TypeMirror type1, Class<?> type2) {
        TypeMirror type = this.processingEnv.getElementUtils().getTypeElement(type2.getTypeName()).asType();
        return this.processingEnv.getTypeUtils().isAssignable(type1, type);
    }

    public boolean isBaseTypeOf(TypeMirror type1, Class<?> type2) {
        TypeMirror type = this.processingEnv.getElementUtils().getTypeElement(type1.toString().replaceFirst("<.*>", "")).asType();
        return this.isOfType(type, type2);
    }

    public Types getTypeUtils() {
        return processingEnv.getTypeUtils();
    }

    public Elements getElementUtils() {
        return processingEnv.getElementUtils();
    }
}
