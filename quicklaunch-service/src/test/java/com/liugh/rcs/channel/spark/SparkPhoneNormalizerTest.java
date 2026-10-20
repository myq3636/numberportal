package com.liugh.rcs.channel.spark;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class SparkPhoneNormalizerTest {

    @Test
    public void normalizesMexicanE164ToTenDigits() {
        assertEquals("5512345678", SparkPhoneNormalizer.normalize("+525512345678", "MX"));
    }

    @Test
    public void stripsSpacesAndHyphens() {
        assertEquals("5512345678", SparkPhoneNormalizer.normalize("+52 55-1234-5678", "MX"));
    }

    @Test
    public void acceptsNullCountryWhenPhoneIsMexican() {
        assertEquals("5512345678", SparkPhoneNormalizer.normalize("+525512345678", null));
    }

    @Test
    public void rejectsNonMexicanCountry() {
        expectFail("+8613800138000", "CN");
    }

    @Test
    public void rejectsNonTenDigit() {
        expectFail("+525512345", "MX");
    }

    @Test
    public void rejectsLetters() {
        expectFail("+52ab12345678", "MX");
    }

    private static void expectFail(String e164, String country) {
        try {
            SparkPhoneNormalizer.normalize(e164, country);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }
}
