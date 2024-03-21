package com.LeggoMahEggo.esson;

import com.LeggoMahEggo.esson.JsonValue.ValueType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TestClass {

    // Helper method to create LinkedHashMap objects for tests
    private static Map<String, Object> createdLHM(List<String> keys, List<?> values) {
        Map<String, Object> lhm = new LinkedHashMap<>();

        for (int i = 0; i < keys.size(); i++)
            lhm.put(keys.get(i), values.get(i));

        return lhm;
    }

    // Helper method to have a null value in a List, as you cannot add a null value directly to List.of
    private static List<Object> nullList() {
        List<Object> nullList = new ArrayList<>();
        nullList.add(null);
        return nullList;
    }


    @TestInstance(Lifecycle.PER_CLASS)
    @Nested
    public class GeneralTests {

        private class TypeCounter {
            private int listNum;
            private int mapNum;

            TypeCounter() {
                listNum = 0;
                mapNum = 0;
            }
        }

        private TypeCounter traverser(JsonValue value, TypeCounter counter) {
            if (value.getValueType().equals(ValueType.LIST)) {
                for (JsonValue lValue : value.getAsList()) {

                    // Increase count
                    switch (lValue.getValueType()) {
                        case LIST: counter.listNum++; break;
                        case MAP: counter.mapNum++; break;
                        case OTHER: continue;
                    }

                    // Check list/map out
                    traverser(lValue, counter);
                }

            } else if (value.getValueType().equals(ValueType.MAP)) {

                for (JsonValue mValue : value.getAsMap().values()) {
                    // Increase count
                    switch (mValue.getValueType()) {
                        case LIST: counter.listNum++; break;
                        case MAP: counter.mapNum++; break;
                        case OTHER: continue;
                    }

                    // Check list/map out
                    traverser(mValue, counter);
                }
            }

            return counter;
        }


        @Test
        public void traverseJson() {
            /*
             * Create JsonValue object
             */
            JsonValue tree = JsonValue.valueOf(JsonList.fromList(
                    List.of("b\\n\\r\\b\\n'", 10000, 15, 371293, 5.9049000000000005d, "13'2", false,
                            createdLHM(List.of("ab", "av"), List.of("b", "d")), true, List.of(), Map.of(),
                            createdLHM(List.of("ab", "value", "mappy"),
                                    List.of(
                                            "c",
                                            true,
                                            createdLHM(List.of("ab", "doot"), List.of(true, List.of(true, false, "b", "cre")))
                                    )
                            ),
                            "beepy", nullList(), Map.of(), "ne\"e\\npy")
            ));

            TypeCounter counter = traverser(tree, new TypeCounter());
            Assertions.assertAll(
                    () -> {
                        int expectedLists = 3;
                        System.out.println("Expected lists: " + expectedLists);
                        System.out.println("Actual lists: " + counter.listNum);
                        Assertions.assertEquals(expectedLists, counter.listNum);
                    },
                    () -> {
                        int expectedMaps = 5;
                        System.out.println("Expected maps: " + expectedMaps);
                        System.out.println("Actual maps: " + counter.mapNum);
                        Assertions.assertEquals(expectedMaps, counter.mapNum);
                    }
            );
        }


        /*
         * JSON conversion from JsonList/Map
         */

        private List<Arguments> jsonListMethodSource() {
            return List.of(
                    Arguments.of(JsonList.fromList(List.of(1, 2, 3, "abc", 55.4D, false)), "[1, 2, 3, \"abc\", 55.4, false]"),
                    Arguments.of(
                            JsonList.fromList(List.of(List.of(), nullList(), List.of(true, false), List.of(1), List.of("help"))),
                            "[[], [null], [true, false], [1], [\"help\"]]"
                    ),
                    Arguments.of(
                            JsonList.fromList(List.of(
                                    createdLHM(List.of("a", "b", "c"), List.of(1, 2, List.of(64.7D)))
                            )),
                            "[{\"a\": 1, \"b\": 2, \"c\": [64.7]}]"
                    )
            );
        }

        private List<Arguments> jsonMapMethodSource() {
            return List.of(
                    Arguments.of(JsonMap.fromMap(
                            createdLHM(List.of("a", "b", "c", "anything can be a key"),
                                    List.of(42, true, Map.of(), nullList())
                            )), "{\"a\": 42, \"b\": true, \"c\": {}, \"anything can be a key\": [null]}"),
                    Arguments.of(JsonMap.fromMap(
                            createdLHM(List.of("1", "lel"),
                                       List.of(Map.of(), List.of(
                                               createdLHM(List.of("1"), List.of(Map.of())),
                                               2
                                       )))
                            ),
                            "{\"1\": {}, \"lel\": [{\"1\": {}}, 2]}"
                    ),
                    Arguments.of(
                            JsonMap.fromMap(
                                    createdLHM(List.of("So"),
                                    List.of(
                                        List.of(
                                            createdLHM(List.of("much"),
                                                List.of(
                                                    List.of(
                                                        createdLHM(List.of("nesting..."), List.of(List.of()))
                                        )   )      )
                                    )))
                            ),
                            "{\"So\": [{\"much\": [{\"nesting...\": []}]}]}"
                    )
            );
        }

        @ParameterizedTest
        @MethodSource("jsonListMethodSource")
        public void jsonListConvertsToCorrectJson(JsonList list, String listAsJson) {

            System.out.println("Expected list: " + listAsJson);
            System.out.println("List to convert: " + list);

            Assertions.assertEquals(listAsJson, list.toJsonString());
        }

        @ParameterizedTest
        @MethodSource("jsonMapMethodSource")
        public void jsonMapConvertsToCorrectJson(JsonMap map, String mapAsJson) {
            System.out.println("Expected map: " + mapAsJson);
            System.out.println("Map to convert: " + map);

            Assertions.assertEquals(mapAsJson, map.toJsonString());
        }
    }

    @TestInstance(Lifecycle.PER_CLASS)
    @Nested
    public class JsonValueTests {

        /*
         * Method sources
         */

        private List<Arguments> valueTypeMethodSource() {
            List<Arguments> list = new ArrayList<>(); // Not List.of() so as to allow null values

            // List
            list.add(Arguments.of(JsonList.fromList(List.of()), ValueType.LIST));
            list.add(Arguments.of(JsonList.fromList(List.of(1, 2, 3, 4)), ValueType.LIST));
            list.add(Arguments.of(JsonList.fromList(List.of("aa", true, 55.9)), ValueType.LIST));
            list.add(Arguments.of(JsonList.fromList(List.of(createdLHM(List.of("Here's a map ;)"), List.of(90)))), ValueType.LIST));

            // Map
            list.add(Arguments.of(JsonMap.fromMap(createdLHM(List.of(), List.of())), ValueType.MAP));
            list.add(Arguments.of(JsonMap.fromMap(createdLHM(List.of("a", "b"), List.of(1, 2))), ValueType.MAP));
            list.add(Arguments.of(JsonMap.fromMap(createdLHM(List.of("Here's a list ;)"), List.of(List.of("boop!")))),
                    ValueType.MAP));

            // Other
            list.add(Arguments.of(null, ValueType.OTHER));
            list.add(Arguments.of(true, ValueType.OTHER));
            list.add(Arguments.of(15, ValueType.OTHER));
            list.add(Arguments.of(87.2, ValueType.OTHER));
            list.add(Arguments.of("yabba dabba doo!", ValueType.OTHER));

            return list;
        }

        public List<Object> throwsMethodSource() {
            return List.of(List.of(), Map.of(), Set.of(), Pattern.compile("abc"), Options.defaultOptions(),
                    new File("file"), Path.of("path"));
        }


        /*
         * Tests
         */

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        public void getAsBoolean(Boolean bool) {
            System.out.println("Expected: " + bool);
            Assertions.assertEquals(bool, JsonValue.valueOf(bool).getAsBoolean());
        }

        @Test
        public void getAsString() {
            String str = "thi\ngy";
            System.out.println("Expected: " + str);
            Assertions.assertEquals(str, JsonValue.valueOf(str.replace("\n", "\\n")).getAsString());
        }

        @Test
        public void getAsLongNumber() {
            Long l = 69420L;
            System.out.println("Expected: " + l);
            Assertions.assertEquals(l, JsonValue.valueOf(l).getAsNumber());
        }

        @Test
        public void getAsDoubleNumber() {
            Double d = 69420.421337D;
            System.out.println("Expected: " + d);
            Assertions.assertEquals(d, JsonValue.valueOf(d).getAsNumber());
        }

        @Test
        public void isNullValue() {
            System.out.println("Expected to be null");
            Assertions.assertTrue(JsonValue.valueOf(null).isNullValue());
        }

        @ParameterizedTest
        @MethodSource("valueTypeMethodSource")
        public void valueTypeIsCorrect(Object obj, ValueType valueType) {
            System.out.println("Expected value type is " + valueType);
            Assertions.assertEquals(valueType, JsonValue.valueOf(obj).getValueType());
        }

        @Test
        public void nullToString() {
            System.out.println("Expected: \"null\"");
            Assertions.assertEquals("null", JsonValue.valueOf(null).toString());
        }

        @Test
        public void stringToString() {
            String str = "This is a string!";
            System.out.println("Expected: '" + str + "'");
            Assertions.assertEquals("'" + str + "'", JsonValue.valueOf(str).toString());
        }

        @Test
        public void listToString() {
            System.out.println("Expected: \"[1, 2, 3]\"");
            Assertions.assertEquals("[1, 2, 3]", JsonValue.valueOf(JsonList.fromList(List.of(1, 2, 3))).toString());
        }

        @Test
        public void mapToString() {
            Map<String, Object> map = createdLHM(List.of("a", "b", "c", "d"), List.of(1, "2", true, List.of()));
            System.out.println("Expected: \"" + map + "\"");
            Assertions.assertEquals("{a=1, b='2', c=true, d=[]}", JsonValue.valueOf(JsonMap.fromMap(map)).toString());
        }

        @ParameterizedTest
        @MethodSource("throwsMethodSource")
        public void throwsForUnsupportedValues(Object obj) {
            Assertions.assertThrowsExactly(IllegalArgumentException.class, () -> JsonValue.valueOf(obj));
        }

        @Test
        public void integerDoesNotThrow() {
            int num = 1;
            Assertions.assertDoesNotThrow(() -> JsonValue.valueOf(num));
        }
    }

    @TestInstance(Lifecycle.PER_CLASS)
    @Nested
    public class FileTests {
        private final List<File> PASS_FILES = new ArrayList<>();
        private final List<File> FAIL_FILES = new ArrayList<>();
        private final List<String> TEST_FOLDERS = List.of(
                "Google_Code_json-test-suite",
                "json.org_JSON_checker",
                "custom"
        );
        private final Map<String, List<String>> FILES_TO_IGNORE = Map.of(
                "json.org_JSON_checker", List.of(
                        "fail1.json", // EMCA-404 standard allows for top-level values that are not arrays/objects
                        "fail18.json" // Depth-checking is not part of the EMCA-404 standard
                )
        );

        @BeforeAll
        public void findTestFiles() {
            for (String folder : TEST_FOLDERS) {
                String folderPath = TestClass.class.getClassLoader().getResource(folder).getPath();
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
        private Named<File> namedArgument(File file) {
            return Named.named(file.getName() + " - " + file.getParentFile().getParentFile().getName(), file);
        }

        private List<Arguments> passFileMethodSource() {
            return PASS_FILES.stream().map(f -> Arguments.of(namedArgument(f))).collect(Collectors.toList());
        }

        private List<Arguments> failFileMethodSource() {
            return FAIL_FILES.stream().map(f -> Arguments.of(namedArgument(f))).collect(Collectors.toList());
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

    @TestInstance(Lifecycle.PER_CLASS)
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

    @TestInstance(Lifecycle.PER_CLASS)
    @Nested
    public class OptionsTests {

        /*
         * Method sources
         */

        private List<Arguments> commaAtEndMethodSource() {
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

        private List<Arguments> leadingZeroesMethodSource() {
            return List.of(
                    Arguments.of("000013", 13),
                    Arguments.of("16e+002", 256)
            );
        }

        private List<Arguments> plusAtFrontMethodSource() {
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
