package kotlin.jvm.functions;

/** Заглушка для компиляции: в APK используется настоящий Function0 из Kotlin stdlib RuStore. */
public interface Function0<R> {
    R invoke();
}
