package autograder;

/**
 * The output message of a submission.
 * 
 * <ul>
 * <li>{@link #DEFAULT}: The default message shown to students after grading.</li>
 * <li>{@link #MBL}: The message shown to students in Mastery-Based Learning mode.</li>
 * <li>{@link #TEST_RUNNER_FAILED}: The message shown to students when the test runner fails.</li>
 */
public enum OutputMessage {
  DEFAULT(
      "# Your submission has been successfully graded \nYour estimated grade is shown to the right under *\"Autograder Score\"*"),
  MBL(
      "# Your submission has been successfully graded \nYour result for each skill is listed below. Skills marked *\"To be graded manually\"* are not graded by the autograder."),
  TEST_RUNNER_FAILED(
      "# ERROR: Grading Failed \n**There was a problem with your code** that caused some tests to unexpectedly fail. Please see the output below and resubmit. Contact an instructor or TA for more help.");

  private final String message;

  OutputMessage(String message) {
    this.message = message;
  }

  String getValue() {
    return message;
  }
}
