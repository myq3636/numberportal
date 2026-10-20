package com.liugh.rcs.channel.spark;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SparkResponseMapperTest {

    @Test
    public void parsesSuccess() {
        SparkResponseMapper.Result r = SparkResponseMapper.parse(
                "{\"code\":200,\"desc\":\"Mensaje Enviado\",\"uuid\":\"e10e\"}");
        assertTrue(r.isSuccess());
        assertEquals("e10e", r.getUuid());
    }

    @Test
    public void parsesBusinessError() {
        SparkResponseMapper.Result r = SparkResponseMapper.parse(
                "{\"code\":401,\"desc\":\"Credenciales no validas\"}");
        assertFalse(r.isSuccess());
        assertEquals("SPARK_401", r.getErrorCode());
        assertEquals("Credenciales no validas", r.getDesc());
    }

    @Test
    public void parsesEmptyAsUnknown() {
        SparkResponseMapper.Result r = SparkResponseMapper.parse(null);
        assertFalse(r.isSuccess());
        assertEquals("SPARK_SUBMIT_UNKNOWN", r.getErrorCode());
    }

    @Test
    public void parsesNonJsonAsUnknown() {
        SparkResponseMapper.Result r = SparkResponseMapper.parse("<html>error</html>");
        assertFalse(r.isSuccess());
        assertEquals("SPARK_SUBMIT_UNKNOWN", r.getErrorCode());
    }

    @Test
    public void parsesMissingCodeAsUnknown() {
        SparkResponseMapper.Result r = SparkResponseMapper.parse("{\"desc\":\"x\"}");
        assertFalse(r.isSuccess());
        assertEquals("SPARK_SUBMIT_UNKNOWN", r.getErrorCode());
    }
}
