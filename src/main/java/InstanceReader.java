import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InstanceReader {
    private static final String GAMS_ORDER_LINE_PATTERN = "a\\(\"(\\d+)\",\"(\\d+)\"\\)=1;";
    private static final Map<String, List<Integer>> jobToTimeAndBus;

    static {
        jobToTimeAndBus = readData();
    }

    public static ProblemInstance readInstance(Path path) {
        String fileName = path.getFileName().getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        fileName = (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex); // Убираем расширение

        String[] splittedFileName = fileName.replace("data_", "").split("_");
        int jobsCount = Integer.parseInt(splittedFileName[0].replace("j", ""));
        int coresCount = Integer.parseInt(splittedFileName[2].replace("c", ""));
        List<String> strJobs = new ArrayList<>();
        for (int k = 3; strJobs.size() < jobsCount; k += 2) {
            strJobs.add(splittedFileName[k] + "_" + splittedFileName[k + 1]);
        }

        List<Job> jobs = new ArrayList<>();
        for (int j = 0; j < strJobs.size(); j++) {
            Job job = new Job(jobToTimeAndBus.get(strJobs.get(j)).get(0), jobToTimeAndBus.get(strJobs.get(j)).get(1), j);
            jobs.add(job);
        }
        List<List<Integer>> order = readOrder(path.toFile(), jobsCount);
        return new ProblemInstance(jobs, coresCount, order, fileName);
    }

    private static Map<String, List<Integer>> readData() {
        try {
            Map<String, List<Integer>> jobToTimeAndBus = new HashMap<>();
            try (BufferedReader br = new BufferedReader(new FileReader("bus_percents_results.txt"))) {
                String strLine;
                while ((strLine = br.readLine()) != null) {
                    String[] splitedData = strLine.trim().replaceAll("   ", " ").replaceAll("    ", " ").split(" ");
                    jobToTimeAndBus.put(splitedData[0], List.of(Integer.parseInt(splitedData[1]), Integer.parseInt(splitedData[2])));
                }
            }
            return jobToTimeAndBus;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static List<List<Integer>> readOrder(File file, int jobsCount) {
        List<List<Integer>> order = new ArrayList<>();
        for (int i = 0; i < jobsCount; i++) {
            order.add(new ArrayList<>());
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String strLine;
            while ((strLine = br.readLine()) != null) {
                if (strLine.contains("a(")) {
                    for (int i = 0; i < jobsCount; i++) {
                        String firstJob = strLine.replaceAll(GAMS_ORDER_LINE_PATTERN, "$1");
                        int afterJob = Integer.parseInt(firstJob.substring(0, firstJob.length() - 1)) - 1;
                        String secondJob = strLine.replaceAll(GAMS_ORDER_LINE_PATTERN, "$2");
                        int beforeJob = Integer.parseInt(secondJob.substring(0, secondJob.length() - 1)) - 1;
                        if (afterJob == i && beforeJob < jobsCount) {
                            order.get(i).add(beforeJob);
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return order;
    }
}
