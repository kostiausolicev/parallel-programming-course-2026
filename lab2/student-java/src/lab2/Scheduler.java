package lab2;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.function.BooleanSupplier;

class Task {
    public Integer getId() {
        return id;
    }

    Integer id;
    TState state;
    Runnable body;
    Semaphore semaphore;
    Thread thread;

    static ThreadLocal<Scheduler> CURRENT_SCHEDULER = ThreadLocal.withInitial(() -> null);
    static ThreadLocal<Task> CURRENT_TASK = ThreadLocal.withInitial(() -> null);

    Task(Integer id, Runnable body) {
        this.id = id;
        this.state = TState.WAITING;
        this.body = body;
        this.semaphore = new Semaphore(0);
    }

    public void resumeTask() {
        this.semaphore.release();
    }

    public void setWaiting() {
        this.state = TState.WAITING;
        _wait();
    }

    public void setWaitingLock() {
        this.state = TState.BLOCKED;
        _wait();
    }

    private void _wait() {
        try {
            this.semaphore.acquire();
            this.state = TState.RUNNING;
        } catch (InterruptedException e) {
            this.state = TState.FINISHED;
            throw new RuntimeException(e);
        }
    }

    String getLiteralId() {
        return switch (id) {
            case 1 -> "A";
            case 2 -> "B";
            case 3 -> "C";
            case 4 -> "D";
            default -> throw new IllegalArgumentException();
        };
    }
}

enum TState {
    RUNNING,
    WAITING,
    BLOCKED,
    FINISHED
}

/* Этап 1: планировщик, который ведёт настоящие потоки строго по одному (передача хода через семафоры).
 * Этап 2: обнаружение deadlock в главном цикле и корректное завершение застрявших потоков.
 * Внутреннее устройство (свои классы, семафоры, вспомогательные методы для Shared и SchedLock) придумываете сами. */
public final class Scheduler {
    /** Если true, каждая операция с общими данными добавляет строку в log. Выставляет runner. */
    boolean logging;
    /** Лог прогона: строки "<поток>  <операция>", например "A  lock mu", "A  balance -> 100". */
    final List<String> log = new ArrayList<>();
    /** null, если deadlock не было; иначе "held={a:A, b:B}" (этап 2). */
    String deadlock;

    public Scheduler() {
    }

    Map<String, Queue<Task>> waiters = new HashMap<>();

    public void addWaitingTask(String lockName, Task task) {
        Queue<Task> w = Optional.ofNullable(waiters.get(lockName)).orElse(new LinkedList<>());
        w.add(task);
    }

    public void releaseWaitingTask(String lockName) {
        Optional.ofNullable(waiters.get(lockName))
                .flatMap(taskQueue -> Optional.ofNullable(taskQueue.poll()))
                .ifPresent(t -> t.state = TState.WAITING);
    }

    int counter = 1;
    List<Task> tasks = new ArrayList<>();

    Semaphore schedulerPermits = new Semaphore(0);
    List<String> trace = new ArrayList<>();
    int last = -1;
    boolean needSwitch = false;

    /** Зарегистрировать поток. Потоки получают буквы A, B, C… в порядке вызова spawn. */
    public void spawn(Runnable body) {
        Task task = new Task(counter, body);
        tasks.add(task);
        counter++;
    }

    /** Запустить потоки и вести их по одному: на каждой паузе спросить strategy.pick(enabled, last). */
    public void run(Strategy strategy) {
        for (Task task : tasks) {
            task.thread = new Thread(() -> {
                Task.CURRENT_SCHEDULER.set(this);
                Task.CURRENT_TASK.set(task);
                task.setWaiting();
                try {
                    task.body.run();
                } finally {
                    task.state = TState.FINISHED;
                    this.schedulerPermits.release();
                }
            });
            task.thread.start();
        }

        // todo оооооч сложно, надо проще делать
        while (tasks.stream()
                .filter(t -> t.state != TState.FINISHED)
                .limit(1L).findAny().isPresent()
        ) {
            this.needSwitch = false;
            // собираем enabled
            List<Integer> enabled = tasks.stream().filter(t -> t.state == TState.WAITING).map(Task::getId).toList();
            if (enabled.isEmpty()) {
                deadlock = waiters.entrySet().stream()
                        .flatMap(entry -> entry.getValue()
                                .stream()
                                .map(t -> String.format("%s: %s", t.getLiteralId(), entry.getKey()))
                        ).reduce((s1, s2) -> s1 + ", " + s2).orElse("");
            }
            int next = strategy.pick(enabled, last);
            last = next;

            // todo поиск оптимизировать. лишние аллокации
            Optional<Task> optional = tasks.stream().filter(t -> t.id == next).findAny();
            if (optional.isEmpty()) break;
            Task nextTask = optional.get();
            trace.add(nextTask.getLiteralId());
            nextTask.resumeTask(); // <- разрешаем задаче продолжить
            try {
                schedulerPermits.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println(this.traceString());
    }

    public void switchTask(boolean locked) {
        Task currentTask = Task.CURRENT_TASK.get();
        if (!this.needSwitch) {
            return;
        }
        this.trace.add(currentTask.getLiteralId());
        if (!locked) {
            currentTask.state = TState.WAITING;
        } else {
            currentTask.state = TState.BLOCKED;
        }
        this.schedulerPermits.release(); // <- будим себя
        // останавливаем задачу
        if (!locked) {
            currentTask.setWaiting(); // <- ставим задачу в ожидание
        } else {
            currentTask.setWaitingLock(); // <- ставим задачу в ожидание из-за лока
        }
    }

    /** Трасса прогона: буквы потоков, выбранных во всех решениях подряд, например "AAAABB". */
    public String traceString() {
        return trace.stream().reduce(String::concat).orElse("");
    }

    /** window() из тестовых программ: пауза перед следующей операцией с общими данными обязательна. */
    public static void point() {
        Scheduler scheduler = Task.CURRENT_SCHEDULER.get();
        if (scheduler.logging) {
            Task task = Task.CURRENT_TASK.get();
            String logStr = String.format("%s window()", task.getLiteralId());
            scheduler.log.add(logStr);
            System.out.println(logStr);
        }
        scheduler.needSwitch = true;
    }

    /** Ждать, пока cond не станет true. До этого поток не попадает в enabled. */
    public static void await(BooleanSupplier cond) {
        // todo log
        while (!cond.getAsBoolean());
        Scheduler scheduler = Task.CURRENT_SCHEDULER.get();
        scheduler.switchTask(false);
    }
}
