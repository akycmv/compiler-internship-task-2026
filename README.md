# MiniKotlin to Java CPS Compiler

This is an internship assignment for implementing a CPS-style (Continuation-Passing Style) compiler from MiniKotlin (a subset of Kotlin) to Java.

## Overview

The goal is to implement a compiler that translates MiniKotlin source code into Java, where all functions are expressed using continuation-passing style.

## Project Structure

- `samples/` - Example MiniKotlin programs
- `src/main/antlr/MiniKotlin.g4` - Grammar definition for MiniKotlin
- `src/main/kotlin/compiler/` - Compiler implementation
- `src/test/` - Testing framework
- `stdlib/` - Standard library with `Prelude` class containing CPS function examples

## Updated examples

<details>
<summary>Factorial </summary>

```kotlin
fun factorial(n: Int): Int {
    if (n <= 1) {
        return 1
    } else {
        return n * factorial(n - 1)
    }
}

fun main(): Unit {
    var result: Int = factorial(5)
    println(result)

    var a: Int = 10 + 5
    var b: Boolean = a > 10
    println(a)
}
```

Compiled into:

```java
public static void factorial(Integer n, Continuation<Integer> __continuation) {
    if ((n <= 1)) {
        __continuation.accept(1);
        return;
    } else {
        factorial((n - 1), (arg0) -> {
            __continuation.accept((n * arg0));
            return;
        });
    }

}

public static void main(String[] args) {
    factorial(5, (arg0) -> {
        Integer result = arg0;
        Prelude.println(result, (arg1) -> {
            Integer a = (10 + 5);
            Boolean b = (a > 10);
            Prelude.println(a, (arg2) -> {
            });
        });
    });

}
```

</details>
<details>
<summary> Fizzbuzz and nested functions </summary>

```kotlin
fun fizzbuzz(n: Int): Unit {
    var a: Int = 0
    while (a <= n) {
        if (a % 15 == 0) {
            println("fizzbuzz")
        }
        if (!(a % 15 == 0) && a % 3 == 0) {
            println("fizz")
        }
        if (!(a % 15 == 0) && a % 5 == 0) {
            println("buzz")
        }
        a = a + 1
    }
}

fun return_plus_one(n: Int): Int {
    return n + 1
}

fun main(): Unit {
    var n: Int = 29
    fizzbuzz(return_plus_one(n))
}
```


Compiled into:
```java
public static void fizzbuzz(Integer n, Continuation<Void> __continuation) {
    Integer[] a = {0};
    Runnable[] _loop0 = {null};
    _loop0[0] = () -> {
    if ((a[0] <= n)) {
        if (Objects.equals((a[0] % 15), 0)) {
            Prelude.println("fizzbuzz", (arg0) -> {
                if ((!Objects.equals((a[0] % 15), 0) && Objects.equals((a[0] % 3), 0))) {
                    Prelude.println("fizz", (arg1) -> {
                        if ((!Objects.equals((a[0] % 15), 0) && Objects.equals((a[0] % 5), 0))) {
                            Prelude.println("buzz", (arg2) -> {
                                a[0] = (a[0] + 1);
                            });
                        } else {
                            a[0] = (a[0] + 1);
                        }
                    });
                } else {
                    if ((!Objects.equals((a[0] % 15), 0) && Objects.equals((a[0] % 5), 0))) {
                        Prelude.println("buzz", (arg3) -> {
                            a[0] = (a[0] + 1);
                        });
                    } else {
                        a[0] = (a[0] + 1);
                    }
                }
            });
        } else {
            if ((!Objects.equals((a[0] % 15), 0) && Objects.equals((a[0] % 3), 0))) {
                Prelude.println("fizz", (arg4) -> {
                    if ((!Objects.equals((a[0] % 15), 0) && Objects.equals((a[0] % 5), 0))) {
                        Prelude.println("buzz", (arg5) -> {
                            a[0] = (a[0] + 1);
                        });
                    } else {
                        a[0] = (a[0] + 1);
                    }
                });
            } else {
                if ((!Objects.equals((a[0] % 15), 0) && Objects.equals((a[0] % 5), 0))) {
                    Prelude.println("buzz", (arg6) -> {
                        a[0] = (a[0] + 1);
                    });
                } else {
                    a[0] = (a[0] + 1);
                }
            }
        }
        _loop0[0].run();
    } else {
    }
    };
    _loop0[0].run();

}

public static void return_plus_one(Integer n, Continuation<Integer> __continuation) {
    __continuation.accept((n + 1));
    return;

}

public static void main(String[] args) {
    Integer n = 29;
    return_plus_one(n, (arg0) -> {
        fizzbuzz(arg0, (arg1) -> {
        });
    });

}
```
</details>
<details>
<summary> Binary search </summary>

```kotlin
fun binarySearch(target: Int, low: Int, high: Int): Boolean {
    if (low > high) {
        return false
    }
    var mid: Int = low + (high - low) / 2
    if (mid == target) {
        return true
    } else {
        if (mid > target) {
            return binarySearch(target, low, mid - 1)
        } else {
            return binarySearch(target, mid + 1, high)
        }
    }
}

fun main(): Unit {
    var found: Boolean = binarySearch(7, 0, 10)
    println(found)
    var notFound: Boolean = binarySearch(11, 0, 10)
    println(notFound)
}
```

Compiled into:

```java
public static void binarySearch(Integer target, Integer low, Integer high, Continuation<Boolean> __continuation) {
    if ((low > high)) {
        __continuation.accept(false);
        return;
    } else {
        Integer mid = (low + ((high - low) / 2));
        if (Objects.equals(mid, target)) {
            __continuation.accept(true);
            return;
        } else {
            if ((mid > target)) {
                binarySearch(target, low, (mid - 1), (arg0) -> {
                    __continuation.accept(arg0);
                    return;
                });
            } else {
                binarySearch(target, (mid + 1), high, (arg1) -> {
                    __continuation.accept(arg1);
                    return;
                });
            }
        }
    }

}

public static void main(String[] args) {
    binarySearch(7, 0, 10, (arg0) -> {
        Boolean found = arg0;
        Prelude.println(found, (arg1) -> {
            binarySearch(11, 0, 10, (arg2) -> {
                Boolean notFound = arg2;
                Prelude.println(notFound, (arg3) -> {
                });
            });
        });
    });

}
```
</details>

## Building and Running

```bash
# Build the project
./gradlew build

# Run with default example
./gradlew run

# Run with a specific file
./gradlew run --args="samples/example.mini"

# Run tests
./gradlew test
```
