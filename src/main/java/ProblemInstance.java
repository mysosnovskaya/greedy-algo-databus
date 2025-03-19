import java.util.List;

/**
 * @param order      список по индексу i представляет собой список работ, от которых зависит работа i
 *                   (чтобы запустить работу i, должны быть выполены все работы из списка по индексу i)
 */
public record ProblemInstance(List<Job> jobs, Integer coresCount, List<List<Integer>> order) {
}
