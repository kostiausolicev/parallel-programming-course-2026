package lab2;

/* Этап 2: лок, о котором знает планировщик. Поток, ждущий занятый лок, не попадает в enabled;
 * обычный synchronized / ReentrantLock здесь нельзя — поток уснёт с ходом в руках и всё зависнет.
 * Лог: "lock <имя>", "unlock <имя>". Планировщик должен знать все локи, чтобы печатать held={a:A, b:B}. */
public final class SchedLock {
    final String name;
    final Scheduler scheduler;
    private volatile boolean isLocked;
    private volatile Task holder;

    /** Создаётся в setup() тестовой программы; зарегистрируйте лок в планировщике s. */
    public SchedLock(Scheduler s, String name) {
        this.name = name;
        this.scheduler = s;
        this.isLocked = false;
        this.scheduler.registerLock(this);
    }

    /** Пауза; если лок занят — ждать, не участвуя в выборе; затем стать владельцем. */
    public void lock() {
        Task currentTask = Task.CURRENT_TASK.get();
        currentTask.waitingLock = this;
        scheduler.switchTask(false);
        // если заблокировано, то добавляем задачу в очередь и стопим ее до вызова resume
        while (isLocked) {
            scheduler.addWaitingTask(this.name, currentTask);
            scheduler.blockCurrentTask();
        }
        currentTask.waitingLock = null;
        holder = currentTask;
        isLocked = true;
        log("lock " + name);
        scheduler.operationDone();
    }

    /** Пауза; освободить лок, ждавшие потоки снова могут быть выбраны. */
    public void unlock() {
        scheduler.switchTask(false);
        if (Task.CURRENT_TASK.get().equals(holder)) {
            isLocked = false;
            holder = null;
            log("unlock " + name);
            scheduler.releaseWaitingTask(this.name);
            scheduler.operationDone();
        }
    }

    boolean isLocked() {
        return isLocked;
    }

    Task getHolder() {
        return holder;
    }

    private void log(String operation) {
        if (scheduler.logging) {
            scheduler.log.add(String.format("%s  %s", Task.CURRENT_TASK.get().getLiteralId(), operation));
        }
    }
}
