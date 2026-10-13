package autograder.calc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import autograder.MblSkill;
import autograder.TestData;
import autograder.Visibility;

/**
 * Reports Mastery-Based Learning (MBL) results. Each skill is "Met" when enough of its problems
 * (test classes) are fully solved. The numeric score is always zero.
 */
public class MasteryScoreCalculator extends ScoreCalculator {
  private final List<MblSkill> skills;
  private final Map<String, Map<String, TestData>> problemsByPackage;

  public MasteryScoreCalculator(List<MblSkill> skills) {
    super(0);
    this.skills = List.copyOf(skills);
    this.problemsByPackage = new LinkedHashMap<>();
  }

  /**
   * Records the problems of a package. Raw per-class entries are not reported.
   */
  @Override
  public JSONArray parseTestResults(Set<TestData> tests) {
    Map<String, TestData> problems =
        problemsByPackage.computeIfAbsent(packageName, key -> new TreeMap<>());
    for (TestData test : tests) {
      problems.put(test.getName(), test);
    }
    return new JSONArray();
  }

  @Override
  public JSONArray finalizeResults() {
    JSONArray results = new JSONArray();
    for (MblSkill skill : skills) {
      results.put(skill.manual() ? manualEntry(skill) : skillEntry(skill));
    }
    return results;
  }

  @Override
  public double getScore() {
    return 0;
  }

  private JSONObject manualEntry(MblSkill skill) {
    JSONObject entry = baseEntry(skill, "To be graded manually");
    entry.put("output", "This skill is graded manually by your instructor.");
    return entry;
  }

  private JSONObject skillEntry(MblSkill skill) {
    Map<String, TestData> problems = problemsByPackage.getOrDefault(skill.packageName(), Map.of());
    List<String> lines = new ArrayList<>();
    int solved = 0;
    for (TestData problem : problems.values()) {
      boolean isSolved = problem.getMaxScore() > 0 && problem.getScore() == problem.getMaxScore();
      if (isSolved) {
        solved++;
        lines.add("- " + simpleName(problem.getName()) + ": solved");
      } else {
        lines.add("- " + simpleName(problem.getName()) + ": not solved"
            + (problem.getOutputText().isBlank() ? ""
                : "\n  - " + problem.getOutputText().strip().replace("\n", "\n  - ")));
      }
    }
    int available = Math.max(skill.problems(), problems.size());
    boolean met = solved >= skill.required();

    JSONObject entry = baseEntry(skill, met ? "Met" : "Not Yet");
    entry.put("status", met ? "passed" : "failed");
    entry.put("output", String.format("**%s**: solved %d of %d problems (%d required)\n\n%s",
        met ? "Met" : "Not Yet", solved, available, skill.required(), String.join("\n", lines)));
    return entry;
  }

  private JSONObject baseEntry(MblSkill skill, String status) {
    JSONObject entry = new JSONObject();
    entry.put("name", String.format("%s %s: %s", skill.code(), skill.name(), status));
    entry.put("score", 0);
    entry.put("max_score", 0);
    entry.put("output_format", "md");
    entry.put("visibility", Visibility.VISIBLE.getValue());
    return entry;
  }

  private static String simpleName(String className) {
    return className.substring(className.lastIndexOf('.') + 1);
  }
}
