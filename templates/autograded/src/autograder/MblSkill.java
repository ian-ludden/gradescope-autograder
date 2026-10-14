package autograder;

/**
 * A skill assessed in Mastery-Based Learning (MBL) mode.
 */
public final class MblSkill {
  private final String code;
  private final String packageName;
  private final String name;
  private final int required;
  private final int problems;
  private final boolean manual;

  /**
   * @param code the short skill code (e.g. MH07)
   * @param packageName the package containing the skill's test classes
   * @param name the human-readable skill name
   * @param required the number of problems that must be solved to earn "Met"
   * @param problems the number of problems available for the skill
   * @param manual whether the skill is graded manually rather than by the autograder
   */
  public MblSkill(String code, String packageName, String name, int required, int problems,
      boolean manual) {
    this.code = code;
    this.packageName = packageName;
    this.name = name;
    this.required = required;
    this.problems = problems;
    this.manual = manual;
  }

  public String code() {
    return code;
  }

  public String packageName() {
    return packageName;
  }

  public String name() {
    return name;
  }

  public int required() {
    return required;
  }

  public int problems() {
    return problems;
  }

  public boolean manual() {
    return manual;
  }
}
