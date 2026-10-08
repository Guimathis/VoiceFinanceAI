package com.voicefinance.domain;

import com.voicefinance.domain.annotation.Confirmable;
import com.voicefinance.domain.annotation.FieldSpec;
import com.voicefinance.domain.entity.Expense;
import com.voicefinance.domain.enums.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConfirmableFieldsTest {

    private ConfirmableFields confirmableFields;

    @BeforeEach
    void setUp() {
        confirmableFields = new ConfirmableFields();
    }

    static class SampleTarget {
        @Confirmable(label = "nome", required = true)
        private String name;

        @Confirmable(label = "observação", required = false)
        private String observation;

        private String ignoredField;

        public SampleTarget(String name, String observation, String ignoredField) {
            this.name = name;
            this.observation = observation;
            this.ignoredField = ignoredField;
        }
    }

    @Test
    void shouldExtractAllAnnotatedFields() {
        List<FieldSpec> specs = confirmableFields.all(SampleTarget.class);

        assertThat(specs).containsExactlyInAnyOrder(
                new FieldSpec("name", "nome", true),
                new FieldSpec("observation", "observação", false)
        );
    }

    @Test
    void shouldValidateFieldNameCorrectly() {
        assertThat(confirmableFields.isValid(SampleTarget.class, "name")).isTrue();
        assertThat(confirmableFields.isValid(SampleTarget.class, "observation")).isTrue();
        assertThat(confirmableFields.isValid(SampleTarget.class, "ignoredField")).isFalse();
        assertThat(confirmableFields.isValid(SampleTarget.class, "unknown")).isFalse();
    }

    @Test
    void shouldValidateFieldNamesCorrectly() {
        assertThat(confirmableFields.isValid(SampleTarget.class, List.of("name", "observation"))).isEmpty();
        List<String> invalidFields = confirmableFields.isValid(SampleTarget.class, List.of("name", "observation", "ignoredField"));
        assertThat(invalidFields).isNotEmpty();
        assertThat(invalidFields).containsExactly("ignoredField");

    }

    @Test
    void shouldReturnOnlyMissingRequiredFieldLabels() {
        SampleTarget targetWithNulls = new SampleTarget(null, null, "any");

        List<String> missing = confirmableFields.missing(targetWithNulls);

        assertThat(missing).containsExactly("nome");
    }

    @Test
    void shouldReturnEmptyListWhenRequiredFieldsArePresent() {
        SampleTarget targetWithRequiredFilled = new SampleTarget("Guilherme", null, null);

        List<String> missing = confirmableFields.missing(targetWithRequiredFilled);

        assertThat(missing).isEmpty();
    }

    @Test
    void shouldWorkCorrectlyWithExpenseEntity() {
        Expense expense = Expense.builder()
                .category(null)
                .occurredOn(null)
                .description(null)
                .build();

        List<String> missing = confirmableFields.missing(expense);

        assertThat(missing).containsExactlyInAnyOrder("categoria", "occurredOn");

        expense.setCategory(Category.TRANSPORTE);
        expense.setOccurredOn(LocalDate.now());

        List<String> missingAfterFill = confirmableFields.missing(expense);
        assertThat(missingAfterFill).isEmpty();
    }
}
