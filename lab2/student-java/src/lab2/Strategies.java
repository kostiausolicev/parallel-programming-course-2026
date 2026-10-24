package lab2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/* Стратегии выбора следующего потока.
 * FirstAlive — этап 1, Replay — этап 2, RandomPick — этап 3, Dfs — этап 4 (ограничение bound — этап 5).
 * enabled — номера потоков, которые могут сделать шаг, по возрастанию; last — прошлый выбранный (-1 в начале). */
public final class Strategies {
    private Strategies() { }

    /** Всегда первый поток из enabled. */
    public static final class FirstAlive implements Strategy {
        public int pick(List<Integer> enabled, int last) {
            if (enabled.isEmpty()) return -1;
            return enabled.getFirst();
        }
    }

    /** Проигрывает трассу: на i-м решении выбирает поток trace[i] ('A' -> 0, 'B' -> 1). */
    public static final class Replay implements Strategy {
        private char[] trace;
        private int i;

        public Replay(String trace) {
            this.trace = trace.toCharArray();
        }

        /** Сбросить позицию в трассе перед прогоном. */
        public void beginRun() {
            this.i = 0;
        }

        public int pick(List<Integer> enabled, int last) {
            if (i >= this.trace.length) {
                throw new IllegalArgumentException();
            }
            int next = charToInt(trace[i]);
            if (!enabled.contains(next)) {
                throw new IllegalArgumentException();
            }
            i++;
            return next;
        }

        private int charToInt(char ch) {
            return switch (ch) {
                case 'A' -> 1;
                case 'B' -> 2;
                case 'C' -> 3;
                case 'D' -> 4;
                default -> -1;
            };
        }
    }

    /** Случайный поток: enabled.get(rnd.nextInt(enabled.size())), rnd = new java.util.Random(seed). */
    public static final class RandomPick implements Strategy {
        private final Random random;
        public RandomPick(long seed) {
            this.random = new Random(seed);
        }

        public int pick(List<Integer> enabled, int last) {
            return enabled.get(this.random.nextInt(enabled.size()));
        }
    }

    /** Stateless DFS по дереву выборов: каждый прогон с нуля, повторяет префикс прошлого и берёт следующий вариант.
     *  bound — максимум вытеснений (этап 5); для dfs runner передаёт Integer.MAX_VALUE. */
    public static final class Dfs implements Strategy {
        private static final class Path {
            final List<Integer> options;
            int selectedOption;

            Path(List<Integer> options) {
                this.options = List.copyOf(options);
                this.selectedOption = 0;
            }

            boolean isLastOption() {
                return selectedOption == options.size() - 1;
            }
        }

        private int run = 0;
        private int dept = 0;
        private final int bound;
        private final Map<Integer, Path> tree;

        public Dfs(int bound) {
            this.bound = bound;
            this.tree = new HashMap<>();
        }

        /** Подготовиться к новому прогону (позиция в пути, счётчик вытеснений). */
        public void beginRun() {
            this.dept = 0;
        }

        public int pick(List<Integer> enabled, int last) {
            Path p = this.tree.computeIfAbsent(this.dept++, (_) -> new Path(enabled));
            return p.options.get(p.selectedOption);
        }

        /** Выбрать следующий путь; false — дерево обойдено. */
        public boolean endRun() {
            run++;

            dept--;
            while (dept >= 0) {
                Path p = tree.get(dept);
                if (p.isLastOption()) {
                    int deptLocal = dept;
                    while (this.tree.containsKey(deptLocal)) {
                        tree.remove(deptLocal);
                        deptLocal++;
                    }
                    dept--;
                    continue;
                }
                p.selectedOption++;
                return true;
            }
            return !tree.isEmpty();
        }
    }
}
