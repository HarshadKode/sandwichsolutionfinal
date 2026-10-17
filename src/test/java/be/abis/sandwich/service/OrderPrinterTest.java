package be.abis.sandwich.service;

import be.abis.sandwich.enumeration.BreadType;
import be.abis.sandwich.exception.PrintOrderException;
import be.abis.sandwich.model.Course;
import be.abis.sandwich.model.Person;
import be.abis.sandwich.model.Sandwich;
import be.abis.sandwich.model.SandwichOrder;
import be.abis.sandwich.model.SandwichOrderDetail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderPrinterTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 16);

    @TempDir
    private Path directory;

    private final Person ada = new Person(10, "Ada", "Lovelace");
    private final Person bob = new Person(11, "Bob", "Builder");
    private final Sandwich pinky = new Sandwich(21, "Pinky", "Special", "Specials", 4.8);
    private final Sandwich hesp = new Sandwich(1, "Hesp", "Ham", "Vlees", 3.2);

    private SandwichOrder order(String courseTitle) {
        return new SandwichOrder(DAY, new Course(1, courseTitle));
    }

    private SandwichOrderDetail detail(Sandwich sandwich, int amount, Person person, boolean vegetables, String comment) {
        return new SandwichOrderDetail(sandwich, amount, BreadType.BROWN, vegetables, comment, person);
    }

    @Test
    void writesAReadableTextFileAndReturnsTheText() throws IOException {
        OrderPrinter printer = new OrderPrinter(directory.toString());

        String text = printer.print(order("Spring Boot"), List.of(
                detail(pinky, 1, ada, true, "no onions"),
                detail(hesp, 1, ada, false, ""),
                detail(hesp, 2, bob, true, "")));

        Path file = directory.resolve("sandwich-order_2026-10-16_spring-boot.txt");
        assertThat(file).exists();
        assertThat(Files.readString(file, StandardCharsets.UTF_8)).isEqualTo(text);
        assertThat(text)
                .contains("Course: Spring Boot", "Date:   2026-10-16")
                .contains("Ada Lovelace", "Bob Builder")
                .contains("1x Pinky", "remark: no onions")
                .contains("1x Hesp")
                .contains("Total: € 14.40")
                .doesNotContain("\r");   // the same line endings on every platform
    }

    @Test
    void addsUpTheSameSandwichInTheSummary() {
        OrderPrinter printer = new OrderPrinter(directory.toString());

        String text = printer.print(order("Spring Boot"), List.of(
                detail(hesp, 1, ada, true, ""),
                detail(hesp, 2, bob, true, "")));

        assertThat(text).contains("3x Hesp");
    }

    @Test
    void createsTheDirectoryWhenItDoesNotExistYet() {
        Path nested = directory.resolve("not").resolve("there").resolve("yet");

        new OrderPrinter(nested.toString()).print(order("Spring Boot"), List.of(detail(hesp, 1, ada, true, "")));

        assertThat(nested.resolve("sandwich-order_2026-10-16_spring-boot.txt")).exists();
    }

    @Test
    void theFileNameOfACourseWithoutLettersStillWorks() {
        new OrderPrinter(directory.toString()).print(order("???"), List.of(detail(hesp, 1, ada, true, "")));

        assertThat(directory.resolve("sandwich-order_2026-10-16_course.txt")).exists();
    }

    @Test
    void failsWithAClearErrorWhenTheFileCannotBeWritten() throws IOException {
        Path notADirectory = Files.createFile(directory.resolve("blocked"));
        OrderPrinter printer = new OrderPrinter(notADirectory.toString());

        assertThatThrownBy(() -> printer.print(order("Spring Boot"), List.of(detail(hesp, 1, ada, true, ""))))
                .isInstanceOf(PrintOrderException.class)
                .hasMessageContaining("Could not write the order");
    }
}
