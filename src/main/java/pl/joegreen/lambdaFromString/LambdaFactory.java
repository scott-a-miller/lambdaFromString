package pl.joegreen.lambdaFromString;

import pl.joegreen.lambdaFromString.classFactory.ClassCompilationException;
import pl.joegreen.lambdaFromString.classFactory.ClassFactory;

import javax.tools.JavaCompiler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

public class LambdaFactory {

    protected static Optional<JavaCompiler> DEFAULT_COMPILER = JavaCompilerProvider.findDefaultJavaCompiler();

    /**
     * Returns a LambdaFactory instance with default configuration.
     * @throws JavaCompilerNotFoundException if the library cannot find any java compiler
     */
    public static LambdaFactory get() {
        return get(LambdaFactoryConfiguration.get());
    }

    /**
     * Returns a LambdaFactory instance with the given configuration.
     * @throws JavaCompilerNotFoundException if the library cannot find any java compiler and it's not provided
     * in the configuration
     */
    public static LambdaFactory get(LambdaFactoryConfiguration configuration) {
        JavaCompiler compiler = getConfiguredOrDefaultCompiler(configuration);
        return new LambdaFactory(
                configuration.getDefaultHelperClassSourceProvider(),
                configuration.getClassFactory(),
                compiler,
                configuration.getImports(),
                configuration.getStaticImports());
    }

    private static JavaCompiler getConfiguredOrDefaultCompiler(LambdaFactoryConfiguration configuration) {
        return configuration.getJavaCompiler()
                .orElse(DEFAULT_COMPILER
                        .orElseThrow(JavaCompilerNotFoundException::new));
    }

    private final HelperClassSourceProvider helperProvider;
    private final ClassFactory classFactory;
    private final JavaCompiler javaCompiler;
    private final List<String> imports;
    private final List<String> staticImports;

    private LambdaFactory(HelperClassSourceProvider helperProvider, ClassFactory classFactory,
                          JavaCompiler javaCompiler, List<String> imports, List<String> staticImports) {
        this.helperProvider = helperProvider;
        this.classFactory = classFactory;
        this.javaCompiler = javaCompiler;
        this.imports = imports;
        this.staticImports = staticImports;
    }

    /**
     * Creates lambda from the given code.
     *
     * @param code          source of the lambda as you would write it in Java expression  {TYPE} lambda = ({CODE});
     * @param typeReference a subclass of TypeReference class with the generic argument representing the type of the lambda
     *                      , for example  <br> {@code new TypeReference<Function<Integer,Integer>>(){}; }
     * @param <T>           type of the lambda you want to get
     * @throws LambdaCreationException when anything goes wrong (no other exceptions are thrown including runtimes),
     *                                 if the exception was caused by compilation failure it will contain a CompilationDetails instance describing them
     */
    public <T> T createLambda(String code, TypeReference<T> typeReference) throws LambdaCreationException {
        String helperClassSource = helperProvider.getHelperClassSource(typeReference.toString(), code, imports, staticImports);
        try {
            Class<?> helperClass = classFactory.createClass(helperProvider.getHelperClassName(), helperClassSource, javaCompiler);
            Method lambdaReturningMethod = helperClass.getMethod(helperProvider.getLambdaReturningMethodName());
            @SuppressWarnings("unchecked")
            // the whole point of the class template and runtime compilation is to make this cast work well :-)
                    T lambda = (T) lambdaReturningMethod.invoke(null);
            return lambda;
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new LambdaCreationException(e);
        } catch (ClassCompilationException classCompilationException) {
            // knows type of the cause so it get CompilationDetails
            throw new LambdaCreationException(classCompilationException);
        }
    }

    /**
     * Convenience wrapper for {@link #createLambda(String, TypeReference)}
     * which throws unchecked exception instead of checked one.
     *
     * @see #createLambda(String, TypeReference)
     */
    public <T> T createLambdaUnchecked(String code, TypeReference<T> type) {
        try {
            return createLambda(code, type);
        } catch (LambdaCreationException e) {
            throw new LambdaCreationRuntimeException(e);
        }
    }

  /**
   * Creates a lambda from the given code
   * @param code source of the lambda as you would write it in Java expression  {TYPE} lambda = ({CODE});
   * @param typeString the type string (as would be created by a TypeReference class)
   *                   , for example {@code "Function<Integer,Integer>>"}
   * @return the lambda object
   * @throws LambdaCreationException
   */
    public Object createLambda(String code, String typeString) throws LambdaCreationException {
        String helperClassSource = helperProvider.getHelperClassSource(typeString, code, imports, staticImports);
        try {
            Class<?> helperClass = classFactory.createClass(helperProvider.getHelperClassName(), helperClassSource, javaCompiler);
            Method lambdaReturningMethod = helperClass.getMethod(helperProvider.getLambdaReturningMethodName());
            Object lambda = lambdaReturningMethod.invoke(null);
            return lambda;
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new LambdaCreationException(e);
        } catch (ClassCompilationException classCompilationException) {
            // knows type of the cause so it get CompilationDetails
            throw new LambdaCreationException(classCompilationException);
        }
    }

    /**
     * Convenience wrapper for {@link #createLambda(String, String)}
     * which throws unchecked exception instead of checked one.
     *
     * @see #createLambda(String, String)
     */
    public Object createLambdaUnchecked(String code, String typeString) {
        try {
            return createLambda(code, typeString);
        } catch (LambdaCreationException e) {
            throw new LambdaCreationRuntimeException(e);
        }
    }
}
