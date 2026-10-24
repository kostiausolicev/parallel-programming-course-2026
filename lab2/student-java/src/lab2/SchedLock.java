package lab2;

/* Этап 2: лок, о котором знает планировщик. Поток, ждущий занятый лок, не попадает в enabled;
 * обычный synchronized / ReentrantLock здесь нельзя — поток уснёт с ходом в руках и всё зависнет.
 * Лог: "lock <имя>", "unlock <имя>". Планировщик должен знать все локи, чтобы печатать held={a:A, b:B}. */
public final class SchedLock {
    final String name;
    final Scheduler scheduler;
    private volatile boolean isLocked;
    private volatile Thread holder;

    /** Создаётся в setup() тестовой программы; зарегистрируйте лок в планировщике s. */
    public SchedLock(Scheduler s, String name) {
        this.name = name;
        this.scheduler = s;
        this.isLocked = false;
    }

    /** Пауза; если лок занят — ждать, не участвуя в выборе; затем стать владельцем. */
    public void lock() {
        scheduler.switchTask(false);
        Task currentTask = Task.CURRENT_TASK.get();
        // если заблокировано, то добавляем задачу в очередь и стопим ее до вызова resume
        if (isLocked) {
            scheduler.addWaitingTask(this.name, currentTask);
            scheduler.switchTask(true);
        }
        scheduler.switchTask(false);
        holder = currentTask.thread;
        isLocked = true;
    }

    /** Пауза; освободить лок, ждавшие потоки снова могут быть выбраны. */
    public void unlock() {
        scheduler.switchTask(false);
        if (Thread.currentThread().equals(holder)) {
            isLocked = false;
            scheduler.releaseWaitingTask(this.name);
            scheduler.switchTask(false);
        }
    }
}
