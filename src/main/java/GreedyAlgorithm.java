
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class GreedyAlgorithm {
    public static void main(String[] args) throws IOException {
    }

    public static double buildSchedule(ProblemInstance problemInstance) {
        List<List<Integer>> schedule = new LinkedList<>();
        List<Integer> notExecutedJodIndexes = new LinkedList<>();
        Map<Integer, Integer> tactsToFullExecution = new HashMap<>();
        Map<Integer, Job> indexToJob = new HashMap<>();
        for (Job job : problemInstance.jobs()) {
            notExecutedJodIndexes.add(job.index());
            tactsToFullExecution.put(job.index(), job.duration());
            indexToJob.put(job.index(), job);
        }

        List<Integer> firstMode = getNextMode(100, problemInstance.coresCount(),
                new LinkedList<>(notExecutedJodIndexes), notExecutedJodIndexes, problemInstance, indexToJob);
        schedule.add(firstMode);

        double estimatedExecutionDuration = estimateModeDuration(firstMode, notExecutedJodIndexes, tactsToFullExecution, indexToJob);
        while (!notExecutedJodIndexes.isEmpty()) {
            int freeBusPercent = 100;
            Set<Integer> nextMode = new HashSet<>();
            // в новой моде продолжаем выполнять те работы, которые начались в предыдущей моде
            for (Integer jobIndex : schedule.getLast()) {
                if (notExecutedJodIndexes.contains(jobIndex)) {
                    nextMode.add(jobIndex);
                    freeBusPercent -= indexToJob.get(jobIndex).busPercent();
                }
            }
            List<Integer> jobsToChooseFrom = new LinkedList<>(notExecutedJodIndexes);
            jobsToChooseFrom.removeAll(nextMode);
            nextMode.addAll(getNextMode(freeBusPercent, problemInstance.coresCount() - nextMode.size(),
                    jobsToChooseFrom, notExecutedJodIndexes, problemInstance, indexToJob));
            schedule.add(new LinkedList<>(nextMode));
            double estimatedModeDuration = estimateModeDuration(schedule.getLast(), notExecutedJodIndexes,
                    tactsToFullExecution, indexToJob);
            estimatedExecutionDuration += estimatedModeDuration;
        }
        return estimatedExecutionDuration;
    }

    private static List<Integer> getNextMode(int freeBusPercent, int freeCoresCount, List<Integer> jobsToChooseFrom,
                                             List<Integer> notExecutedJodIndexes, ProblemInstance problemInstance, Map<Integer, Job> indexToJob) {
        List<Integer> mode = new LinkedList<>();
        for (int i = 0; i < freeCoresCount && !jobsToChooseFrom.isEmpty(); i++) {
            Integer jobIndexToExecute;
            if (freeBusPercent <= 0) {
                jobIndexToExecute = getJobIndexWithMinBusPercent(jobsToChooseFrom, notExecutedJodIndexes, problemInstance, indexToJob);
            } else {
                jobIndexToExecute = findClosestJobToRemainingFreeBus(freeBusPercent / (freeCoresCount - i), jobsToChooseFrom, notExecutedJodIndexes, problemInstance, indexToJob);
            }
            if (jobIndexToExecute != null) {
                jobsToChooseFrom.remove(jobIndexToExecute);
                freeBusPercent -= indexToJob.get(jobIndexToExecute).busPercent();
                mode.add(jobIndexToExecute);
            } else {
                return mode;
            }
        }
        return mode;
    }

    private static Integer getJobIndexWithMinBusPercent(List<Integer> jobsToChooseFrom, List<Integer> notExecutedJodIndexes,
                                                        ProblemInstance problemInstance, Map<Integer, Job> indexToJob) {
        Integer jobIndex = null;
        for (int i = 0; i < jobsToChooseFrom.size(); i++) {
            if (indexToJob.get(jobsToChooseFrom.get(i)).busPercent() < Optional.ofNullable(jobIndex).map(ind -> indexToJob.get(jobsToChooseFrom.get(ind)).busPercent()).orElse(100)
                    && problemInstance.order().get(jobsToChooseFrom.get(i)).stream().noneMatch(notExecutedJodIndexes::contains)) {
                jobIndex = i;
            }
        }
        return Optional.ofNullable(jobIndex).map(jobsToChooseFrom::get).orElse(null);
    }

    private static Integer findClosestJobToRemainingFreeBus(int percent, List<Integer> jobsToChooseFrom, List<Integer> notExecutedJodIndexes,
                                                            ProblemInstance problemInstance, Map<Integer, Job> indexToJob) {
        Integer closestJobIndex = null;
        for (int i = 0; i < jobsToChooseFrom.size(); i++) {
            if (Math.abs(percent - indexToJob.get(jobsToChooseFrom.get(i)).busPercent())
                    <= Optional.ofNullable(closestJobIndex).map(ind -> Math.abs(percent - indexToJob.get(jobsToChooseFrom.get(ind)).busPercent()))
                            .orElse(100)
                    &&  problemInstance.order().get(jobsToChooseFrom.get(i)).stream().noneMatch(notExecutedJodIndexes::contains)) {
                closestJobIndex = i;
            }
        }
        return Optional.ofNullable(closestJobIndex).map(jobsToChooseFrom::get).orElse(null);
    }

    private static double estimateModeDuration(List<Integer> mode, List<Integer> notExecutedJodIndexes, Map<Integer, Integer> tactsToFullExecution,
                                               Map<Integer, Job> indexToJob) {
        List<Job> sortedByBusPercent = mode.stream().map(indexToJob::get)
                .sorted(Comparator.comparingDouble(Job::busPercent)).toList();

        int freeBusPercent = 100;
        int leftFreeCoresCount = mode.size();

        Map<Integer, Double> indexJobToVelocity = new HashMap<>();
        for (Job gaJob : sortedByBusPercent) {
            if (gaJob.busPercent() <= ((double) freeBusPercent) / leftFreeCoresCount) {
                indexJobToVelocity.put(gaJob.index(), 1.0);
                freeBusPercent -= gaJob.busPercent();
            } else {
                int busPercentForJob = freeBusPercent / leftFreeCoresCount;
                double velocity = ((double) busPercentForJob) / gaJob.busPercent();
                indexJobToVelocity.put(gaJob.index(), velocity);
                freeBusPercent -= busPercentForJob;
            }
            leftFreeCoresCount--;
        }

        double modeTime = Double.MAX_VALUE;
        // определяем, какая работа завершится первой. это событие и будет окончанием моды, т.к. придется выбирать новую моду
        for (Integer integer : mode) {
            double timeToFullExecution = tactsToFullExecution.get(integer) / indexJobToVelocity.get(integer);
            if (modeTime >= timeToFullExecution) {
                modeTime = timeToFullExecution;
            }
        }

        for (Integer job : mode) {
            Integer leftTactsToExecute = tactsToFullExecution.get(job);
            int executedInThisModeTacts = ((int) (modeTime * indexJobToVelocity.get(job)));
            int leftExecuteAfterThisMode = leftTactsToExecute - executedInThisModeTacts;
            tactsToFullExecution.put(job, Math.max(leftExecuteAfterThisMode, 0));
            if (leftExecuteAfterThisMode <= 1) {
                notExecutedJodIndexes.remove(job);
            }
        }

        return modeTime;
    }
}
