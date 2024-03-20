import com.esson.JsonList;
import com.esson.JsonMap;
import com.esson.JsonValue;
import com.esson.Options;
import com.esson.Parser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class TestClass {

    // Helper method to create LinkedHashMap objects for tests
    private static Map<String, Object> createdLHM(List<String> keys, List<?> values) {
        Map<String, Object> lhm = new LinkedHashMap<>();

        for (int i = 0; i < keys.size(); i++)
            lhm.put(keys.get(i), values.get(i));

        return lhm;
    }

    @Nested
    public class FileTests {
        private static final List<File> PASS_FILES = new ArrayList<>();
        private static final List<File> FAIL_FILES = new ArrayList<>();
        private static final List<String> TEST_FOLDERS = List.of(
                "Google_Code_json-test-suite",
                "json.org_JSON_checker",
                "custom"
        );
        private static final Map<String, List<String>> FILES_TO_IGNORE = Map.of(
                "json.org_JSON_checker", List.of(
                        "fail1.json", // EMCA-404 standard allows for top-level values that are not arrays/objects
                        "fail18.json" // Depth-checking is not part of the EMCA-404 standard
                )
        );

        @BeforeAll
        public static void findTestFiles() {
            for (String folder : TEST_FOLDERS) {
                String folderPath = TestClass.class.getResource(folder).getPath();
                File[] testFolders = new File(folderPath).listFiles();

                for (File testFolder : testFolders) {
                    String name = testFolder.getName();

                    // Only searches in folder named "pass" and "fail"
                    if (testFolder.isDirectory() && (name.equals("pass") || (name.equals("fail")))) {
                        File[] testFiles = testFolder.listFiles();
                        boolean isPass = name.equals("pass");
                        List<String> ignoreList = FILES_TO_IGNORE.getOrDefault(folder, List.of());

                        for (File file : testFiles) {
                            String filename = file.getName();

                            if (ignoreList.contains(filename))
                                continue;

                            // Parser can technically handle files without .json extension, but we're not testing that
                            if (filename.endsWith(".json")) {
                                if (isPass)
                                    PASS_FILES.add(file);
                                else
                                    FAIL_FILES.add(file);
                            }
                        }
                    }
                }
            }

            // Sort files alphanumerically
            Comparator<File> alphaNumComparator = (a, b) -> {
                String aName = a.getName().replace(".json", "");
                String bName = b.getName().replace(".json", ""); // put "remove generic extension" here

                // Name collisions
                if (aName.equals(bName))
                    return a.getParent().compareTo(b.getParent());

                // One side does not have digits
                Pattern digitsOnly = Pattern.compile("[^\\d]");

                if (digitsOnly.matcher(aName).replaceAll("").isEmpty() ||
                        digitsOnly.matcher(bName).replaceAll("").isEmpty())
                    return aName.compareTo(bName);

                /*
                 * Alphanumeric comparison
                 */
                Pattern lettersOnly = Pattern.compile("[\\d]");
                Integer aNum = Integer.parseInt(digitsOnly.matcher(aName).replaceAll(""));
                Integer bNum = Integer.parseInt(digitsOnly.matcher(bName).replaceAll(""));

                // Name is same, numbers are different
                if (lettersOnly.matcher(aName).replaceAll("").equals(lettersOnly.matcher(bName).replaceAll("")))
                    return aNum.compareTo(bNum);

                // Otherwise, if both name and numbers are different...
                char firstAChar = aName.charAt(0);
                char firstBChar = bName.charAt(0);

                // ...and both start with numbers
                if ((firstAChar >= 48 && firstAChar <= 57) && (firstBChar >= 48 && firstBChar <= 57))
                    return aNum.compareTo(bNum);

                // ...and one starts with a number or both start with letters
                return aName.compareTo(bName);
            };

            PASS_FILES.sort(alphaNumComparator);
            FAIL_FILES.sort(alphaNumComparator);
        }


        /*
         * Method sources
         */
        private static Named<File> namedArgument(File file) {
            return Named.named(file.getName() + " - " + file.getParentFile().getParentFile().getName(), file);
        }

        private static List<Arguments> passFileMethodSource() {
            return PASS_FILES.stream().map(f -> Arguments.of(namedArgument(f))).toList();
        }

        private static List<Arguments> failFileMethodSource() {
            return FAIL_FILES.stream().map(f -> Arguments.of(namedArgument(f))).toList();
        }


        /*
         * Tests
         */
        private void printResultStatus(boolean passed) {
            if (passed)
                System.out.println("Test passed!");
            else
                System.out.println("Test failed!");
        }

        private void testCode(File jsonFile, String testType) {
            System.out.println("File to test: " + jsonFile.getName());
            System.out.println("Found in: " + jsonFile.getParentFile().getParentFile().toPath().resolve(testType));
            System.out.print("Parsing...");
            boolean parsed = false;

            try {
                JsonValue value = Parser.parseFromFile(jsonFile);
                System.out.println("successfully parsed.");
                parsed = true;
                System.out.println("Contents: " + value);
                printResultStatus(testType.equals("pass"));

            } catch (Exception e) {
                System.out.println("failed to parse.");
                printResultStatus(testType.equals("fail"));
                e.printStackTrace();
            }

            if (testType.equals("pass"))
                Assertions.assertTrue(parsed);
            else
                Assertions.assertFalse(parsed);
        }

        @ParameterizedTest
        @MethodSource("passFileMethodSource")
        public void testPassFiles(File jsonFile) {
            testCode(jsonFile, "pass");
        }

        @ParameterizedTest
        @MethodSource("failFileMethodSource")
        public void testFailFiles(File jsonFile) {
            testCode(jsonFile, "fail");
        }
    }


    /*
     * Parse method
     */

    private JsonValue attemptToParseString(String jsonStr, Options options) {
        try {
            System.out.println("String to parse: " + jsonStr);
            System.out.print("Parsing...");
            JsonValue value = Parser.parseFromString(jsonStr, options);
            System.out.println("successfully parsed.");
            System.out.println("Contents: " + value);
            return value;

        } catch (Exception e) {
            System.out.println("failed to parse.");
            throw e;
        }
    }

    @Nested
    public class TopLevelTests {
        @Test
        public void topLevelNumber() {
            JsonValue value = attemptToParseString("42.5", Options.defaultOptions());

            Assertions.assertEquals(42.5d, value.getAsNumber());
        }

        @Test
        public void topLevelString() {
            String jsonStr = "\"This is a top-level string\"";
            JsonValue value = attemptToParseString(jsonStr, Options.defaultOptions());

            Assertions.assertEquals(jsonStr.replaceAll("\"", ""), value.getAsString());
        }

        @Test
        public void topLevelNull() {
            JsonValue value = attemptToParseString("null", Options.defaultOptions());

            Assertions.assertTrue(value.isNullValue());
        }

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        public void topLevelBoolean(Boolean bool) {
            JsonValue value = attemptToParseString(bool.toString(), Options.defaultOptions());

            Assertions.assertEquals(bool, value.getAsBoolean());
        }

        @Test
        public void topLevelEmptyArray() {
            JsonValue value = attemptToParseString("[]", Options.defaultOptions());

            Assertions.assertEquals(new JsonList(), value.getAsList());
        }

        @Test
        public void topLevelEmptyObject() {
            JsonValue value = attemptToParseString("{}", Options.defaultOptions());

            Assertions.assertEquals(new JsonMap(), value.getAsMap());
        }
    }

    @Nested
    public class OptionsTests {

        /*
         * Method sources
         */

        private static List<Arguments> commaAtEndMethodSource() {
            return List.of(
                    Arguments.of("array", "[1,2,3,]",
                            JsonValue.valueOf(JsonList.fromList(List.of(1,2,3)))
                    ),
                    Arguments.of("object", "{\"a\": 1, \"b\": 2, \"c\": 3,}",
                            JsonValue.valueOf(JsonMap.fromMap(
                                    createdLHM(List.of("a", "b", "c"), List.of(1,2,3))
                            ))
                    )
            );
        }

        private static List<Arguments> leadingZeroesMethodSource() {
            return List.of(
                    Arguments.of("000013", 13),
                    Arguments.of("16e+002", 256)
            );
        }

        private static List<Arguments> plusAtFrontMethodSource() {
            return List.of(
                    Arguments.of("+5000", JsonValue.valueOf(5000)),
                    Arguments.of("+10000e-1", JsonValue.valueOf(0.0001d))
            );
        }


        /*
         * Tests
         */

        @ParameterizedTest
        @MethodSource("commaAtEndMethodSource")
        public void commaAtContainerEnd(String containerType, String jsonStr, JsonValue expectedValue) {
            System.out.println("Container type: " + containerType);
            JsonValue value = attemptToParseString(jsonStr, Options.commaAtEnd());

            Assertions.assertEquals(expectedValue, value);
        }

        @ParameterizedTest
        @MethodSource("leadingZeroesMethodSource")
        public void leadingZeroesOnNumbers(String jsonStr, int expectedNum) {
            JsonValue value = attemptToParseString(jsonStr, Options.leadingZeroes());
            System.out.println("Expected result: " + expectedNum);

            Assertions.assertEquals(expectedNum, value.getAsNumber().intValue());
        }

        @ParameterizedTest
        @MethodSource("plusAtFrontMethodSource")
        public void plusAtFrontOfNumbers(String jsonStr, JsonValue expectedNum) {
            JsonValue value = attemptToParseString(jsonStr, Options.plusAtFront());
            System.out.println("Expected result: " + expectedNum);

            Assertions.assertEquals(expectedNum, value);
        }

        @Test
        public void singleQuoteEnclosingString() {
            String jsonStr = "'This is enclosed by single-quotes'";
            JsonValue value = attemptToParseString(jsonStr, Options.singleQuoteString());
            System.out.println("Expected result: " + jsonStr.replaceAll("'", ""));

            Assertions.assertEquals(value.getAsString(), jsonStr.replaceAll("'", ""));
        }

        @Test
        public void allOptionsEnabled() {
            String jsonStr = "['b\\n\\r\\b\\n\\'', 00100e002, 15, 13.0e+05, +2.43e2, \"13'2\" ,false ," +
                    "{ 'ab': 'b', \"av\":   \"d\"    ,},true  , [], {}," +
                    "{ \"ab\": \"c\", \"value\": true, \"mappy\": " +
                        "{\"ab\": true, \"doot\": [true, false, \"b\", 'cre',]}" +
                    "}," +
                    "\"beepy\", [null], {},'ne\"e\\npy']";
            JsonValue value = attemptToParseString(jsonStr, Options.mostPermissive());

            /*
             * Create JsonValue object to compare with
             */
            List<Object> listWithNull = new ArrayList<>(); // Cannot add a null value directly to List.of
            listWithNull.add(null);

            JsonValue expectedValue = JsonValue.valueOf(JsonList.fromList(
                    List.of("b\\n\\r\\b\\n'", 10000, 15, 371293, 5.9049000000000005d, "13'2", false,
                    createdLHM(List.of("ab", "av"), List.of("b", "d")), true, List.of(), Map.of(),
                    createdLHM(List.of("ab", "value", "mappy"),
                            List.of(
                                    "c",
                                    true,
                                    createdLHM(List.of("ab", "doot"), List.of(true, List.of(true, false, "b", "cre")))
                            )
                    ),
                    "beepy", listWithNull, Map.of(), "ne\"e\\npy")
            ));
            System.out.println("Expected contents: " + expectedValue);

            Assertions.assertEquals(expectedValue, value);
        }
    }
}
