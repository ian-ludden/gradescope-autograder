package autograder;

/**
 * A skill assessed in Mastery-Based Learning (MBL) mode.
 *
 * @param code the short skill code (e.g. MH07)
 * @param packageName the package containing the skill's test classes
 * @param name the human-readable skill name
 * @param required the number of problems that must be solved to earn "Met"
 * @param problems the number of problems available for the skill
 * @param manual whether the skill is graded manually rather than by the autograder
 */
public record MblSkill(String code, String packageName, String name, int required, int problems,
    boolean manual) {
}
