![lgpl3.0](https://www.gnu.org/graphics/lgplv3-with-text-154x68.png)

# esson
esson (**S**imple J**SON**) is a "just works" parser -- all you do is call a single method, which a returns a simple set of structures that are easy to navigate and pull data from. Serialization or converting to specific object types was not my goal with the project (though if needed I may look into that in the future).

# Download
To add esson to your project, add the following to your pom.xml file:
```
To be added
```

# How to use
The following classes are all you need to use esson:

* **Parser:** Parses the string/file into Java structures
* **Options:** Extra options for ignoring EMCA-404 standards (such as an extra comma at the end of an array/object)
* **JsonValue:** Represents some legal JSON value: String, Long, Double, Boolean, JsonList and JsonMap (explained below), and null
* **JsonList:** Extends ArrayList\<JsonValue>
* **JsonMap:** Extends LinkedHashMap<String, JsonValue>

## Parser.java
There are two methods to parse JSON: `parseFromString`, and `parseFromFile`. The former takes a String, and the latter takes a File object; both return a JsonValue object.

```java
import com.LeggoMahEggo.esson.Parser;
import com.LeggoMahEggo.esson.JsonValue;

public class Main {
  public static void main(String[] args) {
    JsonValue value = Parser.parseFromString("[\"A very simple example\"]");
    System.out.println(v);
    System.out.println(v.getAsList().get(0));
    System.out.println(v.getAsList().get(0).getAsString());
  }
}
```

This will result in the following output:
```
['A very simple example']
'A very simple example'
A very simple example
```

## Options.java
If you wish to load a JSON file that doesn't exactly follow EMCA-404 standards (such as including commas at the end of arrays/objects), you can include a Options object with particular flags set. The Options class comes a number of methods for ignoring EMCA-404 standards:

```java
import com.LeggoMahEggo.esson.Parser;
import com.LeggoMahEggo.esson.Options;

public class Main {
  public static void main(String[] args) {
    // Prints [1,2,3]
    System.out.println(Parser.parseFromString("[1,2,3,]", Options.commaAtEnd()));

    // Prints 13
    System.out.println(Parser.parseFromString("0000013e+000001", Options.leadingZeroes()));

    // Prints 256
    System.out.println(Parser.parseFromString("+256", Options.plusAtFront()));

    // Prints 'This is enclosed by single-quotes'
    System.out.println(Parser.parseFromString("'This is enclosed by single-quotes'", Options.singleQuoteString()));

    // Prints [[1,2,3],13,256,'This is enclosed by single-quotes']
    System.out.println(Parser.parseFromString("[[1,2,3,], 0000013e+000001, +256, 'This is enclosed by single-quotes']",
      Options.mostPermissive()));
  }
}
```

You can also use OptionsBuilder to add the flags you want:
```java
import com.LeggoMahEggo.esson.Parser;
import com.LeggoMahEggo.esson.Options.OptionsBuilder;

public class Main {
  public static void main(String[] args) {
    // Prints [[1,2,3],'This is enclosed by single-quotes']
    System.out.println(Parser.parseFromString("[1,2,3,'This is enclosed by single-quotes',]", OptionsBuilder.newBuilder()
      .commaAtEnd().singleQuoteString().build()));

    // Prints [13,256]
    System.out.println(Parser.parseFromString("[0000013e+000001,+256]", OptionsBuilder.newBuilder()
      .leadingZeroes().plusAtFront().build()));
  }
}
```

As of current writing, only the following options exist:
* a single comma at the end of an array/object
* leading zeroes on numbers
* a leading plus for numbers
* enclosing a string with single-quotes

## JsonValue.java
This class represents the following JSON types:

* string (String)
* number (Long/Double)
* boolean (Boolean)
* array (JsonList)
* object (JsonMap)
* null

The following methods return the internal value:
* getAsString() - returns the string value as a String. Any legal escaped control characters inside (such as \n) will be converted to the _actual_ character
* getAsNumber() - returns the number value as a Number (from which you can cast to Long/Double/etc)
* getAsBoolean() - returns the boolean value as a Boolean
* getAsList() - returns the array value as a JsonList
* getAsMap() - returns the object value as a JsonMap

To check for nulls, call the isNullValue() method.

The toString() method will return the following:
* "null", in the case of a null internal value
* for strings, the internal value wrapped with single-quotes
* for all other types, the toString() method return value of the internal value

Additionally, for traversing the entire structure, you can call the getValueType() method to check what the JsonValue is representing. These are the possible values that can be returned:
* ValueType.LIST - if the internal value is a JsonList
* ValueType.MAP - if the internal value is a JsonMap
* ValueType.OTHER - if the internal value is any other value (includes nulls)

To use the class, import the following:
```java
import com.LeggoMahEggo.esson.JsonValue;
import com.LeggoMahEggo.esson.JsonValue.ValueType;
```

## JsonList.java
Represents an array; this class simply ArrayList\<JsonValue>. It includes a static method to convert a List of valid JSON types (`fromList`), and an instance method (`toJsonString`) to convert all its contents into a correctly formatted JSON string

To use the class, import the following:
```java
import com.LeggoMahEggo.esson.JsonList;
```

## JsonMap.java
Represents an object; this class extends LinkedHashMap<String, JsonValue>. It includes a static method to convert a Map whose values are valid JSON types (`fromMap`), and an instance method (`toJsonString`) to convert all its contents into a correctly formatted JSON string

To use the class, import the following:
```java
import com.LeggoMahEggo.esson.JsonMap;
```

### Final note
Both JsonList and JsonMap extend an interface called JsonContainer -- it implements the `toJsonString` method. To use the interface, import the following:

```java
import com.LeggoMahEggo.esson.JsonContainer;
```

# Exceptions
esson throws 4 different types of exceptions (all located in `com.LeggoMahEggo.esson.exceptions`):
* JsonParserException: the base exception thrown
* IllegalCharacterException: if the parser encounters a character in a place where it shouldn't be
* ValueParserException: if the parser encounters a problem while collecting a value
* NumberParserException: if the parser encounters a problem while collecting a number (subclass of ValueParserException)

# Acknowledgements
* Douglas Crockford of https://www.json.org/ for providing me with a clear format to follow for parsing, as well as the bulk of the json file tests (located at https://www.json.org/JSON_checker/)

# License
esson is licensed under the LGPL 3.0 license. For more information, please visit https://www.gnu.org/licenses/licenses.html

Additionally, esson uses the Junit5 framework for tests; no changes have been made to JUnit5's source code. BOM files generated with CycloneDX have been included with the repo.
