package lab2;

/* Этап 1: общая переменная. Перед каждым get/set — точка переключения (пауза планировщика),
 * после — строка в лог: чтение "<имя> -> <значение>", запись "<имя> = <значение>".
 * getAcquire/setRelease для планировщика ведут себя так же, как get/set. */
public final class Shared<T> {
    private final String name;
    private T value;

    public Shared(String name, T value) { this.name = name; this.value = value; }

    /** Чтение с паузой перед ним. */
    public T get() {
        Task currentTask = Task.CURRENT_TASK.get();
        Scheduler scheduler = Task.CURRENT_SCHEDULER.get();
        scheduler.switchTask(false);
        T result = value;
        if (scheduler.logging) {
            String logStr = String.format("%s  %s -> %s", currentTask.getLiteralId(), name, result);
            scheduler.log.add(logStr);
        }
        scheduler.operationDone();
        return result;
    }

    /** Запись с паузой перед ней. */
    public void set(T v) {
        Task currentTask = Task.CURRENT_TASK.get();
        Scheduler scheduler = Task.CURRENT_SCHEDULER.get();
        scheduler.switchTask(false);
        value = v;
        if (scheduler.logging) {
            String logStr = String.format("%s  %s = %s", currentTask.getLiteralId(), name, v);
            scheduler.log.add(logStr);
        }
        scheduler.operationDone();
    }

    /** То же, что get(): планировщик не моделирует порядки памяти. */
    public T getAcquire() {
        return get();
    }

    /** То же, что set(v). */
    public void setRelease(T v) {
        set(v);
    }

    /** Чтение без паузы и без лога: для проверок после прогона и условий await. */
    public T peek() { return value; }
}
