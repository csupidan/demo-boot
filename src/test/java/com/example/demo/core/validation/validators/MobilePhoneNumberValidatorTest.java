package com.example.demo.core.validation.validators;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class MobilePhoneNumberValidatorTest {

	@Test
	public void testIsValid() {
		assertThat(MobilePhoneNumberValidator.isValid("10000000"), is(false));
		assertThat(MobilePhoneNumberValidator.isValid("11811111111"), is(false));
		assertThat(MobilePhoneNumberValidator.isValid("13811111111"), is(true));
		assertThat(MobilePhoneNumberValidator.isValid("15800000000"), is(true));
		assertThat(MobilePhoneNumberValidator.isValid("18900000000"), is(true));
	}

	@Test
	public void testRandomValue() {
		for (int i = 0; i < 100; i++)
			assertThat(MobilePhoneNumberValidator.isValid(MobilePhoneNumberValidator.randomValue()), is(true));
	}

}
